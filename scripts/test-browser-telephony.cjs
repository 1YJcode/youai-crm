// Runs a real browser WebRTC loopback with fake microphone audio and an in-page gateway fixture.
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');
const assert = require('node:assert/strict');
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const root = path.resolve(__dirname, '..');

(async () => {
  const executable = process.env.BROWSER_EXE || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
  const profile = path.join(root, `.tmp_browser_telephony_${Date.now()}`);
  fs.mkdirSync(profile);
  const server = http.createServer((request, response) => {
    response.setHeader('Content-Type', 'text/html; charset=utf-8');
    response.end(fs.readFileSync(path.join(root, 'index.html'), 'utf8').replace(/<script[^]*?<\/script>/g, '').replace(/<link[^>]+>/g, ''));
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = spawn(executable, ['--headless=new', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
    '--no-first-run', '--no-default-browser-check', '--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream', 'about:blank'],
    {windowsHide: true, stdio: 'ignore'});
  let socket;
  try {
    const portFile = path.join(profile, 'DevToolsActivePort');
    for (let i = 0; i < 150 && !fs.existsSync(portFile); i++) await delay(100);
    const port = fs.readFileSync(portFile, 'utf8').split('\n')[0];
    const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
    socket = new WebSocket(targets.find(target => target.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    const pending = new Map();
    let nextId = 0;
    socket.onmessage = event => {
      const message = JSON.parse(event.data);
      const request = pending.get(message.id);
      if (!request) return;
      pending.delete(message.id);
      message.error ? request.reject(new Error(JSON.stringify(message.error))) : request.resolve(message.result);
    };
    const command = (method, params = {}) => new Promise((resolve, reject) => {
      const id = ++nextId;
      pending.set(id, {resolve, reject});
      socket.send(JSON.stringify({id, method, params}));
    });
    const evaluate = async expression => {
      const result = await command('Runtime.evaluate', {expression, awaitPromise: true, returnByValue: true});
      if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
      return result.result.value;
    };
    const waitFor = async expression => {
      for (let i = 0; i < 150; i++) { if (await evaluate(expression)) return; await delay(100); }
      throw new Error('Timed out waiting for ' + expression);
    };
    await command('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/`});
    await waitFor('document.readyState === "complete"');
    await evaluate(fs.readFileSync(path.join(root, 'app.js'), 'utf8').replace(/initialize\(\);\s*$/, ''));
    await evaluate(`
      state.auth.user = {id: 1, username: 'test', roles: ['ADMIN']};
      state.auth.token = 'fixture'; state.backendOnline = true;
      window.fixtureCall = null; window.fixtureIncoming = []; window.fixtureHangups = 0;
      window.fixtureConfig = {configured: true, mode: 'WEBRTC', iceServers: []};
      window.remotePeer = null;
      apiRequest = async (path, options = {}) => {
        if (path === '/telephony/browser-config') return fixtureConfig;
        if (path === '/telephony/incoming') return fixtureIncoming;
        if (path.endsWith('/hangup')) { fixtureHangups++; fixtureCall.call.gatewayState = 'ENDED'; return fixtureCall; }
        if (options.method === 'POST' && (path === '/telephony/calls' || path.endsWith('/answer'))) {
          const body = JSON.parse(options.body);
          window.remotePeer = new RTCPeerConnection({iceServers: []});
          const remoteStream = await navigator.mediaDevices.getUserMedia({audio: true});
          window.remoteStream = remoteStream;
          remoteStream.getTracks().forEach(track => remotePeer.addTrack(track, remoteStream));
          await remotePeer.setRemoteDescription({type: 'offer', sdp: body.offerSdp});
          await remotePeer.setLocalDescription(await remotePeer.createAnswer());
          await new Promise(resolve => {
            if (remotePeer.iceGatheringState === 'complete') return resolve();
            remotePeer.addEventListener('icegatheringstatechange', () => { if (remotePeer.iceGatheringState === 'complete') resolve(); });
          });
          fixtureCall = {call: {id: path.endsWith('/answer') ? 502 : 501, status: '已接通', gatewayState: 'CONNECTED', durationSeconds: 1}, answerSdp: remotePeer.localDescription.sdp};
          return fixtureCall;
        }
        if (path.startsWith('/telephony/calls/')) return fixtureCall;
        return [];
      };
      window.fetch = async (url, options) => {
        if (String(url).includes('/telephony/calls/') && String(url).endsWith('/hangup')) {
          fixtureHangups++;
          fixtureCall.call.gatewayState = 'ENDED';
          return new Response(JSON.stringify(fixtureCall), {status: 200, headers: {'Content-Type':'application/json'}});
        }
        throw new Error('Unexpected fixture HTTP request');
      };
      requireBackend = () => {};
    `);
    await evaluate(fs.readFileSync(path.join(root, 'followup-work.js'), 'utf8'));
    await evaluate(fs.readFileSync(path.join(root, 'recordings.js'), 'utf8'));
    await evaluate(fs.readFileSync(path.join(root, 'telephony.js'), 'utf8'));
    await evaluate(`state.view = 'customers'; state.customerDetailId = customers[0].id; customers[0].contactVisible = true;
      render(); window.savedView = state.view; document.querySelector('[data-call-customer]').click();`);
    await waitFor('browserCallSession?.peer?.connectionState === "connected"');
    assert.equal(await evaluate('browserCallSession.audio.srcObject.getAudioTracks().length'), 1);
    assert.equal(await evaluate('state.view === savedView'), true);
    await evaluate(`document.querySelector('[data-mute]').click()`);
    assert.equal(await evaluate('browserCallSession.stream.getAudioTracks()[0].enabled'), false);
    await evaluate(`document.querySelector('[data-mute]').click(); window.savedTracks = browserCallSession.stream.getTracks(); document.querySelector('[data-hangup]').click()`);
    await waitFor('browserCallSession.ended === true');
    assert.equal(await evaluate('savedTracks.every(track => track.readyState === "ended")'), true);
    await evaluate(`closeBrowserCall(); remotePeer.close(); remoteStream.getTracks().forEach(track => track.stop());
      fixtureConfig.configured = false; openBrowserCall(customers[0].id);`);
    await waitFor('browserCallSession.ended === true');
    assert.match(await evaluate(`document.querySelector('[data-call-status]').textContent`), /电话网关尚未配置/);
    assert.equal(await evaluate('browserCallSession.stream'), null);
    await evaluate(`closeBrowserCall(); fixtureConfig.configured = true;
      fixtureIncoming = [{id:502, customer:'来电客户', customerId:customers[0].id, phone:'13800138000'}];
      refreshBrowserIncomingCalls();`);
    await waitFor('document.querySelector("[data-answer-call]") !== null');
    await evaluate(`document.querySelector('[data-answer-call]').click()`);
    await waitFor('browserCallSession?.peer?.connectionState === "connected"');
    await evaluate(`window.inboundTracks = browserCallSession.stream.getTracks(); stopBrowserTelephony(); remotePeer.close(); remoteStream.getTracks().forEach(track => track.stop());`);
    assert.equal(await evaluate('inboundTracks.every(track => track.readyState === "ended")'), true);
    assert.equal(await evaluate('document.querySelector(".browser-call-panel")'), null);
    console.log('PASS real WebRTC audio loopback, mute, hangup, page preservation, disabled gateway, incoming answer and logout media cleanup');
  } finally {
    socket?.close();
    browser.kill();
    server.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
