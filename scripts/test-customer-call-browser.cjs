const {chromium} = require('playwright');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({channel:'msedge', headless:true});
  try {
    const page = await browser.newPage({viewport:{width:1440,height:1000}});
    const errors = []; page.on('pageerror', e => errors.push(e.message));
    await page.setContent('<html lang="zh"><body><main></main></body></html>');
    await page.addStyleTag({path:'styles.css'});
    await page.evaluate(() => {
      window.state = {auth:{user:{id:1,displayName:'测试销售'},token:'test'}, customerDetailId:'C1', customerCalls:[], poolCustomers:[], systemUsers:[], view:'customers'};
      window.customers = [{id:'C1',name:'测试客户'}]; window.calls = []; window.fixture = [];
      window.escapeHtml = s => String(s ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('"','&quot;');
      window.formatDateTime = s => s; window.formatDuration = s => String(s);
      window.normalizeCall = c => ({...c,started:c.startedAt}); window.isAdmin = () => false;
      window.pageContent = d => ({...d,page:d.number});
      window.render = () => { document.querySelector('main').innerHTML = '<button data-upload-recording="C1">上传录音</button>' + customerCallsView(customers[0]); };
      window.apiRequest = async (url, options={}) => {
        if (url === '/recording-uploads') return {direct:false};
        if (url.endsWith('/playback')) return {url:null};
        if (url === '/call-uploads') {
          const body = options.body;
          if (body.get('customerId') !== 'C1' || body.has('agentUserId') || body.has('startedAt') || body.has('durationSeconds')) throw Error('wrong association');
          const c = {id:7,customerId:'C1',customer:'测试客户',agent:'测试销售',recordingSource:'UPLOAD',startedAt:'2026-10-23T11:00:00',uploadedAt:'2026-10-23T11:00:00',direction:'外部通话',status:'已接通',durationSeconds:12,note:'<img src=x onerror=alert(1)>'};
          fixture.push(c); return c;
        }
        if (url.startsWith('/customers/C1/calls')) return {content:fixture,number:0,totalElements:fixture.length,totalPages:1};
        if (url.endsWith('/analysis') || url.includes('/transcriptions/')) return {configured:false,status:'PENDING'};
        throw Error('该通话暂无录音');
      };
    });
    await page.addScriptTag({path:'recordings.js'});
    await page.evaluate(() => loadCustomerCalls('C1'));
    await page.getByRole('button',{name:'上传录音',exact:true}).click();
    assert.equal(await page.locator('[name=agentUserId], [name=startedAt], [name=durationSeconds]').count(), 0);
    await page.locator('input[type=file]').setInputFiles({name:'test.wav',mimeType:'audio/wav',buffer:Buffer.from('fixture')});
    await page.locator('form input[type=checkbox]').check();
    await page.getByRole('button',{name:'上传并保存'}).click();
    await page.waitForSelector('[data-call-recording="7"]');
    assert.match(await page.locator('main').innerText(), /测试销售/);
    assert.match(await page.locator('main').innerText(), /上传录音/);
    assert.equal(await page.locator('main img').count(),0);
    assert.match(await page.locator('[data-customer]').innerText(), /通话销售：测试销售/);
    await page.getByRole('button',{name:'关闭',exact:true}).click();
    await page.locator('[data-call-recording="7"]').click();
    await page.waitForSelector('[data-ai-status]');
    assert.match(await page.locator('[data-ai-status]').innerText(), /尚未配置/);
    await page.screenshot({path:'.tmp_recording_browser/customer-calls.png',fullPage:true});
    await page.getByRole('button',{name:'关闭',exact:true}).click();
    await page.evaluate(() => {
      [...calls, ...state.customerCalls].forEach(call => { call.canAccessRecording = false; });
      openCallRecording(7);
    });
    assert.equal(await page.locator('dialog').count(), 0, 'transferred calls cannot open recording dialog');
    assert.deepEqual(errors,[]);
    console.log('Customer call upload, association, shared recording, AI access and escaping passed');
  } finally { await browser.close(); }
})().catch(e => {console.error(e);process.exit(1);});
