// Run with: node --test scripts/test-customer-import-security.cjs
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const assert = require('node:assert/strict');
const source = readFileSync(join(__dirname, '..', 'app.js'), 'utf8');

function setup(admin = true) {
  const record = { id: 123, uploader: '赵娣', rows: [{ name: '私密客户', phone: '13800997654' }] };
  const stored = new Map([['youai.crm.importHistory', JSON.stringify([record])]]);
  const reads = [];
  const context = vm.createContext({
    state: { auth: { user: { id: admin ? 1 : 2, roles: [admin ? 'ADMIN' : 'SALES'] } },
      importHistory: [], workspaceTabs: [], customerSection: '客户列表' },
    localStorage: { getItem: key => { reads.push(key); return stored.get(key); },
      setItem: (key, value) => stored.set(key, value), removeItem: key => stored.delete(key) },
    toast: () => {}, loadCustomerTableColumns: () => {},
    customerListView: () => '客户列表', customers: [],
    viewMeta: { customers: {}, dashboard: {} }, location: { hash: '#customers' },
    setWorkspaceSection: (view, section) => { context.state.customerSection = section; },
    workspaceTabLabel: (view, section) => section,
    render: () => { throw new Error('Unauthorized import must not render'); },
    readCustomerImportRows: () => { throw new Error('Unauthorized file must not be read'); }
  });
  for (const name of ['isAdmin', 'resetCustomerImportState', 'loadCustomerImportHistory',
    'requireCustomerImportAccess', 'enforceCustomerImportAccess', 'importHistoryRecord',
    'persistImportHistory', 'importRowsForRecord', 'customerImportView', 'customerImportDetailView',
    'clearAuth', 'saveAuth', 'workspaceTabId', 'loadWorkspaceTabs', 'saveWorkspaceTabs',
    'restoreActiveWorkspaceTab', 'handleCustomerImportFile', 'importCustomersFromRows',
    'showNavHoverMenu']) {
    const start = source.search(new RegExp(`(?:async )?function ${name}\\(`));
    assert.notEqual(start, -1, `${name} must exist`);
    const rest = source.slice(start);
    const next = rest.search(/\n(?:async )?function /);
    vm.runInContext(next < 0 ? rest : rest.slice(0, next), context);
  }
  return { context, stored, reads, record };
}

test('employees never read persisted import data or render import details', () => {
  const { context, reads } = setup(false);
  context.loadCustomerImportHistory();
  assert.equal(reads.length, 0);
  assert.equal(context.importHistoryRecord(123), null);
  assert.equal(context.customerImportView(), '客户列表');
  assert.equal(context.customerImportDetailView({ rows: [{ phone: 'private' }] }), '客户列表');
});

test('switching from administrator clears private data and removes restored import tabs', () => {
  const { context, stored } = setup();
  context.loadCustomerImportHistory();
  context.state.importRows = context.state.importHistory[0].rows;
  context.state.importDetailId = 123;
  context.state.importActiveId = 123;
  context.state.customerSection = '客户导入';
  stored.set('youai.crm.workspaceTabs', JSON.stringify([
    { view: 'customers', section: '客户导入' },
    { view: 'customers', section: '客户导入', importDetailId: 123 },
    { view: 'customers', section: '客户列表' }
  ]));
  stored.set('youai.crm.activeWorkspaceTab', 'customers:import-detail:123');
  context.saveAuth({ accessToken: 'employee-token', user: { id: 2, roles: ['SALES'] } });
  context.restoreActiveWorkspaceTab();
  assert.equal(context.state.importHistory.length, 0);
  assert.equal(context.state.importRows.length, 0);
  assert.equal(context.state.importDetailId, null);
  assert.equal(context.state.importActiveId, null);
  assert.equal(context.state.customerSection, '客户列表');
  assert.equal(context.state.workspaceTabs.length, 1);
  assert.equal(JSON.parse(stored.get('youai.crm.workspaceTabs')).length, 1);
  assert.equal(JSON.parse(stored.get('youai.crm.importHistory')).length, 1);
});

test('logout clears memory and administrator login retains existing history', () => {
  const { context } = setup();
  context.loadCustomerImportHistory();
  context.clearAuth();
  assert.equal(context.state.importHistory.length, 0);
  context.saveAuth({ accessToken: 'admin-token', user: { id: 1, roles: ['ADMIN'] } });
  context.loadCustomerImportHistory();
  assert.equal(context.state.importHistory[0].uploader, '赵娣');
  assert.equal(context.importHistoryRecord(123).rows[0].phone, '13800997654');
  context.state.importHistory[0].rows[0].name = '修改后的客户';
  context.persistImportHistory();
  context.loadCustomerImportHistory();
  assert.equal(context.state.importHistory[0].rows[0].name, '修改后的客户');
});

test('employee upload, import and persistence are denied', async () => {
  const { context, stored } = setup(false);
  const before = stored.get('youai.crm.importHistory');
  await context.handleCustomerImportFile({ target: { files: [{ size: 10 }] } });
  await context.importCustomersFromRows();
  context.state.importHistory = [{ id: 999 }];
  context.persistImportHistory();
  assert.equal(stored.get('youai.crm.importHistory'), before);
});

test('file parsing finishing after account switch cannot restore private import data', async () => {
  const { context } = setup();
  context.loadCustomerImportHistory();
  let finish;
  context.readCustomerImportRows = () => new Promise(resolve => { finish = resolve; });
  const upload = context.handleCustomerImportFile({ target: { files: [{ size: 10, name: 'private.csv' }] } });
  context.saveAuth({ accessToken: 'employee-token', user: { id: 2, roles: ['SALES'] } });
  finish([{ name: 'private', phone: '13800997654' }]);
  await upload;
  assert.equal(context.state.importHistory.length, 0);
  assert.equal(context.state.importRows.length, 0);
});

test('navigation shows customer import only to administrators', () => {
  const { context } = setup();
  context.navHoverItems = { customers: ['客户列表', '客户导入'] };
  context.navHoverMenu = { dataset: {}, style: {}, classList: { add: () => {}, remove: () => {} } };
  context.currentWorkspaceSection = () => '客户列表';
  context.escapeHtml = value => value;
  const item = { dataset: { view: 'customers' }, getBoundingClientRect: () => ({ left: 10, bottom: 10 }) };
  context.showNavHoverMenu(item);
  assert.match(context.navHoverMenu.innerHTML, /客户导入/);
  context.state.auth.user.roles = ['SALES'];
  context.showNavHoverMenu(item);
  assert.doesNotMatch(context.navHoverMenu.innerHTML, /客户导入/);
});

test('import response arriving after switch cannot merge private customers or continue batch', async () => {
  const { context } = setup();
  context.loadCustomerImportHistory();
  context.state.importActiveId = 123;
  context.state.importHistory[0].rows.push({ name: 'second', phone: '13800997655' });
  context.state.poolCustomers = [];
  context.document = { querySelector: () => null };
  context.requireBackend = () => {};
  context.importRowValidation = row => ({ errors: [], phone: row.phone });
  context.importPayloadFromRow = row => row;
  context.mergeCustomerRecord = () => { throw new Error('Must not merge administrator data after switch'); };
  let finish;
  let requests = 0;
  context.apiRequest = () => { requests++; return new Promise(resolve => { finish = resolve; }); };
  const batch = context.importCustomersFromRows();
  assert.equal(requests, 1);
  context.saveAuth({ accessToken: 'employee-token', user: { id: 2, roles: ['SALES'] } });
  finish({ id: 99, name: 'private' });
  await batch;
  assert.equal(requests, 1);
  assert.equal(context.state.importRows.length, 0);
  assert.equal(context.state.importHistory.length, 0);
});

test('administrator upload and batch import still persist completed history', async () => {
  const { context, stored } = setup();
  context.loadCustomerImportHistory();
  context.render = () => {};
  context.currentOwner = () => '赵娣';
  context.readCustomerImportRows = async () => [{ name: '上传客户', phone: '13800997656', _importStatus: '未导入' }];
  await context.handleCustomerImportFile({ target: { files: [{ size: 10, name: 'customers.csv' }] } });
  const record = context.state.importHistory[0];
  assert.equal(record.fileName, 'customers.csv');
  assert.equal(record.uploader, '赵娣');
  context.state.poolCustomers = [];
  context.document = { querySelector: () => null };
  context.requireBackend = () => {};
  context.importRowValidation = row => ({ errors: [], phone: row.phone });
  context.importPayloadFromRow = row => row;
  context.apiRequest = async () => ({ id: 99, name: '上传客户' });
  context.mergeCustomerRecord = customer => { context.customers.push(customer); return customer; };
  context.recordCustomerActivity = () => {};
  await context.importCustomersFromRows();
  assert.equal(context.customers.length, 1);
  assert.equal(record.status, '已完成');
  assert.equal(record.success, 1);
  assert.equal(JSON.parse(stored.get('youai.crm.importHistory'))[0].rows[0]._importStatus, '已导入');
});

test('token refresh for the same administrator keeps active import state', () => {
  const { context } = setup();
  context.loadCustomerImportHistory();
  context.state.importDetailId = 123;
  const version = context.state.importSessionVersion;
  context.saveAuth({ accessToken: 'refreshed', user: { id: 1, roles: ['ADMIN'] } });
  assert.equal(context.state.importSessionVersion, version);
  assert.equal(context.state.importDetailId, 123);
  assert.equal(context.state.importHistory.length, 1);
});
