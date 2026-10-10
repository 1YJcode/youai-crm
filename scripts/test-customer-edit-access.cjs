const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const assert = require('node:assert/strict');
const source = readFileSync(join(__dirname, '..', 'app.js'), 'utf8');

function setup(roles = ['SALES']) {
  const requests = [];
  const context = vm.createContext({
    state: { auth: { user: { id: 2, roles } }, poolCustomers: [], dashboard: {} },
    customers: [], toast: () => {},
    document: { querySelector: () => { throw new Error('Unauthorized editor touched DOM'); } },
    FormData: class { constructor(form) { return Object.entries(form); } },
    currentOwner: () => '员工', requireBackend: () => {}, closeModal: () => {}, render: () => {},
    apiRequest: async (url, options) => { requests.push({ url, ...options }); return { id: '9', owner: '公海' }; },
    mergeCustomerRecord: record => record
  });
  for (const name of ['isAdmin', 'isCurrentCustomerOwner', 'findEditableCustomerRecord', 'canEditCustomer', 'openModal']) {
    const match = source.match(new RegExp(`^function ${name}\\([^]*?^\\}`, 'm'));
    assert.ok(match, name);
    vm.runInContext(match[0], context);
  }
  const submit = source.match(/document.querySelector\("#customerForm"\).addEventListener\("submit", (async event => \{[^]*?^\})\);/m);
  assert.ok(submit);
  context.submit = vm.runInContext(`(${submit[1]})`, context);
  return { context, requests };
}

test('edit permissions follow role and current owner ID through claim and return to pool', () => {
  const { context: c } = setup();
  const customer = { id: '9', owner: '公海', ownerId: null, contactVisible: true };
  c.state.poolCustomers.push(customer);
  assert.equal(c.canEditCustomer(customer), false);
  c.openModal(customer);
  customer.owner = '同名员工'; customer.ownerId = '2';
  assert.equal(c.canEditCustomer(customer), true);
  customer.ownerId = 3;
  assert.equal(c.canEditCustomer(customer), false);
  customer.owner = '公海'; customer.ownerId = 2;
  assert.equal(c.canEditCustomer(customer), false);
  c.state.auth.user.roles = ['ROLE_ADMIN'];
  assert.equal(c.canEditCustomer(customer), true);
  assert.equal(c.findEditableCustomerRecord(9), customer);
  assert.equal(c.canEditCustomer(undefined), false);
});

test('stale or missing edit records never issue a save or become creates', async () => {
  const { context: c, requests } = setup();
  c.state.poolCustomers.push({ id: '9', owner: '公海', ownerId: null });
  for (const customerId of ['9', 'missing']) {
    await c.submit({ preventDefault() {}, currentTarget: { customerId } });
  }
  assert.equal(requests.length, 0);
});

test('administrator editing a pool record sends PUT and preserves existing values', async () => {
  const { context: c, requests } = setup(['ADMIN']);
  c.state.poolCustomers.push({ id: '9', owner: '公海', source: '原来源', tags: ['原标签'], collaboratorIds: [] });
  await c.submit({ preventDefault() {}, currentTarget: { customerId: '9', phone: '13800138000', name: '客户' } });
  assert.equal(requests.length, 1);
  assert.equal(requests[0].method, 'PUT');
  assert.equal(requests[0].url, '/customers/9');
  assert.equal(JSON.parse(requests[0].body).owner, '公海');
  assert.equal(JSON.parse(requests[0].body).source, '原来源');
});
