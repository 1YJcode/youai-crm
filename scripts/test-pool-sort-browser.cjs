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
await page.evaluate(()=>{state.view='customers';state.customerSection='公海列表';state.auth.token='fixture';window.requests=[];window.fixture=customers.slice(0,3).map((c,i)=>({...c,owner:'公海',followUpCount:i})); state.poolCustomers=fixture;apiRequest=async(path)=>{requests.push(path);const p=new URL(path,'http://test').searchParams;let rows=fixture.filter(c=>(!p.has('followUpMin')||c.followUpCount>=Number(p.get('followUpMin')))&&(!p.has('followUpMax')||c.followUpCount<=Number(p.get('followUpMax'))));return {content:rows,number:0,size:20,totalElements:rows.length,totalPages:1};};render();});
await page.locator('[data-customer-sort="followUpCount"]').click();await page.waitForTimeout(200);
console.log('sort',await page.evaluate(()=>({sort:state.customerSort,requests,headers:document.querySelector('[data-customer-sort="followUpCount"]').outerHTML})));
await page.locator('#poolFollowUpMin').fill('2');await page.locator('#applyCustomerFilters').click();await page.waitForTimeout(200);
console.log('range',await page.evaluate(()=>({min:state.poolFollowUpMin,requests,rows:state.poolCustomers.map(c=>c.followUpCount),text:document.querySelector('.customer-detail-table')?.innerText})));
for(const direction of ['desc','asc','desc','asc']) {
  const button=page.locator('button[data-customer-sort="followUpCount"]');
  await button.locator(`[data-customer-sort-direction="${direction}"]`).click();
  await page.waitForTimeout(100);
  assert.equal(await page.locator('th[data-customer-column="followUpCount"]').getAttribute('aria-sort'), direction==='asc'?'ascending':'descending');
  assert.equal(await button.count(),1);
}
await page.locator('#poolFollowUpMin').fill('');await page.locator('#poolFollowUpMax').fill('0');await page.locator('#applyCustomerFilters').click();await page.waitForTimeout(150);
assert.deepEqual(await page.evaluate(()=>state.poolCustomers.map(c=>c.followUpCount)),[0]);
assert.equal(await page.locator('tbody tr').first().locator('td').nth(11).innerText(),'0');
await page.locator('#poolFollowUpMax').fill('');await page.locator('#poolFollowUpMin').fill('2');await page.locator('#applyCustomerFilters').click();await page.waitForTimeout(150);
console.log('PASS: repeated asc/desc, range after sorting, zero count display',errors);assert.equal(await page.evaluate(()=>state.poolCustomers.length),1);assert.equal(await page.evaluate(()=>state.poolCustomers[0].followUpCount),2);
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});

