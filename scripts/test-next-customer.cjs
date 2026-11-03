const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app.js', 'utf8');
const start = source.indexOf('  document.querySelectorAll("[data-next-customer]").forEach(button => button.addEventListener("click", async');
const end = source.indexOf('  document.querySelector("[data-new-order-customer]")', start);
async function run({section = '公海列表', pool = [], customers = [], meta = {}, online = false, load = async () => []} = {}) {
  let handler;
  const opened = [], messages = [];
  const state = {customerSection: section, poolCustomers: pool, whiteboardCustomers: [], poolCustomerPageMeta: meta, customerPageMeta: meta, poolCustomerPage: 1, customerPage: 1, backendOnline: online};
  vm.runInNewContext(source.slice(start, end), {state, customers, document: {querySelectorAll: () => [{addEventListener: (_, fn) => handler = fn}]}, sortedCustomerRows: rows => rows, visibleCustomerLibraryRows: rows => rows, refreshCustomerSearchFromApi: load, openCustomer: async id => opened.push(id), toast: message => messages.push(message)});
  await handler({currentTarget: {dataset: {nextCustomer: '1'}}});
  assert.equal(state.nextCustomerPending, false);
  return {opened, messages, state};
}
(async () => {
  assert.deepEqual((await run({pool: [{id: '1'}, {id: '2'}], customers: [{id: '99'}]})).opened, ['2']);
  assert.equal((await run({pool: [{id: '1'}]})).messages[0], '当前列表没有下一个客户');
  assert.equal((await run()).messages.length, 1);
  let options;
  const paged = await run({pool: [{id: '1'}], online: true, meta: {page: 0, totalPages: 2}, load: async opts => {options = opts; return [{id: '3'}];}});
  assert.deepEqual(paged.opened, ['3']); assert.equal(options.section, '公海列表'); assert.equal(paged.state.poolCustomerPage, 2);
  const failed = await run({pool: [{id: '1'}], online: true, meta: {page: 0, totalPages: 2}, load: async () => {throw Error('network');}});
  assert.equal(failed.state.poolCustomerPage, 1); assert.match(failed.messages[0], /切换客户失败/);
  assert.deepEqual((await run({section: '客户列表', customers: [{id: '1'}, {id: '4'}]})).opened, ['4']);
  console.log('PASS: pool isolation, next customer, end/empty feedback, pagination, failure recovery, assigned customers');
})().catch(error => {console.error(error); process.exitCode = 1;});
