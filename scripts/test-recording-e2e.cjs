const {chromium} = require('playwright');
const http = require('node:http');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const port = Number(process.argv[2]);
const storageMode = process.env.RECORDING_STORAGE_E2E === 'true';
if (!Number.isInteger(port) || port < 1024) throw Error('isolated test backend port required');
const server = http.createServer((req,res) => {
  if (req.url.startsWith('/api/')) {
    const upstream = http.request({hostname:'127.0.0.1',port,path:req.url,method:req.method,headers:req.headers}, response => {
      res.writeHead(response.statusCode,response.headers); response.pipe(res);
    });
    upstream.on('error', () => {res.writeHead(502);res.end();}); req.pipe(upstream); return;
  }
  const pathname = req.url.split('?')[0];
  if (pathname === '/config.js') {res.setHeader('Content-Type','text/javascript');res.end("window.YOUAI_API_BASE='/api';");return;}
  const files = {'/customer-review.js':['customer-review.js','text/javascript'],'/rag.js':['rag.js','text/javascript'],'/followup-work.js':['followup-work.js','text/javascript'],'/knowledge.js':['knowledge.js','text/javascript'], '/recordings.js':['recordings.js','text/javascript'], '/app.js':['app.js','text/javascript'], '/styles.css':['styles.css','text/css'], '/logo-youai.png':['logo-youai.png','image/png']};
  if (files[pathname]) {const [file,type]=files[pathname];res.setHeader('Content-Type',type);res.end(fs.readFileSync(file));return;}
  res.setHeader('Content-Type','text/html; charset=utf-8');
  res.end(fs.readFileSync('index.html'));
});
(async () => {
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  let browser;
  try {
    browser=await chromium.launch({channel:'msedge',headless:true});
    const page=await browser.newPage({viewport:{width:1200,height:900}});
    const errors=[]; page.on('pageerror',error=>errors.push(error.message));
    await page.route('https://cdn.sheetjs.com/**', route => route.fulfill({contentType:'text/javascript',body:''}));
    await page.goto(`http://127.0.0.1:${server.address().port}`);
    await page.locator('#loginForm input[name=username]').fill(['company-case','customer-transcript','call-intelligence'].includes(process.argv[3])?'linxi':'admin');
    await page.locator('#loginForm input[name=password]').fill(['company-case','customer-transcript','call-intelligence'].includes(process.argv[3])?'Linxi@123':'Admin@123');
    await page.locator('#loginForm input[name=captcha]').fill((await page.locator('#refreshCaptcha').textContent()).trim());
    await page.locator('#loginSubmit').click();
    if(process.argv[3]==='customer-review'){
      await page.waitForFunction(()=>Boolean(state.auth.token));
      await page.evaluate(async id=>{const c=normalizeCustomer(await apiRequest(`/customers/${id}`));if(!customers.some(v=>v.id===id))customers.push(c);await openCustomer(id);},process.env.PIPELINE_CUSTOMER_ID);
      await page.locator('[data-customer-detail-tab=ai-analysis]').click();await page.waitForSelector('.review-summary');
      assert.match(await page.locator('.review-summary').innerText(),/费用/);assert.equal(await page.locator('.review-card').count(),5);assert.equal(await page.locator('[data-review-version] option').count(),2);
      await page.locator('[data-review-source]').first().click();await page.waitForSelector('.rag-source-dialog');assert.match(await page.locator('.rag-source-dialog pre').innerText(),/我担心费用/);await page.locator('.rag-source-dialog [data-close]').click();
      const options=await page.locator('[data-review-version] option').evaluateAll(items=>items.map(o=>o.value));await page.locator('[data-review-version]').selectOption(options[1]);await page.waitForFunction(()=>!document.querySelector('[data-review-publish]'));
      await page.locator('[data-review-version]').selectOption(options[0]);await page.waitForSelector('[data-review-publish]');
      await page.screenshot({path:'.tmp_customer_review_page.png',fullPage:true});await page.locator('[data-review-publish]').click();await page.waitForSelector('.review-publish-dialog');
      const form=page.locator('.review-publish-dialog form');assert.equal(await form.evaluate(f=>f.checkValidity()),false);await form.locator('[name=content]').fill('客户A先了解婚恋服务，销售A核实需求后回应费用顾虑。人工确认服务签约，保留可复用的沟通经验。');await form.locator('[name=sharing]').check();assert.equal(await form.evaluate(f=>f.checkValidity()),true);
      await form.locator('[type=submit]').click();await page.waitForFunction(()=>!document.querySelector('.review-publish-dialog'));await page.waitForSelector('.review-published');assert.deepEqual(errors,[]);
      console.log('PASS: cumulative review, five coaching sections, source verification, historical revisions, manual editing and explicit case publication in isolated test database');return;
    }
    if(process.argv[3]==='call-intelligence') {
      await page.waitForFunction(()=>Boolean(state.auth.token));
      await page.evaluate(async id=>{const c=normalizeCustomer(await apiRequest(`/customers/${id}`));if(!customers.some(item=>item.id===id))customers.push(c);await openCustomer(id);},process.env.PIPELINE_CUSTOMER_ID);
      const before=await page.evaluate(async()=>{const r=await apiRequest('/tasks?size=1');return r.totalElements;});
      await page.evaluate(id=>{const c=customers.find(c=>String(c.id)===id);state.poolCustomers=[...state.poolCustomers,c];customers=customers.filter(c=>String(c.id)!==id);render();},process.env.PIPELINE_CUSTOMER_ID);
      await page.locator(`[data-customer-followup="${process.env.PIPELINE_CUSTOMER_ID}"]`).first().click();
      assert.equal(await page.locator('#businessForm [name=customerId]').inputValue(),process.env.PIPELINE_CUSTOMER_ID,'detail entry must preserve customer ID even outside the main list');
      assert.equal(await page.locator('[data-followup-ai-template]').count(),0);await page.waitForSelector('[data-call-ai-upload]');
      await page.evaluate(()=>{closeBusinessModal();openBusinessModal('task',{corner:true,customer:'无编号旧提醒'});});
      assert(await page.locator('#businessForm select[name=customerId]').isVisible());
      await page.locator('#businessForm select[name=customerId]').selectOption(process.env.PIPELINE_CUSTOMER_ID);
      assert.equal(await page.locator('#businessForm input[name=customer]').inputValue(),await page.evaluate(id=>state.poolCustomers.find(c=>c.id===id).name,process.env.PIPELINE_CUSTOMER_ID));
      await page.waitForFunction(()=>document.querySelector('[data-call-draft-state]')?.textContent.includes('请先上传录音'));
      await page.locator('[data-call-ai-upload]').click();
      const wav=Buffer.alloc(16044);wav.write('RIFF');wav.writeUInt32LE(16036,4);wav.write('WAVEfmt ',8);wav.writeUInt32LE(16,16);wav.writeUInt16LE(1,20);wav.writeUInt16LE(1,22);wav.writeUInt32LE(8000,24);wav.writeUInt32LE(16000,28);wav.writeUInt16LE(2,32);wav.writeUInt16LE(16,34);wav.write('data',36);wav.writeUInt32LE(16000,40);
      await page.locator('.recording-dialog input[name=file]').setInputFiles({name:'pipeline.wav',mimeType:'audio/wav',buffer:wav});
      await page.locator('.recording-dialog form input[type=checkbox]').check();await page.getByRole('button',{name:'上传并保存',exact:true}).click();
      await page.waitForSelector('[data-ai-draft]',{timeout:30000});
      assert.equal(await page.locator('[data-ai-draft]').count(),10,'all existing template buttons must have drafts automatically');
      assert.equal(await page.locator('[data-call-ai-generate]').count(),0);
      const firstCall=await page.locator('[data-call-work-select]').inputValue();
      const second=await page.evaluate(async({id,bytes})=>{const body=new FormData();body.append('customerId',id);body.append('autoProcess','true');body.append('processingConsent','true');body.append('file',new Blob([new Uint8Array(bytes)],{type:'audio/wav'}),'second.wav');return apiRequest('/call-uploads',{method:'POST',body});},{id:process.env.PIPELINE_CUSTOMER_ID,bytes:[...wav]});
      await page.waitForFunction(async id=>(await apiRequest('/call-followup-work')).some(w=>w.callId===id&&w.analysisState==='COMPLETED'),second.id);
      await page.evaluate(id=>{closeBusinessModal();openBusinessModal('task',{corner:true,customerId:id,customer:'测试客户'});},process.env.PIPELINE_CUSTOMER_ID);
      await page.waitForFunction(id=>document.querySelector('[data-call-work-select]')?.value===String(id),second.id);
      await page.waitForFunction(()=>document.querySelector('[data-call-work-select] option:checked')?.textContent.includes('咨询服务费用'));
      assert.match(await page.locator('[data-call-work-select] option:checked').innerText(),/咨询服务费用/);
      assert.match(await page.locator('[data-unfollowed-count]').innerText(),/2 通未跟进/);
      await page.locator('[data-call-work-select]').selectOption(firstCall);
      await page.waitForSelector('[data-ai-draft="二通"]');
      assert.match(await page.locator('[data-call-work-select] option:checked').innerText(),/约定周五/);
      await page.locator('#businessForm select[name=type]').selectOption('电话');
      const button=page.locator('[data-ai-draft="一通"]');
      const editor=page.locator('#businessForm [data-followup-rich-content]');
      assert.equal((await editor.innerText()).trim(),'');
      await button.hover();assert(await page.locator('.call-draft-preview').first().isVisible());
      assert.match(await page.locator('.call-draft-preview').first().innerText(),/已加微信/);
      assert.equal((await editor.innerText()).trim(),'');
      await button.click();assert.match(await editor.innerText(),/已加微信/);
      const value=editor.locator('[contenteditable=true]').first();await value.fill('销售已核对：周五下午继续联系');
      await page.locator('[data-followup-template-toggle]').click();await page.locator('[data-ai-draft="二通"]').click();await page.locator('[data-followup-template-toggle]').click();await button.click();
      assert.match(await editor.innerText(),/销售已核对/,'switching drafts must preserve salesperson edits');
      await page.locator('#businessForm [name=createNextTask]').check();
      await page.locator('#businessForm [name=nextTitle]').fill('下次联系客户');
      assert.equal(await page.locator('#businessForm').evaluate(f=>f.checkValidity()),true,'follow-up form must be valid before submit');
      await page.screenshot({path:'.tmp_call_followup_draft.png',fullPage:true});
      await page.locator('#businessForm button[type=submit]').click();
      await page.waitForFunction(()=>document.querySelector('#businessModalBackdrop').hidden);
      const result=await page.evaluate(async id=>({tasks:(await apiRequest('/tasks?size=1')).totalElements,records:await apiRequest(`/customers/${id}/follow-ups`),archive:await apiRequest(`/customers/${id}/transcript`)}),process.env.PIPELINE_CUSTOMER_ID);
      assert.equal(result.tasks,before+2);assert(result.records.some(r=>r.content.includes('销售已核对')));assert.equal(result.archive.totalSegments,2); assert.equal((await page.evaluate(()=>apiRequest('/call-followup-work'))).length,1);
      const submitted=result.records.find(r=>r.content.includes('销售已核对'));assert.equal(submitted.source.callId,Number(firstCall));assert.equal(submitted.source.generationMethod,'AI_CONFIRMED');
      const sources=await page.evaluate(id=>apiRequest(`/call-followup-work/sources?customerNo=${id}`),process.env.PIPELINE_CUSTOMER_ID);assert.equal(sources.find(s=>s.callId===Number(firstCall)).followupStatus,'FOLLOWED');assert.equal(sources.find(s=>s.callId===second.id).followupStatus,'UNFOLLOWED');
      await page.evaluate(async id=>{state.customerDetailTab='calls';await loadCustomerCalls(id);},process.env.PIPELINE_CUSTOMER_ID);
      assert.match(await page.locator('.data-table').innerText(),/约定周五继续联系/);assert.match(await page.locator('.data-table').innerText(),/咨询服务费用/);
      await page.locator('[data-customer-detail-tab=follow]').click();await page.waitForSelector('.followup-source');assert.match(await page.locator('.followup-source').first().innerText(),/AI草稿经确认/);
      await page.locator('.followup-source summary').first().click();await page.locator('[data-followup-source-segment]').first().click();await page.waitForSelector('[data-source-text]:not([hidden])');await page.locator('[data-source-text] summary').click();assert.match(await page.locator('[data-source-text]').innerText(),/周五下午再联系我/);await page.locator('.recording-dialog [data-close]').click();
      await page.locator('[data-customer-detail-tab=ai-analysis]').click();await page.waitForSelector('[data-ai-call-results] [data-rag-advice]');
      await page.locator('[data-ai-call-results] [data-rag-advice]').click();await page.locator('.rag-advice-dialog button[type=submit]').click();
      await page.waitForSelector('.rag-advice-dialog .rag-citation');assert.match(await page.locator('.rag-advice-dialog [data-rag-answer]').innerText(),/建议先确认/);await page.locator('.rag-advice-dialog [data-close]').click();
      await page.evaluate(()=>{state.view='analytics';state.analyticsSection='知识库';render();});await page.waitForSelector('[data-rag-form]');
      await page.locator('[data-rag-form] textarea').fill('如何处理客户顾虑？');await page.locator('[data-rag-form] button[type=submit]').click();await page.waitForSelector('.rag-citation');assert.match(await page.locator('[data-rag-answer]').innerText(),/回访沟通案例/);
      await page.screenshot({path:'.tmp_call_pipeline_browser.png',fullPage:true});assert.deepEqual(errors,[]);
      console.log('PASS: upload automatically transcribes, DeepSeek analyzes and prefills templates, salesperson edits and confirms once, follow-up and customer archive persist.');return;
    }
    if(['company-case','customer-transcript'].includes(process.argv[3])) {
      if(process.argv[3]==='customer-transcript') {
        await page.waitForFunction(()=>Boolean(state.auth.token));
        await page.evaluate(async id=>{const c=normalizeCustomer(await apiRequest(`/customers/${id}`));if(!customers.some(item=>item.id===id))customers.push(c);await openCustomer(id);},process.env.COMPANY_CASE_CUSTOMER_ID);
        await page.locator('[data-customer-detail-tab=transcript]').click();
        await page.waitForFunction(()=>document.querySelectorAll('.transcript-segment').length===2);
        assert.match(await page.locator('.customer-transcript').innerText(),/42秒/);assert.match(await page.locator('.customer-transcript').innerText(),/1分15秒/);
        assert.match(await page.locator('.customer-transcript').innerText(),/林夕/);assert.match(await page.locator('.customer-transcript').innerText(),/陈晨/);
        const play=page.locator('[data-transcript-audio]').first();await play.click();await page.waitForSelector('.recording-dialog audio:not([hidden])');
        await page.evaluate(async()=>{const a=document.querySelector('.recording-dialog audio');a.muted=true;await a.play();a.pause();});await page.locator('.recording-dialog [data-close]').click();
        await page.screenshot({path:'.tmp_customer_transcript_browser.png',fullPage:true});
        await page.locator('[data-transcript-select]').nth(0).check();await page.locator('[data-transcript-select]').nth(1).check();await page.locator('[data-transcript-case]').click();
      } else {
      await page.locator('[data-view=calls]').first().click();
      await page.locator('[data-hover-subnav="通话记录"]').click();
      await page.locator(`[data-call-recording="${process.env.COMPANY_CASE_CALL_ID}"]`).first().click();
      await page.locator('[data-company-case]:not([hidden])').click();
      }
      await page.waitForSelector('#companyCaseTitle');
      assert.equal(await page.locator('.knowledge-dialog textarea[name=content]').inputValue(),'');
      assert.match(await page.locator('[data-private-reference]').textContent(),/PRIVATE-BROWSER-ORIGINAL/);
      let publications=0;page.on('request',r=>{if(r.method()==='POST' && r.url().endsWith('/company-case'))publications++;});
      await page.locator('.knowledge-dialog input[name=title]').fill('公司学习案例');
      await page.locator('.knowledge-dialog textarea[name=content]').fill('PUBLIC-BROWSER-LESSON：先确认需求，再约定跟进时间。');
      await page.getByRole('button',{name:'发布公司案例',exact:true}).click();
      assert.equal(publications,0,'sharing confirmation must be explicit');
      await page.locator('[name=sharingConfirmed]').check();
      await page.getByRole('button',{name:'发布公司案例',exact:true}).click();
      await page.waitForFunction(()=>document.querySelector('[data-visibility]')?.textContent.includes('公司共享案例'));
      assert.equal(publications,1);
      const sharedId=await page.evaluate(async()=>{const r=await apiRequest('/knowledge?keyword=PUBLIC-BROWSER-LESSON');return r.content[0].id;});
      const colleague=await browser.newContext();const other=await colleague.newPage();
      other.on('pageerror',e=>errors.push(e.message));
      await other.route('https://cdn.sheetjs.com/**',r=>r.fulfill({contentType:'text/javascript',body:''}));
      await other.goto(`http://127.0.0.1:${server.address().port}`);
      await other.locator('#loginForm input[name=username]').fill('chenchen');
      await other.locator('#loginForm input[name=password]').fill('Sales@123');
      await other.locator('#loginForm input[name=captcha]').fill((await other.locator('#refreshCaptcha').textContent()).trim());
      await other.locator('#loginSubmit').click();
      await other.locator('[data-view=analytics]').first().click();await other.locator('[data-hover-subnav="知识库"]').click();
      await other.getByLabel('搜索知识').fill('PUBLIC-BROWSER-LESSON');await other.locator('.knowledge-search button').filter({hasText:'搜索'}).click();
      await other.locator(`[data-knowledge-id="${sharedId}"]`).click();
      await other.waitForSelector('[data-visibility]');
      assert.equal(await other.locator('[data-source-play]').count(),0);
      assert(await other.locator('[data-source]').isHidden());
      assert.match(await other.locator('.knowledge-dialog textarea').inputValue(),/PUBLIC-BROWSER-LESSON/);
      assert(!((await other.locator('.knowledge-dialog').innerText()).includes('PRIVATE-BROWSER-ORIGINAL')));
      const access=await other.evaluate(async({sourceId,callId})=>{
        const headers={Authorization:'Bearer '+state.auth.token};
        return {source:(await fetch(`/api/knowledge/${sourceId}`,{headers})).status,audio:(await fetch(`/api/calls/${callId}/recording`,{headers})).status,
          privateSearch:(await apiRequest('/knowledge?keyword=PRIVATE-BROWSER-ORIGINAL')).totalElements};
      },{sourceId:process.env.COMPANY_CASE_SOURCE_ID,callId:process.env.COMPANY_CASE_CALL_ID});
      assert.deepEqual(access,{source:403,audio:403,privateSearch:0});
      await other.screenshot({path:'.tmp_company_case_browser.png',fullPage:true});assert.deepEqual(errors,[]);
      await colleague.close();console.log('PASS: owner publishes a separate company case; colleague reads only curated text and cannot access source, raw transcription or recording.');return;
    }
    if(process.argv[3]==='knowledge') {
      await page.locator('[data-view=analytics]').first().click();
      await page.locator('[data-hover-subnav="知识库"]').click();
      await page.locator('[data-knowledge-new]').click();
      await page.locator('.knowledge-dialog input[name=title]').fill('知识库浏览器验证');
      await page.locator('.knowledge-dialog input[name=category]').fill('服务流程');
      await page.locator('.knowledge-dialog textarea').fill('请先确认客户需求。\n<script>window.knowledgeInjected=true</script>');
      await page.getByRole('button',{name:'保存知识',exact:true}).click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-list]').textContent.includes('知识库浏览器验证'));
      await page.getByLabel('搜索知识').fill('确认客户需求');
      await page.locator('.knowledge-search button').filter({hasText:'搜索'}).click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-status]').textContent==='共 1 篇知识');
      await page.locator('[data-knowledge-id]').first().click();
      assert.match(await page.locator('.knowledge-dialog textarea').inputValue(),/确认客户需求/);
      assert.equal(await page.evaluate(()=>window.knowledgeInjected),undefined);
      await page.locator('.knowledge-dialog textarea').fill('已更新：回访前先确认客户可联系时间。');
      await page.getByRole('button',{name:'保存知识',exact:true}).click();
      await page.locator('.knowledge-search button[type=reset]').click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-list]').textContent.includes('回访前'));
      await page.locator('[data-knowledge-id]').first().click();
      await page.screenshot({path:'.tmp_knowledge_browser.png',fullPage:true});
      await page.locator('[data-delete]').click();await page.locator('[data-delete]').click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-status]').textContent.includes('还没有内容'));
      assert.deepEqual(errors,[]);
      console.log('PASS: full application knowledge navigation, creation, body search, literal script text, editing and confirmed deletion');
      return;
    }
    await page.locator('[data-view=calls]').first().click();
    await page.locator('[data-hover-subnav="通话记录"]').click();
    await page.locator('[data-call-recording]').first().click();
    await page.waitForFunction(()=>document.querySelector('[data-ai-status]').textContent.includes('请先上传'));
    const wav=Buffer.alloc(16044); wav.write('RIFF');wav.writeUInt32LE(16036,4);wav.write('WAVEfmt ',8);wav.writeUInt32LE(16,16);
    wav.writeUInt16LE(1,20);wav.writeUInt16LE(1,22);wav.writeUInt32LE(8000,24);wav.writeUInt32LE(16000,28);
    wav.writeUInt16LE(2,32);wav.writeUInt16LE(16,34);wav.write('data',36);wav.writeUInt32LE(16000,40);
    await page.locator('.recording-dialog input[type=file]').setInputFiles({name:'test.wav',mimeType:'audio/wav',buffer:wav});
    await page.locator('.recording-dialog form input[type=checkbox]').check();
    await page.locator('.recording-dialog').getByRole('button',{name:'上传录音',exact:true}).click();
    await page.waitForFunction(()=>!document.querySelector('audio').hidden && Boolean(document.querySelector('audio').src));
    await page.evaluate(async()=>{const audio=document.querySelector('audio');await audio.play();audio.pause();});
    if(!storageMode){await page.locator('[data-ai-consent]').check();await page.locator('[data-analyze]').click();}
    if (storageMode) {
      await page.waitForFunction(()=>document.querySelector('[data-ai-status]').textContent.includes('转写完成'));
      assert.match(await page.locator('[data-transcript]').textContent(),/周五下午/);
      await page.locator('[data-close]').click(); await page.locator('[data-call-recording]').first().click();
      await page.waitForFunction(()=>document.querySelector('[data-transcript]').textContent.includes('周五下午'));
      await page.locator('.recording-dialog input[type=file]').setInputFiles({name:'replacement.wav',mimeType:'audio/wav',buffer:wav});
      await page.locator('.recording-dialog form input[type=checkbox]').check();
      await page.locator('.recording-dialog').getByRole('button',{name:'上传录音',exact:true}).click();
      await page.waitForFunction(()=>document.querySelector('.recording-dialog form input[type=file]').files.length===0);
      await page.waitForFunction(()=>document.querySelector('[data-ai-status]').textContent.includes('转写完成'));
      await page.locator('[data-history] summary').click();
      await page.waitForFunction(()=>document.querySelector('[data-history-content]').textContent.includes('周五下午'));
      const previousAudio = await page.locator('audio').getAttribute('src');
      await page.locator('[data-history-content]').getByRole('button',{name:/播放历史录音/}).first().click();
      await page.waitForFunction(previous => document.querySelector('audio').getAttribute('src') !== previous, previousAudio);
      await page.evaluate(async()=>{const audio=document.querySelector('audio');await audio.play();audio.pause();});
      await page.screenshot({path:'.tmp_recording_browser/storage-history.png',fullPage:true});
    } else {
    await page.waitForFunction(()=>!document.querySelector('[data-review]').hidden);
    assert.match(await page.locator('[data-transcript]').textContent(),/周五下午/);
    await page.locator('[data-review-text]').fill('已核对：周五下午回访，先短信确认时间。');
    await page.locator('[data-save-review]').click();
    await page.waitForFunction(()=>document.querySelector('[data-review-status]').textContent.includes('核对结果已保存'));
    await page.locator('[data-close]').click();await page.locator('[data-call-recording]').first().click();
    await page.waitForFunction(()=>document.querySelector('[data-review-text]').value.includes('先短信确认'));
    assert.match(await page.locator('[data-review-status]').textContent(),/admin/);
    }
    await page.locator('[data-close]').click();
    await page.evaluate(async () => {
      const id = calls[0].customerId;
      if (!customers.some(c => c.id === id)) customers.push(normalizeCustomer(await apiRequest(`/customers/${encodeURIComponent(id)}`)));
      await openCustomer(id);
    });
    await page.locator('[data-customer-detail-tab="calls"]').click();
    await page.waitForSelector('[data-refresh-customer-calls]');
    await page.waitForFunction(() => !state.customerCallsLoading);
    assert.match(await page.locator('.profile-reference-content').innerText(), /上传录音/);
    await page.locator('[data-upload-recording]').click();
    await page.locator('.recording-dialog input[name=file]').setInputFiles({name:'customer.wav',mimeType:'audio/wav',buffer:wav});
    await page.locator('.recording-dialog form input[type=checkbox]').check();
    await page.getByRole('button',{name:'上传并保存',exact:true}).click();
    await page.waitForSelector('.recording-dialog audio:not([hidden])');
    await page.evaluate(async()=>{const audio=document.querySelector('.recording-dialog audio');await audio.play();audio.pause();});
    await page.locator('[data-close]').click();
    await page.waitForFunction(() => !state.customerCallsLoading);
    assert.match(await page.locator('.profile-reference-content').innerText(), /上传录音/);
    await page.screenshot({path:'.tmp_recording_browser/customer-calls-full-app.png',fullPage:true});
    assert.deepEqual(errors,[]);
    console.log(storageMode ? 'PASS: login, upload, playback, persistent transcription queue, reload, replacement, history and customer upload' : 'PASS: browser → JWT login → real upload/playback → async mock-AI analysis → real review persistence and reload');
  } finally {if(browser) await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
