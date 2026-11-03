const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app.js','utf8');
const fn = source.match(/^async function loadWorkspaceCustomer\([^]*?^\}/m)[0];
(async () => {
 let paths=[], content=[{id:'1430038043',phone:''}];
 const ctx=vm.createContext({apiRequest:async path=>{paths.push(path);return {content};},pageContent:x=>x,normalizeCustomer:x=>x});
 vm.runInContext(fn,ctx);
 const row=await ctx.loadWorkspaceCustomer({section:'公海列表',detailId:'1430038043'});
 assert.equal(row.phone,''); assert.match(paths[0],/^\/customers\/pool\?keyword=1430038043/);
 content=[{id:'99'}]; await assert.rejects(ctx.loadWorkspaceCustomer({section:'公海列表',detailId:'1430038043'}),/已不在公海/);
 await ctx.loadWorkspaceCustomer({section:'客户列表',detailId:'1'}); assert.equal(paths.at(-1),'/customers/1');
 console.log('PASS: pool tab uses masked pool endpoint, exact ID validation, assigned detail endpoint preserved');
})().catch(e=>{console.error(e);process.exitCode=1;});
