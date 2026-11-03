const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const fn=fs.readFileSync('app.js','utf8').match(/^async function verifyCustomerPhone\([^]*?^\}/m)[0];
async function run(phone,result,error){let calls=0,messages=[],focused=false; const input={value:phone,focus:()=>focused=true},button={disabled:false,textContent:'检测'};
const form={querySelector:s=>s.includes('customerId')?{value:'self'}:input};
const ctx=vm.createContext({document:{querySelector:()=>form},requireBackend:()=>{},apiRequest:async(path,options)=>{calls++;assert.equal(path,'/customers/phone-check');assert.equal(JSON.parse(options.body).customerId,'self');if(error)throw Error('offline');return result;},toast:m=>messages.push(m)});
vm.runInContext(fn,ctx);await ctx.verifyCustomerPhone({currentTarget:button});return {calls,messages,focused,button};}
(async()=>{
assert.equal((await run('123')).calls,0);assert.equal((await run('123')).focused,true);
assert.match((await run('13132196202',{duplicate:true})).messages[0],/已存在/);
assert.match((await run('13132196202',{duplicate:false})).messages[0],/未发现重复/);
const failed=await run('13132196202',null,true);assert.match(failed.messages[0],/检测失败/);assert.equal(failed.button.disabled,false);assert.equal(failed.button.textContent,'检测');
console.log('PASS: format validation, duplicate/unused results, edit exclusion, error feedback and button recovery');
})().catch(e=>{console.error(e);process.exitCode=1;});
