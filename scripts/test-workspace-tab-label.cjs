const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const src=fs.readFileSync('app.js','utf8');
const stored=[{view:'customers',section:'公海列表',detailId:'1430038043',label:'测试资源，无需跟进[1430038043]'}];
const state={workspaceTabsLoaded:false,poolCustomers:[]};
const ctx=vm.createContext({state,customers:[],localStorage:{getItem:key=>key==='youai.crm.workspaceTabs'?JSON.stringify(stored):''},viewMeta:{customers:{}},workspaceTabId:()=>'',workspaceTabLabel:()=>'',enforceCustomerImportAccess:()=>{}});
vm.runInContext(src.match(/^function loadWorkspaceTabs\([^]*?^\}/m)[0],ctx);ctx.loadWorkspaceTabs();assert.equal(state.workspaceTabs[0].label,stored[0].label);
console.log('PASS: refresh preserves customer name when detail is not cached');
