const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app.js', 'utf8');
const requests = [];
const context = vm.createContext({
  AbortController, FormData, Blob, setTimeout, clearTimeout,
  state: {auth: {token: 'test-token'}}, API_BASE: 'https://test.invalid/api',
  fetch: async (url, options) => {
    requests.push({url, options});
    return {ok: true, status: 200, json: async () => ({size: 5}), blob: async () => new Blob(['audio'])};
  }
});
vm.runInContext(source.slice(source.indexOf('async function apiRequest('), source.indexOf('function clearAuth(')), context);
(async () => {
  const body = new FormData(); body.append('file', new Blob(['audio']), 'call.wav');
  await context.apiRequest('/calls/1/recording', {method: 'POST', body, timeoutMs: 120000});
  assert.equal(requests[0].options.headers.Authorization, 'Bearer test-token');
  assert.equal(requests[0].options.headers['Content-Type'], undefined, 'browser must generate multipart boundary');
  assert.equal(requests[0].options.body, body);
  assert.equal(requests[0].options.timeoutMs, undefined);
  const audio = await context.apiRequest('/calls/1/recording', {responseType: 'blob'});
  assert.equal(await audio.text(), 'audio');
  assert.equal(requests[1].options.responseType, undefined);
  await context.apiRequest('/calls', {method: 'POST', body: '{}'});
  assert.equal(requests[2].options.headers['Content-Type'], 'application/json');
  console.log('PASS: authenticated multipart, blob playback and existing JSON requests');
})().catch(error => {console.error(error); process.exitCode = 1;});
