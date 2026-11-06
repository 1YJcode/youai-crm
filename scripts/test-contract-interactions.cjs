const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const deferred = () => { let resolve; const promise = new Promise(r => resolve = r); return {promise, resolve}; };
(async () => {
 const browser = await chromium.launch({channel:'msedge',headless:true});
 try {
  const page = await browser.newPage({viewport:{width:1440,height:900}});
  const errors=[]; page.on('pageerror',e=>errors.push(e.message));
  let fail=false, shrink=false, listGate=null, detailGate=null, reviewGate=null, reviewed=false, conflict=false;
  const requests=[]; const fixture={contractNo:'HT-619901',orderId:'619901',customerId:'1001',customer:'测试会员',owner:'张三',status:'待审核',createdAt:'2026-11-06T10:00:00',version:3};
  await page.route('http://youai.test/**',async route=>{
   const req=route.request(), u=new URL(req.url());
   if (!u.pathname.startsWith('/api/')) return route.fulfill({contentType:'text/html',body:fs.readFileSync('index.html','utf8').replace(/<script[^]*?<\/script>/g,'')});
   requests.push(u.pathname+u.search);
   const response={...fixture,status:reviewed?'审核通过':'待审核',version:reviewed?4:3};
   if(u.searchParams.get('customerId')==='slow') response.customer='旧结果';
   if(u.searchParams.get('customerId')==='fast') response.customer='新结果';
   if(req.headers().authorization==='Bearer second-account') response.customer='新账号结果';
   let body, status=200;
   if(u.pathname.endsWith('/owners')) body=['张三','李四','无合同员工'];
   else if(u.pathname.endsWith('/review')) {
    const payload=req.postDataJSON(); assert.equal(payload.version,3);
    if(reviewGate){const gate=reviewGate;reviewGate=null;gate.started.resolve();await gate.release.promise;}
    if(conflict){status=409;body={message:'合同已被其他人更新，请刷新详情后重试'};}
    else {reviewed=true;body={...response,status:'审核通过',version:4};}
   } else if(u.pathname==='/api/contracts') {
    const gate=listGate;if(gate){listGate=null;gate.started.resolve();await gate.release.promise;}
    if(fail){status=503;body={message:'测试服务暂时不可用'};}
    else {const empty=u.searchParams.get('customerId')==='999'||(shrink&&u.searchParams.get('page')==='1');body={content:empty?[]:[response],totalElements:shrink?1:empty?0:21,totalPages:shrink?1:empty?0:3,number:Number(u.searchParams.get('page')||0),size:10};}
   } else {
    if(detailGate){const gate=detailGate;detailGate=null;gate.started.resolve();await gate.release.promise;}
    body=response;
   }
   await route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
  });
  await page.goto('http://youai.test/'); await page.addStyleTag({path:'styles.css'});
  await page.evaluate(()=>window.YOUAI_API_BASE='http://youai.test/api');
  await page.addScriptTag({content:fs.readFileSync('app.js','utf8').replace(/initialize\(\);\s*$/,'')});
  await page.evaluate(()=>{state.auth.user={id:1,username:'test',displayName:'测试',roles:['ADMIN']};state.auth.token='fixture';document.body.classList.remove('app-booting');state.view='orders';state.orderSection='合同列表';render();});
  const loaded=()=>page.waitForFunction(()=>!contractData.loading&&contractData.rows.length===1);
  await loaded();
  await page.getByLabel('会员ID',{exact:true}).fill('999');await page.getByRole('button',{name:'查询',exact:true}).click();await page.getByText('暂无数据',{exact:true}).waitFor();
  await page.getByRole('button',{name:'重置',exact:true}).click();await loaded();assert(requests.some(r=>r.includes('customerId=999')));
  await page.getByLabel('归属人',{exact:true}).selectOption('无合同员工');await page.getByLabel('状态',{exact:true}).selectOption('待审核');await page.getByRole('button',{name:'查询',exact:true}).click();await loaded();assert(requests.some(r=>r.includes('owner=')&&r.includes('status=')));
  await page.getByRole('button',{name:'重置',exact:true}).click();await loaded();
  shrink=true;await page.locator('[data-contract-page="1"]').click();await page.waitForFunction(()=>!contractData.loading&&contractData.page===0);shrink=false;
  fail=true;await page.evaluate(()=>loadContracts());await page.getByText('合同查询失败',{exact:true}).waitFor();assert.equal(await page.locator('[data-contract-detail]').count(),0);
  fail=false;await page.getByRole('button',{name:'重试',exact:true}).click();await loaded();
  const gate={started:deferred(),release:deferred()};listGate=gate;
  await page.evaluate(()=>{void loadContracts();});await gate.started.promise;
  assert.equal(await page.getByRole('button',{name:'查询',exact:true}).isDisabled(),true);
  gate.release.resolve();await loaded();
  const stale={started:deferred(),release:deferred()};listGate=stale;
  await page.evaluate(()=>{contractData.filters.customerId='slow';window.slowList=loadContracts();});await stale.started.promise;
  await page.evaluate(()=>{contractData.filters.customerId='fast';return loadContracts();});await loaded();
  stale.release.resolve();await page.evaluate(()=>window.slowList);assert.equal(await page.evaluate(()=>contractData.rows[0].customer),'新结果');
  const account={started:deferred(),release:deferred()};listGate=account;
  await page.evaluate(()=>{window.oldAccountList=loadContracts();});await account.started.promise;
  await page.evaluate(()=>{state.auth.token='second-account';return loadContracts();});await loaded();
  account.release.resolve();await page.evaluate(()=>window.oldAccountList);assert.equal(await page.evaluate(()=>contractData.rows[0].customer),'新账号结果');
  await page.evaluate(()=>{state.auth.token='fixture';return loadContracts();});await loaded();
  const slow={started:deferred(),release:deferred()};detailGate=slow;
  await page.evaluate(()=>{window.pendingContract=openContract('HT-619901');});await slow.started.promise;
  await page.evaluate(()=>closeDrawer());slow.release.resolve();await page.evaluate(()=>window.pendingContract);
  assert.equal(await page.locator('#detailDrawer').evaluate(e=>e.classList.contains('open')),false);
  await page.getByRole('button',{name:'详情',exact:true}).click();await page.getByRole('heading',{name:'合同详情'}).waitFor();
  conflict=true;await page.getByRole('button',{name:'审核通过',exact:true}).click();await page.getByText('合同已被其他人更新，请刷新详情后重试',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:'审核通过',exact:true}).isEnabled(),true);conflict=false;
  const saving={started:deferred(),release:deferred()};reviewGate=saving;
  await page.getByRole('button',{name:'审核通过',exact:true}).click();await saving.started.promise;
  assert.equal(await page.getByRole('button',{name:'审核驳回',exact:true}).isDisabled(),true);
  await page.evaluate(()=>closeDrawer());const listDone=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/contracts');saving.release.resolve();await listDone;await loaded();
  assert.equal(await page.locator('#detailDrawer').evaluate(e=>e.classList.contains('open')),false);
  assert.deepEqual(errors,[]);
  console.log('PASS: filters, reset, empty results, page fallback, error/retry, loading guards, closed detail, review conflict recovery, duplicate-submit guard, closed review, version payload, out-of-order requests, account switching, no browser errors');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
