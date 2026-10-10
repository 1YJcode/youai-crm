// Run with: node --test scripts/test-customer-scene-sync.cjs
// Exercise production query/sync functions with in-memory API responses only.
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const assert = require('node:assert/strict');
const source = readFileSync(join(__dirname, '..', 'app.js'), 'utf8');
const fixedNow = Date.parse('2026-10-10T12:00:00Z');

class FixedDate extends Date {
  constructor(...args) { super(...(args.length ? args : [fixedNow])); }
  static now() { return fixedNow; }
}

function extract(name) {
  const match = source.match(new RegExp(`^(?:async )?function ${name}\\([^]*?^\\}`, 'm'));
  assert.ok(match, `${name} must exist`);
  return match[0];
}

function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
}

function page(content = [], number = 0, size = 20, totalElements = content.length) {
  return { content, number, size, totalElements, totalPages: Math.max(1, Math.ceil(totalElements / size)) };
}

function setup(overrides = {}, respond = () => page()) {
  const requests = [];
  const errors = [];
  const warnings = [];
  const state = {
    auth: { token: 'fixture-token', user: { id: 2, roles: ['SALES'] } },
    view: 'customers', customerSection: '客户列表', customerDetailId: null, importDetailId: null,
    customerScene: 'new-unfollowed', customerScope: 'mine',
    customerPage: 1, poolCustomerPage: 1, whiteboardCustomerPage: 1, customerPageSize: 20,
    customerSearchPending: false,
    customerPageMeta: { page: 0, size: 20, totalElements: 0, totalPages: 1 },
    customerSearch: '', customerNameSearch: '', whiteboardNameSearch: '',
    customerStage: '全部阶段', customerLevel: '全部等级', whiteboardStatus: '全部',
    customerTagFilter: '', whiteboardSelectedTags: [],
    customerOwnerSelection: {}, customerAdvancedCollaboratorSelection: {},
    customerAdvancedLevels: [], customerAdvancedDateRanges: {},
    customerGender: 'all', customerMaritalStatus: 'all', customerAvatarFilter: 'all',
    quickFilter: '全部客户',
    whiteboardCustomers: [], poolCustomers: [], systemNotifications: [], notificationRead: {},
    ...overrides
  };
  const feedback = { attributes: {}, status: null };
  const scenes = ['all', 'new-unfollowed'].map(key => {
    const classes = new Set(key === 'all' ? ['active'] : []);
    return { dataset: { customerScene: key }, classes,
      classList: { toggle: (name, enabled) => enabled ? classes.add(name) : classes.delete(name) } };
  });
  const panel = {
    querySelector: selector => selector === '.customer-query-status' ? feedback.status : null,
    prepend: status => { feedback.status = status; }
  };
  const visiblePage = {
    setAttribute: (name, value) => { feedback.attributes[name] = value; },
    querySelectorAll: selector => selector === '[data-customer-scene]' ? scenes : [],
    querySelector: selector => selector === '.data-panel' ? panel : null
  };
  const context = vm.createContext({
    state, Date: FixedDate, URLSearchParams, customers: [], tasks: [], calls: [],
    customerSyncPromise: null, customerSearchRequestId: 0,
    apiRequest: async path => {
      requests.push(path);
      return path === '/system-notifications' ? [] : respond(path);
    },
    document: {
      querySelector: selector => {
        const visible = state.view === 'customers' && !state.customerDetailId;
        if (selector === '.pool-page') return visible && state.customerSection === '公海列表' ? visiblePage : null;
        if (selector === '.whiteboard-page') return visible && state.customerSection === '白板列表' ? visiblePage : null;
        if (selector === '.customer-list-page') return visible && ['客户列表', '公海列表'].includes(state.customerSection) ? visiblePage : null;
        if (selector === '.customer-list-page, .pool-page, .whiteboard-page') {
          return visible && ['客户列表', '公海列表', '白板列表'].includes(state.customerSection) ? visiblePage : null;
        }
        return null;
      },
      createElement: () => ({ attributes: {}, setAttribute(name, value) { this.attributes[name] = value; } })
    },
    normalizeCustomer: customer => ({ ...customer }),
    customerOwnerSelectionNames: () => [], selectedCustomerStatuses: () => [],
    selectedCustomerEducations: () => [], selectedCustomerUncontactedDaysThreshold: () => null,
    isAdmin: () => false,
    console: { warn: (...args) => warnings.push(args) },
    localStorage: { setItem() {} }, toast: message => errors.push(message),
    render: () => { context.renderCount += 1; }, renderCount: 0
  });
  const names = ['localDateValue', 'pageContent', 'parseCustomerDate', 'daysSince',
    'customerFollowUpCount', 'customerMatchesScene', 'updateCustomerSearchFeedback', 'refreshCustomerSearchFromApi',
    'refreshVisibleCustomerList', 'refreshAssignedCustomers', 'mergeCustomerRecord', 'refreshCustomerFollowUpCounts'];
  // Include the production visibility predicate when the implementation shares one.
  for (const name of ['customerAssignedListVisible']) {
    if (source.includes(`function ${name}(`)) names.push(name);
  }
  names.forEach(name => vm.runInContext(extract(name), context));
  return { context, state, requests, errors, warnings, feedback, scenes };
}

test('canonical follow-up count excludes contacted customers even when local pages are empty', () => {
  const { context } = setup();
  const customer = { id: 'already-contacted', name: '客户', duplicateRegistration: true,
    followUpCount: 3, lastContactAt: '2026-10-10T10:00:00' };
  assert.equal(context.customerFollowUpCount(customer), 3);
  assert.equal(context.customerMatchesScene(customer, 'new-unfollowed'), false);
  assert.equal(context.customerMatchesScene(customer, 'duplicate-unfollowed'), false);
});

test('canonical zero remains unfollowed despite a same-name task or cached call', () => {
  const { context } = setup();
  context.tasks = [{ customer: '同名客户', customerId: 'different-customer' }];
  context.calls = [{ customerId: 'unfollowed' }];
  const customer = { id: 'unfollowed', name: '同名客户', lastContactAt: '2026-10-10T10:00:00',
    duplicateRegistration: true, followUpCount: 0 };
  assert.equal(context.customerFollowUpCount(customer), 0);
  assert.equal(context.customerMatchesScene(customer, 'new-unfollowed'), true);
  assert.equal(context.customerMatchesScene(customer, 'duplicate-unfollowed'), true);
});

test('fallback follow-up count identifies customers by stable IDs rather than names', () => {
  const { context } = setup();
  context.tasks = [{ customer: '同名客户', customerId: 'different-customer' },
    { customer: '已改名', customerId: 'same-customer' }];
  context.calls = [{ customerId: 'same-customer' }];
  assert.equal(context.customerFollowUpCount({ id: 'same-customer', name: '同名客户' }), 2);
  assert.equal(context.customerFollowUpCount({ id: 'unrelated', name: '同名客户' }), 0);
});

test('visible customer polling preserves scene, scope, filters and pagination', async () => {
  const row = { id: 'filtered-only', owner: '林夕', updatedAt: '2026-10-10T10:00:00' };
  const { context, state, requests, errors } = setup({ customerPage: 3, customerPageSize: 50,
    customerNameSearch: '客户昵称', customerTagFilter: '重点客户' }, () => page([row], 2, 50, 151));
  await context.refreshAssignedCustomers();
  const customerRequests = requests.filter(path => path.startsWith('/customers?'));
  assert.equal(customerRequests.length, 1);
  const params = new URL(customerRequests[0], 'http://fixture').searchParams;
  assert.equal(params.get('scene'), 'new-unfollowed');
  assert.equal(params.get('scope'), 'mine');
  assert.equal(params.get('nameKeyword'), '客户昵称');
  assert.equal(params.get('tag'), '重点客户');
  assert.equal(params.get('page'), '2');
  assert.equal(params.get('size'), '50');
  assert.equal(state.customerPage, 3);
  assert.equal(state.customerPageMeta.totalElements, 151);
  assert.equal(state.customerPageMeta.size, 50);
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['filtered-only']);
  assert.deepEqual(errors, []);
});

test('polling an empty unfollowed scene does not refill it with the unfiltered list', async () => {
  const { context, state, requests, errors } = setup({}, path => {
    const params = new URL(path, 'http://fixture').searchParams;
    return params.get('scene') === 'new-unfollowed' ? page() : page([{ id: 'contacted', owner: '林夕' }], 0, 100);
  });
  await context.refreshAssignedCustomers();
  assert.equal(context.customers.length, 0);
  assert.equal(state.customerPageMeta.totalElements, 0);
  assert.equal(state.customerPageMeta.size, 20);
  assert.ok(requests.some(path => path.includes('scene=new-unfollowed')));
  assert.deepEqual(errors, []);
});

test('uncontested background polling still updates cached customers', async () => {
  const { context, state, requests, errors } = setup({ view: 'dashboard' }, () =>
    page([{ id: 'new-assignment', owner: '林夕', updatedAt: '2026-10-10T10:00:00' }], 0, 100));
  await context.refreshAssignedCustomers();
  assert.ok(requests.includes('/customers?page=0&size=100'));
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['new-assignment']);
  assert.equal(state.customerPageMeta.size, 100);
  assert.deepEqual(errors, []);
});

test('a pending background unfiltered response cannot replace a list opened meanwhile', async () => {
  const pending = deferred();
  const { context, state, errors } = setup({ view: 'dashboard' }, () => pending.promise);
  const sync = context.refreshAssignedCustomers();
  state.view = 'customers';
  context.customers = [{ id: 'current-filtered-row', owner: '林夕' }];
  const currentMeta = { page: 2, size: 50, totalElements: 120, totalPages: 3 };
  state.customerPageMeta = currentMeta;
  pending.resolve(page([{ id: 'old-unfiltered-row', owner: '林夕' }], 0, 100));
  await sync;
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['current-filtered-row']);
  assert.equal(state.customerPageMeta, currentMeta);
  assert.deepEqual(errors, []);
});

test('an old background response cannot overwrite a newer search after leaving the list', async () => {
  const pending = deferred();
  let customerRequestCount = 0;
  const { context, state, errors } = setup({ view: 'dashboard' }, () => {
    customerRequestCount += 1;
    return customerRequestCount === 1 ? pending.promise : page([{ id: 'new-search-row', owner: '林夕' }]);
  });
  const sync = context.refreshAssignedCustomers();
  state.view = 'customers';
  await context.refreshCustomerSearchFromApi();
  const currentMeta = state.customerPageMeta;
  state.view = 'dashboard';
  pending.resolve(page([{ id: 'old-unfiltered-row', owner: '林夕' }], 0, 100));
  await sync;
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['new-search-row']);
  assert.equal(state.customerPageMeta, currentMeta);
  assert.deepEqual(errors, []);
});

test('a newer customer query wins over a pending visible-list poll', async () => {
  const pending = deferred();
  let customerRequestCount = 0;
  const { context, state, errors } = setup({}, () => {
    customerRequestCount += 1;
    return customerRequestCount === 1 ? pending.promise : page([{ id: 'new-scene-row', owner: '林夕' }]);
  });
  const sync = context.refreshAssignedCustomers();
  state.customerScene = 'all';
  await context.refreshCustomerSearchFromApi();
  const currentMeta = state.customerPageMeta;
  pending.resolve(page([{ id: 'outdated-scene-row', owner: '林夕' }]));
  await sync;
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['new-scene-row']);
  assert.equal(state.customerPageMeta, currentMeta);
  assert.deepEqual(errors, []);
});

test('a background response from an old account cannot restore its customer snapshot', async () => {
  const pending = deferred();
  const { context, state, errors } = setup({ view: 'dashboard' }, () => pending.promise);
  const sync = context.refreshAssignedCustomers();
  state.auth.token = 'new-account-token';
  context.customers = [{ id: 'new-account-row', owner: '其他销售' }];
  const currentMeta = { page: 0, size: 20, totalElements: 1, totalPages: 1 };
  state.customerPageMeta = currentMeta;
  pending.resolve(page([{ id: 'old-account-row', owner: '林夕' }], 0, 100));
  await sync;
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['new-account-row']);
  assert.equal(state.customerPageMeta, currentMeta);
  assert.deepEqual(errors, []);
});

test('foreground queries select the scene and show loading immediately until the response arrives', async () => {
  const pending = deferred();
  const { context, state, feedback, scenes, errors } = setup({}, () => pending.promise);
  const search = context.refreshCustomerSearchFromApi();
  assert.equal(state.customerSearchPending, true);
  assert.equal(feedback.attributes['aria-busy'], 'true');
  assert.equal(scenes.find(button => button.dataset.customerScene === 'new-unfollowed').classes.has('active'), true);
  assert.equal(scenes.find(button => button.dataset.customerScene === 'all').classes.has('active'), false);
  assert.equal(feedback.status.textContent, '正在加载客户…');
  assert.equal(feedback.status.hidden, false);
  pending.resolve(page([{ id: 'loaded-row', owner: '林夕' }]));
  await search;
  assert.equal(state.customerSearchPending, false);
  assert.equal(feedback.attributes['aria-busy'], 'false');
  assert.equal(feedback.status.hidden, true);
  assert.deepEqual(errors, []);
});

test('silent polling does not show foreground loading feedback', async () => {
  const pending = deferred();
  const { context, state, feedback, errors } = setup({}, () => pending.promise);
  const search = context.refreshCustomerSearchFromApi({ silent: true });
  assert.equal(state.customerSearchPending, false);
  assert.equal(feedback.status, null);
  assert.notEqual(feedback.attributes['aria-busy'], 'true');
  pending.resolve(page());
  await search;
  assert.equal(state.customerSearchPending, false);
  assert.equal(feedback.status, null);
  assert.deepEqual(errors, []);
});

test('polling skips the list request while a foreground query is pending', async () => {
  const pending = deferred();
  const { context, state, requests, feedback, errors } = setup({}, () => pending.promise);
  const search = context.refreshCustomerSearchFromApi();
  const requestId = context.customerSearchRequestId;
  await context.refreshAssignedCustomers();
  assert.equal(requests.filter(path => path.startsWith('/customers?')).length, 1);
  assert.equal(context.customerSearchRequestId, requestId);
  assert.equal(state.customerSearchPending, true);
  assert.equal(feedback.attributes['aria-busy'], 'true');
  pending.resolve(page([{ id: 'foreground-row', owner: '林夕' }]));
  await search;
  assert.deepEqual(Array.from(context.customers, customer => customer.id), ['foreground-row']);
  assert.equal(state.customerSearchPending, false);
  assert.deepEqual(errors, []);
});

test('refreshing follow-up counts replaces stale canonical counts after a successful mutation', async () => {
  let unavailable = false;
  const { context, state, requests, errors, warnings } = setup({}, () => {
    if (unavailable) throw new Error('temporary API failure');
    return { id: 'changed-customer', name: '客户', owner: '林夕', followUpCount: 0 };
  });
  context.customers = [{ id: 'changed-customer', name: '客户', owner: '林夕', followUpCount: 3 },
    { id: 'other-customer', owner: '林夕', followUpCount: 4 }];
  const currentMeta = state.customerPageMeta;
  assert.equal(context.customerMatchesScene(context.customers[0], 'new-unfollowed'), false);
  await context.refreshCustomerFollowUpCounts(['changed-customer']);
  assert.deepEqual(requests, ['/customers/changed-customer']);
  assert.equal(context.customerFollowUpCount(context.customers[0]), 0);
  assert.equal(context.customerMatchesScene(context.customers[0], 'new-unfollowed'), true);
  assert.equal(context.customers[1].followUpCount, 4);
  assert.equal(state.customerPageMeta, currentMeta);
  assert.deepEqual(errors, []);
  assert.deepEqual(warnings, []);
  unavailable = true;
  await assert.doesNotReject(context.refreshCustomerFollowUpCounts(['changed-customer']));
  assert.equal(context.customers[0].followUpCount, 0);
  assert.equal(warnings.length, 1);
  assert.deepEqual(errors, [], 'a refresh failure must not turn the successful mutation into a UI error');
});

test('refreshing several customers deduplicates IDs and merges the returned counts into caches', async () => {
  const { context, state, requests, warnings } = setup({}, path => path === '/customers/42'
    ? { id: '42', owner: '公海', followUpCount: 4 }
    : { id: 'assigned', owner: '林夕', followUpCount: 2 });
  context.customers = [{ id: 'assigned', owner: '林夕', followUpCount: 0 },
    { id: '42', owner: '公海', followUpCount: 1 }];
  state.poolCustomers = [{ id: '42', owner: '公海', followUpCount: 1 }];
  await context.refreshCustomerFollowUpCounts(['assigned', 'assigned', 42, '42', null, undefined, '']);
  assert.deepEqual(requests.sort(), ['/customers/42', '/customers/assigned']);
  assert.equal(context.customers.length, 2);
  assert.equal(context.customers.find(customer => customer.id === 'assigned').followUpCount, 2);
  assert.equal(state.poolCustomers.length, 1);
  assert.equal(state.poolCustomers[0].followUpCount, 4);
  assert.deepEqual(warnings, []);
});

test('follow-up refresh responses from a previous account cannot change the current caches', async () => {
  const pending = deferred();
  const { context, state, warnings } = setup({}, () => pending.promise);
  const refresh = context.refreshCustomerFollowUpCounts(['shared-id']);
  state.auth.token = 'new-account-token';
  context.customers = [{ id: 'shared-id', owner: '当前员工', followUpCount: 0 }];
  state.poolCustomers = [{ id: 'pool-id', owner: '公海', followUpCount: 1 }];
  pending.resolve({ id: 'shared-id', owner: '旧员工', followUpCount: 9 });
  await refresh;
  assert.equal(context.customers.length, 1);
  assert.equal(context.customers[0].owner, '当前员工');
  assert.equal(context.customers[0].followUpCount, 0);
  assert.equal(state.poolCustomers.length, 1);
  assert.equal(state.poolCustomers[0].id, 'pool-id');
  assert.deepEqual(warnings, []);
});
