const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch({channel: 'msedge', headless: true});
  try {
    const page = await browser.newPage({viewport: {width: 1200, height: 900}});
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.setContent('<html lang="zh"><body><button data-call-recording="1">录音管理</button></body></html>');
    await page.addStyleTag({path: 'styles.css'});
    await page.evaluate(() => {
      window.state = {auth: {user: {id: 1}, token: 'fixture-token'}};
      window.calls = [{id: 1, customer: '测试客户', started: '测试通话'}];
      window.fixture = {uploaded: false, status: 'NO_RECORDING', version: 0, text: '', posts: 0, revoked: 0};
      const revoke = URL.revokeObjectURL.bind(URL);
      URL.revokeObjectURL = url => {fixture.revoked++; revoke(url);};
      window.apiRequest = async (url, options = {}) => {
        const method = options.method || 'GET';
        if (url === '/recording-uploads') return {direct:false};
        if (url.endsWith('/playback')) return {url:null};
        if (url.endsWith('/review')) {
          if (method === 'PUT') {const body = JSON.parse(options.body); fixture.text = body.text; fixture.version++; return null;}
          return {revision: 'test-revision', started: '2026-10-17T09:00:00', version: fixture.version, text: fixture.text || '请周五回访', reviewer: 'tester', reviewedAt: fixture.version ? '2026-10-17T10:00:00' : null};
        }
        if (url.endsWith('/analysis') || url.includes('/transcriptions/')) {
          if (method === 'POST') {fixture.posts++; fixture.status = 'PROCESSING'; setTimeout(() => fixture.status = 'COMPLETED', 50);}
          return {configured: true, status: fixture.status, transcript: fixture.status === 'COMPLETED' ? '周五再联系' : null,
            summary: fixture.status === 'COMPLETED' ? '<img src=x onerror="window.injected=true">客户希望周五回访' : null};
        }
        if (method === 'POST') {if (!(options.body instanceof FormData)) throw Error('multipart required'); fixture.uploaded = true; fixture.status = 'PENDING'; return {};}
        if (!fixture.uploaded) throw Error('该通话暂无录音');
        const buffer = new ArrayBuffer(16044); const view = new DataView(buffer);
        const text = (offset, str) => [...str].forEach((c, i) => view.setUint8(offset+i, c.charCodeAt(0)));
        text(0,'RIFF'); view.setUint32(4,16036,true); text(8,'WAVEfmt '); view.setUint32(16,16,true);
        view.setUint16(20,1,true); view.setUint16(22,1,true); view.setUint32(24,8000,true); view.setUint32(28,16000,true);
        view.setUint16(32,2,true); view.setUint16(34,16,true); text(36,'data'); view.setUint32(40,16000,true);
        return new Blob([buffer], {type:'audio/wav'});
      };
    });
    await page.addScriptTag({path: 'recordings.js'});
    await page.getByRole('button', {name: '录音管理'}).click();
    await page.waitForFunction(() => document.querySelector('[data-ai-status]').textContent.includes('请先上传'));
    assert(await page.locator('[data-analyze]').isDisabled());
    await page.locator('input[type=file]').setInputFiles({name:'test.wav', mimeType:'audio/wav', buffer:Buffer.from('fixture')});
    await page.locator('form input[type=checkbox]').check();
    await page.getByRole('button', {name:'上传录音', exact:true}).click();
    await page.waitForFunction(() => document.querySelector('audio').src.startsWith('blob:'));
    await page.evaluate(async () => {const audio=document.querySelector('audio'); await audio.play(); audio.pause();});
    await page.locator('[data-analyze]').click();
    assert.equal(await page.evaluate(() => fixture.posts), 0, 'consent required');
    await page.locator('[data-ai-consent]').check();
    await page.locator('[data-analyze]').click();
    await page.waitForFunction(() => !document.querySelector('[data-review]').hidden);
    assert.equal(await page.locator('[data-summary] img').count(), 0);
    assert.equal(await page.evaluate(() => window.injected), undefined);
    await page.locator('[data-review-text]').fill('人工确认：周五下午回访');
    await page.locator('[data-save-review]').click();
    await page.waitForFunction(() => fixture.version === 1);
    const output = path.resolve('.tmp_recording_browser'); fs.mkdirSync(output, {recursive:true});
    await page.screenshot({path:path.join(output,'review.png'), fullPage:true});
    await page.locator('[data-close]').click();
    assert.equal(await page.locator('dialog').count(), 0);
    assert.equal(await page.evaluate(() => fixture.revoked), 1);
    await page.getByRole('button', {name:'录音管理'}).click();
    await page.waitForFunction(() => document.querySelector('[data-review-text]').value.includes('人工确认'));
    await page.evaluate(() => state.auth.user.id = 2);
    await page.waitForFunction(() => !document.querySelector('dialog'));
    assert.deepEqual(errors, []);
    console.log('PASS: real browser upload UI, WAV playback, consent, analysis polling, XSS-safe output, review persistence and account cleanup (mock API)');
  } finally {await browser.close();}
})().catch(error => {console.error(error); process.exitCode=1;});
