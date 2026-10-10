const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app.js', 'utf8');
const start = source.indexOf('  ["Min", "Max"].forEach(bound => {');
const end = source.indexOf('  document.querySelectorAll("[data-select-customer]")', start);
const handlers = new Map();
const state = { poolFollowUpMin: '', poolFollowUpMax: '', customerPage: 3, poolCustomerPage: 3 };
let requests = 0, renders = 0;
const messages = [];
const context = vm.createContext({state, document: {querySelector(selector) {
  if (['#poolFollowUpMin', '#poolFollowUpMax', '#applyCustomerFilters', '#resetCustomerFilters'].includes(selector))
    return {addEventListener(type, handler) {handlers.set(`${selector}:${type}`, handler);}};
  return null;
}}, toast: message => messages.push(message), render: () => renders++, refreshCustomerSearchFromApi: async () => requests++});
vm.runInContext(source.slice(start, end), context);
const input = (bound, value) => handlers.get(`#poolFollowUp${bound}:input`)({target: {value}});
const query = () => handlers.get('#applyCustomerFilters:click')();
(async () => {
  input('Min', '1'); input('Max', '3'); await query();
  assert.equal(requests, 1); assert.equal(state.poolCustomerPage, 1);
  assert.equal(state.poolFollowUpMin, '1'); assert.equal(state.poolFollowUpMax, '3');
  input('Min', '4'); await query(); assert.equal(requests, 1);
  input('Min', '-1'); await query(); assert.equal(requests, 1);
  input('Min', '1.5'); await query(); assert.equal(requests, 1);
  input('Min', 'abc'); await query(); assert.equal(requests, 1);
  input('Min', ''); input('Max', '0'); await query(); assert.equal(requests, 2);
  const params = new URLSearchParams();
  vm.runInNewContext(source.slice(source.indexOf('  if (isPoolPage && state.poolFollowUpMin'), source.indexOf('  if (isPoolPage && state.poolDeepTalkDuration')), {state, params, isPoolPage: true});
  assert.equal(params.get('followUpMax'), '0'); assert.equal(params.has('followUpMin'), false);
  handlers.get('#resetCustomerFilters:click')();
  assert.equal(state.poolFollowUpMin, ''); assert.equal(state.poolFollowUpMax, '');
  assert.equal(requests, 3);
  assert.equal(messages.length, 4);
  console.log('PASS: pool follow-up input, validation, zero query parameter, pagination reset and filter reset');
})().catch(error => {console.error(error); process.exitCode = 1;});
