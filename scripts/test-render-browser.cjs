// Run with Playwright installed or NODE_PATH pointing to its package directory.
const {chromium}=require('playwright');
const fs=require('node:fs');
const assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});try{
const page=await browser.newPage({viewport:{width:1440,height:900}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
await page.route('http://youai.test/**',route=>route.fulfill({contentType:'text/html',body:fs.readFileSync('index.html','utf8').replace(/<script[^]*?<\/script>/g,'')}));
await page.goto('http://youai.test/');await page.addStyleTag({path:'styles.css'});
await page.addScriptTag({content:fs.readFileSync('app.js','utf8').replace(/initialize\(\);\s*$/,'')});
await page.evaluate(()=>{state.auth.user={id:1,username:'test',displayName:'浏览器测试',roles:['ADMIN']};state.auth.token='';document.body.classList.remove('app-booting');render();});
const results=[];
for(const view of ['dashboard','customers','tasks','calls','messages','orders','system','analytics','finance']){
await page.evaluate(v=>navigate(v),view);assert.ok((await page.locator('#app').innerText()).length>0, view + JSON.stringify(errors) + await page.evaluate(()=>document.querySelector('#app').innerHTML.slice(0,180)));results.push('页面切换 '+view+' 通过');}
for (const view of ['customers','tasks','calls','messages','orders','system','analytics','finance']) {
  await page.evaluate(v=>navigate(v),view);
  await page.evaluate(()=>{window.savedPanels=[...document.querySelectorAll('#app .data-panel')];lastRenderedViewHtml=null;render();});
  assert.equal(await page.evaluate(()=>window.savedPanels.every((n,i)=>n===document.querySelectorAll('#app .data-panel')[i])),true,view+' unchanged list panels');
  for (const selector of ['[data-customer-scope]','[data-quick-filter]','[data-sincere-scope]','[data-service-scope]','[data-service-scenario]','[data-notification-filter]','[data-call-filter]','[data-call-task-filter]']) {
    const values=await page.locator(selector).evaluateAll(nodes=>nodes.map(n=>({attr:n.getAttributeNames().find(a=>a.startsWith('data-')),value:n.getAttribute(n.getAttributeNames().find(a=>a.startsWith('data-')))})));
    for(const item of values){const target=page.locator(`[${item.attr}="${item.value}"]`).first();if(!await target.isVisible())continue;await target.click();const before=await page.evaluate(()=>{window.repeatRoot=document.querySelector('#app').firstChild;return 0;});await target.click();assert.equal(await page.evaluate(()=>document.querySelector('#app').firstChild===window.repeatRoot),true,view+selector+' repeated click');}
  }
  results.push(view+' 列表框复用及可见单选筛选重复点击 通过');
}
await page.evaluate(()=>navigate('customers'));
await page.evaluate(()=>{state.customerSection='公海列表';state.auth.token='fixture';apiRequest=async()=>({content:[],number:0,size:20,totalElements:0,totalPages:1});render();window.sceneBar=document.querySelector('.customer-scene-bar');window.sceneRequests=0;const api=apiRequest;apiRequest=async()=>{window.sceneRequests++;return api();};});
const sceneKeys=await page.locator('[data-customer-scene]').evaluateAll(nodes=>nodes.map(n=>n.dataset.customerScene));
for(const key of sceneKeys){
const before=await page.evaluate(()=>window.sceneRequests);
const alreadySelected=await page.locator(`[data-customer-scene="${key}"]`).evaluate(n=>n.classList.contains('active'));
await page.locator(`[data-customer-scene="${key}"]`).click();
await page.waitForFunction(()=>!state.customerSearchPending);
assert.equal(await page.evaluate(()=>window.sceneRequests),before+(alreadySelected?0:1),'one request per changed scene');
assert.equal(await page.evaluate(()=>document.querySelector('.customer-scene-bar')===window.sceneBar),true,'scene toolbar must survive');
assert.equal(await page.locator(`[data-customer-scene="${key}"]`).evaluate(n=>n.classList.contains('active')),true);
}
const repeatResult=await page.evaluate(()=>{window.tableBefore=document.querySelector('.data-panel');return window.sceneRequests;});
for(let i=0;i<15;i++) await page.locator('[data-customer-scene="today-follow"]').click();
await page.waitForFunction(()=>!state.customerSearchPending);
const afterFirst=await page.evaluate(()=>{window.tableStable=document.querySelector('.data-panel');return window.sceneRequests;});
for(let i=0;i<15;i++) await page.locator('[data-customer-scene="today-follow"]').click();
assert.equal(await page.evaluate(()=>window.sceneRequests),afterFirst);
assert.equal(await page.evaluate(()=>window.tableStable===document.querySelector('.data-panel')),true);
results.push('连续点击今日待跟进30次：重复点击不请求、不重建列表框 通过');
results.push('公海全部场景实际点击：工具栏保留、选中状态正确、每次仅请求一次 通过');
const r=await page.evaluate(()=>{const root=document.querySelector('#app');render();const child=root.firstChild;render();return root.firstChild===child;});assert.equal(r,true);results.push('相同内容重绘保留 DOM 通过');
await page.evaluate(()=>{const input=document.createElement('textarea');input.id='browserDraft';document.querySelector('#app').append(input);input.value='未提交的测试内容';input.focus();window.testDraft=input;render({skipUnchanged:true});});
assert.equal(await page.locator('#browserDraft').inputValue(),'未提交的测试内容');assert.equal(await page.evaluate(()=>document.activeElement===window.testDraft),true);results.push('输入中后台刷新保留内容和焦点 通过');
await page.locator('#browserDraft').blur();await page.evaluate(()=>render({skipUnchanged:true}));assert.equal(await page.locator('#browserDraft').inputValue(),'未提交的测试内容');results.push('移开焦点后后台刷新仍保留草稿 通过');
await page.evaluate(()=>{document.querySelector('#browserDraft').remove();const d=document.createElement('dialog');d.id='browserDialog';d.innerHTML='<button>关闭</button>';document.querySelector('#app').append(d);d.showModal();render({skipUnchanged:true});});assert.equal(await page.locator('#browserDialog').evaluate(e=>e.open),true);results.push('后台刷新保留弹窗 通过');
await page.locator('#browserDialog').evaluate(e=>e.remove());await page.evaluate(()=>navigate('dashboard'));await page.screenshot({path:'.tmp_flicker_browser.png',fullPage:true});
assert.deepEqual(errors,[]);console.log(JSON.stringify({results,errors,screenshot:'.tmp_flicker_browser.png'},null,2));
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});


