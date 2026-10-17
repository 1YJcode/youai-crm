// Loaded after app.js; recording bytes are fetched with the current user's token.
let closeCallRecording = null;

function openCallRecording(callId) {
  closeCallRecording?.();
  const call = calls.find(item => String(item.id) === String(callId));
  if (!call) return;
  const accountId = state.auth.user?.id;
  const dialog = document.createElement('dialog');
  dialog.className = 'recording-dialog';
  dialog.setAttribute('aria-labelledby', 'recordingTitle');
  dialog.innerHTML = `<header><h2 id="recordingTitle">通话录音</h2><button type="button" class="button secondary" data-close>关闭</button></header>
    <p data-customer></p><p>上传已获得授权的销售与客户通话录音。支持 WAV、MP3、FLAC、OGG，最大 20MB。</p>
    <audio controls preload="none" hidden></audio><p role="status" data-status></p>
    <form><label>选择录音<input type="file" accept=".wav,.mp3,.flac,.ogg" required></label>
    <label><input type="checkbox" required>我确认录音对应当前客户，且有权上传；已有录音将被替换。</label>
    <button class="button primary" type="submit">上传录音</button></form>
    <section><h3>AI 通话整理</h3><p data-ai-status role="status"></p>
    <label><input data-ai-consent type="checkbox">允许将本次录音发送至管理员配置的 OpenAI 服务进行转写和总结。</label>
    <button type="button" class="button primary" data-analyze disabled>开始分析</button>
    <p>请核对录音与转写。AI 内容仅为草稿，不会自动写入客户资料或创建任务。</p>
    <details><summary>转写原文</summary><pre data-transcript></pre></details>
    <h4>AI 原始整理</h4><pre data-summary></pre>
    <section data-review hidden><h4>人工核对</h4><label>确认后的总结<textarea data-review-text rows="9" maxlength="50000"></textarea></label>
    <button type="button" class="button primary" data-save-review>确认保存</button><p data-review-status role="status"></p></section></section>`;
  dialog.querySelector('[data-customer]').textContent = `${call.customer || ''} · ${call.started || ''}`;
  const status = dialog.querySelector('[data-status]');
  const audio = dialog.querySelector('audio');
  let objectUrl = null;
  let closed = false;
  let analysisTimer = null;
  let analysisRequest = 0;
  let reviewSnapshot = null;
  let mutationPending = false;
  let audioRequest = 0;
  const valid = () => !closed && state.auth.user?.id === accountId && Boolean(state.auth.token);
  const cleanup = () => {
    if (closed) return;
    closed = true;
    clearInterval(accountWatcher);
    clearTimeout(analysisTimer);
    audio.pause(); audio.removeAttribute('src'); audio.load();
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    dialog.remove();
    closeCallRecording = null;
  };
  closeCallRecording = cleanup;
  const accountWatcher = setInterval(() => { if (!valid()) cleanup(); }, 500);
  dialog.addEventListener('close', cleanup);
  dialog.addEventListener('cancel', cleanup);
  dialog.querySelector('[data-close]').onclick = cleanup;
  const analyzeButton = dialog.querySelector('[data-analyze]');
  const analysisStatus = dialog.querySelector('[data-ai-status]');
  function showAnalysis(result) {
    if (!valid()) return;
    const labels = {NO_RECORDING: '请先上传录音', PENDING: '待分析', PROCESSING: '正在转写和整理…', COMPLETED: '分析完成，请核对', FAILED: '分析失败，可重试'};
    analysisStatus.textContent = result.configured ? (result.error || labels[result.status] || result.status) : '管理员尚未配置AI服务';
    analyzeButton.disabled = !result.configured || ['NO_RECORDING', 'PROCESSING'].includes(result.status);
    dialog.querySelector('[data-transcript]').textContent = result.transcript || '';
    dialog.querySelector('[data-summary]').textContent = result.summary || '';
    if (result.status === 'COMPLETED') loadReview();
    else { reviewSnapshot = null; dialog.querySelector('[data-review]').hidden = true; }
    clearTimeout(analysisTimer);
    if (result.status === 'PROCESSING') analysisTimer = setTimeout(loadAnalysis, 3000);
  }
  async function loadAnalysis() {
    const request = ++analysisRequest;
    try {
      const result = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/analysis`);
      if (request === analysisRequest) showAnalysis(result);
    } catch (error) { if (valid() && request === analysisRequest) analysisStatus.textContent = error.message; }
  }
  async function loadReview() {
    const request = analysisRequest;
    try {
      const review = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/review`);
      if (!valid() || request !== analysisRequest) return;
      reviewSnapshot = review;
      dialog.querySelector('[data-review]').hidden = false;
      dialog.querySelector('[data-review-text]').value = review.text || '';
      dialog.querySelector('[data-review-status]').textContent = review.reviewedAt ? `已由 ${review.reviewer} 确认保存` : '尚未人工确认';
    } catch (error) { if (valid()) analysisStatus.textContent = error.message; }
  }
  dialog.querySelector('[data-save-review]').onclick = async event => {
    if (!reviewSnapshot || !valid() || mutationPending) return;
    const text = dialog.querySelector('[data-review-text]').value.trim();
    const message = dialog.querySelector('[data-review-status]');
    if (!text) { message.textContent = '请填写核对后的总结。'; return; }
    const button = event.currentTarget;
    const snapshot = reviewSnapshot;
    mutationPending = true;
    button.disabled = true;
    try {
      await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/review`, {method: 'PUT', body: JSON.stringify({...snapshot, text})});
      if (!valid()) return;
      message.textContent = '核对结果已保存';
      snapshot.version += 1;
    } catch (error) { if (valid()) message.textContent = error.message; }
    finally { button.disabled = false; mutationPending = false; }
  };
  analyzeButton.onclick = async () => {
    if (mutationPending || !valid()) return;
    if (!dialog.querySelector('[data-ai-consent]').checked) { analysisStatus.textContent = '请先确认本次录音的AI处理授权。'; return; }
    analyzeButton.disabled = true;
    mutationPending = true;
    const request = ++analysisRequest;
    clearTimeout(analysisTimer);
    try {
      const result = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording/analysis`, {method: 'POST'});
      if (request === analysisRequest) showAnalysis(result);
    }
    catch (error) { if (valid()) { analysisStatus.textContent = error.message; analyzeButton.disabled = false; } }
    finally { mutationPending = false; }
  };
  async function loadAudio() {
    const request = ++audioRequest;
    status.textContent = '正在读取录音…';
    try {
      const blob = await apiRequest(`/calls/${encodeURIComponent(callId)}/recording`, { responseType: 'blob', timeoutMs: 60000 });
      if (!valid() || request !== audioRequest) return;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      objectUrl = URL.createObjectURL(blob);
      audio.src = objectUrl; audio.hidden = false;
      status.textContent = '录音已加载，可播放核对。';
    } catch (error) { if (valid() && request === audioRequest) status.textContent = error.message; }
  }
  dialog.querySelector('form').onsubmit = async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const file = form.querySelector('[type=file]').files[0];
    if (!file || !valid() || mutationPending) return;
    if (!file.size || file.size > 20 * 1024 * 1024) { status.textContent = '请选择非空且不超过20MB的录音。'; return; }
    const body = new FormData(); body.append('file', file);
    ++analysisRequest;
    ++audioRequest;
    mutationPending = true;
    clearTimeout(analysisTimer);
    const button = form.querySelector('button'); button.disabled = true;
    status.textContent = '正在上传…';
    try {
      await apiRequest(`/calls/${encodeURIComponent(callId)}/recording`, { method: 'POST', body, timeoutMs: 120000 });
      if (!valid()) return;
      form.reset();
      dialog.querySelector('[data-ai-consent]').checked = false;
      await loadAnalysis();
      await loadAudio();
    } catch (error) { if (valid()) status.textContent = error.message; }
    finally { button.disabled = false; mutationPending = false; if (valid()) loadAnalysis(); }
  };
  document.body.append(dialog);
  dialog.showModal();
  loadAudio();
  loadAnalysis();
}

document.addEventListener('click', event => {
  const button = event.target.closest('[data-call-recording]');
  if (button) openCallRecording(button.dataset.callRecording);
});
