// Browser WebRTC audio and CRM call controls. PSTN routing lives in the configured gateway.
let browserCallSession = null;
let telephonyMonitorTimer = null;
let telephonyIncomingLoading = false;
let telephonyIncomingAccount = null;

function browserCallStatus(session, message) {
  if (browserCallSession === session) session.panel.querySelector("[data-call-status]").textContent = message;
}

function stopBrowserCallMedia(session) {
  session.peer?.close();
  session.stream?.getTracks().forEach(track => track.stop());
  session.audio.pause();
  session.audio.srcObject = null;
  session.peer = null;
  session.stream = null;
}

function requestBrowserCallHangup(session) {
  if (!session.callId) return Promise.resolve();
  const token = state.auth.user?.id === session.userId ? state.auth.token : session.token;
  return fetch(`${API_BASE}/telephony/calls/${session.callId}/hangup`, {
    method: "POST", headers: { Authorization: `Bearer ${token}` }, keepalive: true
  }).then(response => { if (!response.ok) throw new Error("挂断请求未确认"); });
}

function closeBrowserCall({ hangup = false } = {}) {
  const session = browserCallSession;
  if (!session) return;
  if (hangup) requestBrowserCallHangup(session).catch(() => {});
  clearInterval(session.timer);
  stopBrowserCallMedia(session);
  session.panel.remove();
  browserCallSession = null;
}

function stopBrowserTelephony() {
  clearInterval(telephonyMonitorTimer);
  telephonyMonitorTimer = null;
  telephonyIncomingAccount = null;
  document.querySelector("#browserIncomingCalls")?.remove();
  closeBrowserCall({ hangup: true });
}

function waitForBrowserIce(peer) {
  if (peer.iceGatheringState === "complete") return Promise.resolve();
  return new Promise((resolve, reject) => {
    const finished = () => {
      if (peer.iceGatheringState !== "complete") return;
      cleanup();
      resolve();
    };
    const cleanup = () => { clearTimeout(timer); peer.removeEventListener("icegatheringstatechange", finished); };
    const timer = setTimeout(() => { cleanup(); reject(new Error("音频连接准备超时，请检查网络或网关 TURN 配置")); }, 12000);
    peer.addEventListener("icegatheringstatechange", finished);
  });
}

async function prepareBrowserCallAudio(session, config) {
  if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia || !window.RTCPeerConnection)
    throw new Error("浏览器通话需要支持 WebRTC 的浏览器，并使用 HTTPS 或 localhost 访问");
  const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true }, video: false });
  if (browserCallSession !== session) { stream.getTracks().forEach(track => track.stop()); throw new Error("通话窗口已关闭"); }
  session.stream = stream;
  const peer = new RTCPeerConnection({ iceServers: config.iceServers || [] });
  session.peer = peer;
  stream.getAudioTracks().forEach(track => peer.addTrack(track, stream));
  peer.addEventListener("track", event => {
    session.audio.srcObject = event.streams[0] || new MediaStream([event.track]);
    session.audio.play().catch(() => { session.panel.querySelector("[data-enable-audio]").hidden = false; });
  });
  peer.addEventListener("connectionstatechange", () => {
    if (peer.connectionState === "failed") {
      browserCallStatus(session, "音频连接失败，正在结束通话");
      requestBrowserCallHangup(session).catch(() => browserCallStatus(session, "音频连接失败，挂断状态待确认，请重试挂断"));
      stopBrowserCallMedia(session);
    }
  });
  await peer.setLocalDescription(await peer.createOffer());
  await waitForBrowserIce(peer);
  if (browserCallSession !== session) throw new Error("通话窗口已关闭");
  return peer.localDescription.sdp;
}

async function refreshBrowserCall(session) {
  if (browserCallSession !== session || !session.callId || session.refreshing) return;
  session.refreshing = true;
  try {
    const result = await apiRequest(`/telephony/calls/${session.callId}`);
    if (browserCallSession !== session || state.auth.user?.id !== session.userId) return;
    const call = result.call;
    session.panel.querySelector("[data-call-duration]").textContent = call.durationSeconds ? `${call.durationSeconds} 秒` : "";
    if (["ENDED", "FAILED"].includes(call.gatewayState)) {
      session.ended = true;
      clearInterval(session.timer);
      stopBrowserCallMedia(session);
      browserCallStatus(session, call.gatewayState === "FAILED" ? "通话失败" : "通话已结束");
      session.panel.querySelector("[data-hangup]").hidden = true;
      session.panel.querySelector("[data-mute]").hidden = true;
      return;
    }
    if (result.answerSdp && session.peer && !session.peer.remoteDescription) {
      try { await session.peer.setRemoteDescription({ type: "answer", sdp: result.answerSdp }); }
      catch (error) {
        stopBrowserCallMedia(session);
        requestBrowserCallHangup(session).catch(() => {});
        throw new Error("网关音频协商失败，已请求挂断");
      }
    }
    browserCallStatus(session, result.hangupRequested || session.hangupInitiated ? "正在确认挂断…" : call.status);
  } catch (error) {
    browserCallStatus(session, `通话状态更新失败，${error.message}`);
  } finally {
    session.refreshing = false;
  }
}

function createBrowserCallPanel(name, inbound) {
  const panel = document.createElement("section");
  panel.className = "browser-call-panel";
  panel.setAttribute("role", "dialog");
  panel.setAttribute("aria-label", "浏览器通话");
  panel.innerHTML = `<header><strong>${inbound ? "接听来电" : "浏览器外呼"}</strong><button class="icon-button" type="button" data-close-call aria-label="关闭并挂断">${icon("close")}</button></header><h3>${escapeHtml(name)}</h3><p data-call-status aria-live="polite">正在检查电话通道…</p><p data-call-duration></p><audio autoplay></audio><button type="button" class="button secondary" data-enable-audio hidden>播放通话声音</button><footer><button type="button" class="button secondary" data-mute disabled>静音</button><button type="button" class="button danger" data-hangup disabled>挂断</button></footer><small>使用浏览器麦克风和耳机通话</small>`;
  document.body.append(panel);
  const session = { panel, audio: panel.querySelector("audio"), userId: state.auth.user?.id, token: state.auth.token, callId: null, ended: false, refreshing: false };
  browserCallSession = session;
  panel.querySelector("[data-close-call]").addEventListener("click", () => closeBrowserCall({ hangup: !session.ended }));
  panel.querySelector("[data-enable-audio]").addEventListener("click", async event => {
    try { await session.audio.play(); event.currentTarget.hidden = true; } catch (_) { browserCallStatus(session, "无法播放通话声音，请检查耳机或浏览器音频权限"); }
  });
  panel.querySelector("[data-mute]").addEventListener("click", event => {
    const tracks = session.stream?.getAudioTracks() || [];
    const muted = tracks.some(track => track.enabled);
    tracks.forEach(track => { track.enabled = !muted; });
    event.currentTarget.textContent = muted ? "取消静音" : "静音";
  });
  panel.querySelector("[data-hangup]").addEventListener("click", async () => {
    session.hangupInitiated = true;
    stopBrowserCallMedia(session);
    browserCallStatus(session, "正在挂断…");
    try { await requestBrowserCallHangup(session); await refreshBrowserCall(session); }
    catch (_) { browserCallStatus(session, "本地音频已关闭，挂断状态待确认，请重试挂断"); }
  });
  return session;
}

async function openBrowserCall(customerId, incomingCall = null, resolvedCustomer = null) {
  if (browserCallSession && !browserCallSession.ended) { toast("当前已有通话窗口，请先结束该通话"); return; }
  closeBrowserCall();
  const customer = resolvedCustomer || [...customers, ...state.poolCustomers].find(item => String(item.id) === String(customerId));
  if (!incomingCall && !customer) { toast("未找到客户"); return; }
  const session = createBrowserCallPanel(incomingCall?.customer || customer.name, Boolean(incomingCall));
  try {
    requireBackend();
    const config = await apiRequest("/telephony/browser-config");
    if (browserCallSession !== session) return;
    if (!config.configured) throw new Error("电话网关尚未配置，请先接入支持 WebRTC 的电话通道");
    browserCallStatus(session, "请允许浏览器使用麦克风…");
    const offerSdp = await prepareBrowserCallAudio(session, config);
    const requestId = crypto.randomUUID?.() || Array.from(crypto.getRandomValues(new Uint8Array(16)), byte => byte.toString(16).padStart(2, "0")).join("");
    const path = incomingCall ? `/telephony/calls/${incomingCall.id}/answer` : "/telephony/calls";
    const body = incomingCall ? { offerSdp } : { customerId: customer.id, requestId, offerSdp };
    // Keep the same payload for a safe retry when the HTTP response is lost.
    let result;
    try { result = await apiRequest(path, { method: "POST", body: JSON.stringify(body) }); }
    catch (error) {
      if (browserCallSession !== session || state.auth.user?.id !== session.userId) throw error;
      result = await apiRequest(path, { method: "POST", body: JSON.stringify(body) });
    }
    session.callId = result.call.id;
    if (browserCallSession !== session || state.auth.user?.id !== session.userId) { await requestBrowserCallHangup(session); return; }
    session.panel.querySelector("[data-hangup]").disabled = false;
    session.panel.querySelector("[data-mute]").disabled = false;
    browserCallStatus(session, incomingCall ? "正在接听…" : "正在发起呼叫…");
    session.timer = setInterval(() => refreshBrowserCall(session), 2000);
    await refreshBrowserCall(session);
  } catch (error) {
    stopBrowserCallMedia(session);
    if (session.callId) requestBrowserCallHangup(session).catch(() => {});
    const message = error.name === "NotAllowedError" ? "麦克风权限未开启，请允许访问麦克风后重试" : error.message;
    browserCallStatus(session, message);
    session.ended = true;
  }
}

async function refreshBrowserIncomingCalls() {
  if (!state.auth.user || !state.auth.token || !state.backendOnline || telephonyIncomingLoading) return;
  const account = state.auth.user.id;
  telephonyIncomingLoading = true;
  try {
    const calls = await apiRequest("/telephony/incoming");
    if (state.auth.user?.id !== account || telephonyIncomingAccount !== account) return;
    let region = document.querySelector("#browserIncomingCalls");
    if (!region) { region = document.createElement("aside"); region.id = "browserIncomingCalls"; region.className = "browser-incoming-calls"; region.setAttribute("aria-live", "polite"); document.body.append(region); }
    const visible = calls.filter(call => call.id !== browserCallSession?.callId);
    const html = visible.map(call => `<article><div><strong>客户来电 · ${escapeHtml(call.customer)}</strong><p>${escapeHtml(call.phone)}</p></div><button class="button primary" type="button" data-answer-call="${call.id}">接听</button><button class="button secondary" type="button" data-reject-call="${call.id}">拒接</button></article>`).join("");
    if (region.innerHTML === html) return;
    region.innerHTML = html;
    region.querySelectorAll("[data-answer-call]").forEach(button => button.addEventListener("click", () => {
      const call = calls.find(item => String(item.id) === button.dataset.answerCall);
      if (call) openBrowserCall(call.customerId, call);
    }));
    region.querySelectorAll("[data-reject-call]").forEach(button => button.addEventListener("click", async () => {
      button.disabled = true;
      try { await apiRequest(`/telephony/calls/${button.dataset.rejectCall}/hangup`, { method: "POST" }); await refreshBrowserIncomingCalls(); }
      catch (error) { toast(`拒接失败：${error.message}`); button.disabled = false; }
    }));
  } catch (_) {
    // Foreground controls report errors. Background checks keep the working page stable.
  } finally { telephonyIncomingLoading = false; }
}

function startBrowserTelephony() {
  if (!state.auth.user || telephonyMonitorTimer) return;
  telephonyIncomingAccount = state.auth.user.id;
  refreshBrowserIncomingCalls();
  telephonyMonitorTimer = setInterval(refreshBrowserIncomingCalls, 3000);
}

document.addEventListener("visibilitychange", () => { if (!document.hidden) refreshBrowserIncomingCalls(); });
window.addEventListener("pagehide", () => closeBrowserCall({ hangup: true }));
if (state.auth.user) startBrowserTelephony();
