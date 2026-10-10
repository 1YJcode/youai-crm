const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app.js', 'utf8');
const state = {customerTagFilters: [], poolTagFilters: [], customerPage: 4, poolCustomerPage: 5, whiteboardCustomerPage: 6};
let activePool = false, renders = 0, requests = 0;
const handlers = new Map();
const buttons = ['customers', 'pool'].flatMap(list => ['__none__', '重点客户', '普通客户'].map(tag => ({dataset: {customerTag: tag, customerTagList: list},addEventListener(type, callback) {handlers.set(`${list}:${tag}`, callback);}})));
const context = vm.createContext({state, URLSearchParams, render: () => renders++, refreshCustomerSearchFromApi: () => requests++, document: {
 querySelectorAll: () => buttons,
 querySelector: selector => selector === '.pool-page' ? (activePool ? {} : null) : selector === '.customer-list-page' ? {} : selector === '#resetCustomerFilters' ? {addEventListener(type, callback) {handlers.set('reset',callback);}} : null
}});
vm.runInContext(source.slice(source.indexOf('function selectedCustomerTagFilters('),source.indexOf('function customerListView(')),context);
vm.runInContext(source.slice(source.indexOf('  document.querySelectorAll("[data-customer-tag]")'),source.indexOf('  document.querySelectorAll("[data-customer-scope]")')),context);
const resetStart = source.indexOf('  document.querySelector("#resetCustomerFilters")');
vm.runInContext(source.slice(resetStart,source.indexOf('  document.querySelectorAll("[data-select-customer]")', resetStart)),context);
const click = (list,tag) => handlers.get(`${list}:${tag}`)();
click('customers','重点客户'); click('customers','普通客户');
assert.deepEqual(Array.from(state.customerTagFilters),['重点客户','普通客户']); assert.equal(state.customerPage,1); assert.equal(state.poolCustomerPage,5);
click('pool','__none__'); click('pool','重点客户');
assert.deepEqual(Array.from(state.poolTagFilters),['__none__','重点客户']); assert.equal(state.whiteboardCustomerPage,6);
const customerButtons=vm.runInContext('customerTagFilterButtons(false)', context);
const poolButtons=vm.runInContext('customerTagFilterButtons(true)', context);
assert.equal((customerButtons.match(/aria-pressed="true"/g)||[]).length,2);
assert(poolButtons.includes('data-customer-tag="__none__" data-customer-tag-list="pool" aria-pressed="true"'));
const params=new URLSearchParams(); context.params=params; vm.runInContext('appendCustomerTagFilterParams(params,true)',context);
assert.equal(params.get('tagMatch'),'any');assert.equal(params.get('tag'),'重点客户');assert.equal(params.get('noTag'),'true');
context.customer={tags:[]};assert.equal(vm.runInContext('customerMatchesTagFilters(customer,state.poolTagFilters)',context),true);
context.customer={tags:['重点客户','普通客户']};assert.equal(vm.runInContext('customerMatchesTagFilters(customer,state.poolTagFilters)',context),true);
context.customer={tags:['其他']};assert.equal(vm.runInContext('customerMatchesTagFilters(customer,state.poolTagFilters)',context),false);
click('customers','普通客户'); assert.deepEqual(Array.from(state.customerTagFilters),['重点客户']);
activePool=true; handlers.get('reset')(); assert.equal(state.poolTagFilters.length,0); assert.deepEqual(Array.from(state.customerTagFilters),['重点客户']);
click('pool','普通客户'); activePool=false; handlers.get('reset')(); assert.equal(state.customerTagFilters.length,0);assert.deepEqual(Array.from(state.poolTagFilters),['普通客户']);
const clearParams=new URLSearchParams();context.params=clearParams;vm.runInContext('appendCustomerTagFilterParams(params,false)',context);assert.equal(clearParams.toString(),'');
const css=fs.readFileSync('styles.css','utf8');assert(css.includes('.tag-filter.active::before { content: "✓"'));
console.log('PASS: independent multi-select tags, toggle, pressed state, OR matching, params and isolated resets');
