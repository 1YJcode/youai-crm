// Run with: node --test scripts/test-customer-identity.cjs
// Exercise the production helpers without booting the DOM-heavy application.
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const assert = require('node:assert/strict');
const source = readFileSync(join(__dirname, '..', 'app.js'), 'utf8');

function helpers() {
  const context = vm.createContext({ state: {
    auth: { user: { id: 11, username: 'first', displayName: '同名员工' } },
    systemUsers: [
      { id: 11, username: 'first', displayName: '同名员工', roles: ['SALES'] },
      { id: 12, username: 'second', displayName: '同名员工', roles: ['SALES'] }
    ]
  }, escapeHtml: value => String(value ?? ''), businessModalFooter: () => '' });
  for (const name of ['customerOwnerAccountUsers', 'customerOwnerIsAdminUser', 'customerOwnerEmployeeOptions',
    'customerOwnerTree', 'customerUserReferenceLabel', 'collaborationOwnerField', 'collaborationOrdinal',
    'collaborationRow', 'businessModalFields', 'isCurrentCustomerOwner', 'currentOwner', 'customerPayload']) {
    const start = source.indexOf(`function ${name}(`);
    assert.notEqual(start, -1, `${name} must exist`);
    const remainder = source.slice(start);
    const next = remainder.search(/\n(?:async )?function /);
    vm.runInContext(next < 0 ? remainder : remainder.slice(0, next), context);
  }
  return context;
}

test('same-name employees remain separate choices and show their accounts', () => {
  const context = helpers();
  const options = context.customerOwnerEmployeeOptions();
  assert.equal(options.length, 2);
  assert.notEqual(options[0][0], options[1][0]);
  assert.match(options[0][1], /first/);
  assert.match(options[1][1], /second/);
  const employees = context.customerOwnerTree(false).children[0].children;
  assert.match(employees[0].label, /first/);
  assert.match(employees[1].label, /second/);
  assert.match(context.customerUserReferenceLabel('user:11'), /first/);
  assert.match(context.customerUserReferenceLabel('user:12'), /second/);
});

test('reopening multiple collaborators preserves separate editable rows and account labels', () => {
  const context = helpers();
  for (const record of [
    { collaborators: ['user:11', 'user:12'] },
    { collaborator: 'user:11、user:12' }
  ]) {
    const html = context.businessModalFields('collaboration', record);
    assert.equal((html.match(/data-collaboration-row/g) || []).length, 2);
    assert.deepEqual([...html.matchAll(/name="collaborators" value="([^"]*)"/g)].map(match => match[1]),
      ['user:11', 'user:12']);
    assert.match(html, /同名员工（first）/);
    assert.match(html, /同名员工（second）/);
  }
});

test('removing one collaborator preserves the other stable account reference', () => {
  const context = helpers();
  const html = context.businessModalFields('collaboration', { collaborators: ['user:11', 'user:12'] });
  const references = [...html.matchAll(/name="collaborators" value="([^"]*)"/g)].map(match => match[1]);
  references.splice(0, 1);
  const payload = context.customerPayload({ collaboratorIds: [11, 12] }, { collaborator: references.join('、') });
  assert.equal(payload.collaborator, 'user:12');
});

test('UI ownership survives rename and cannot be inferred from matching names', () => {
  const context = helpers();
  assert.equal(context.isCurrentCustomerOwner({ ownerId: 11, owner: '旧姓名' }), true);
  assert.equal(context.isCurrentCustomerOwner({ ownerId: 12, owner: '同名员工' }), false);
  assert.equal(context.isCurrentCustomerOwner({ owner: '同名员工' }), false);
});

test('saving a customer keeps stable owner and collaborator identities', () => {
  const payload = helpers().customerPayload({ ownerId: 12, owner: '旧姓名', collaboratorIds: [11], collaborator: '同名员工' });
  assert.equal(payload.owner, 'user:12');
  assert.equal(payload.collaborator, 'user:11');
});

test('clearing collaborators or moving to pool does not restore stale names', () => {
  const context = helpers();
  assert.equal(context.customerPayload({ collaboratorIds: [], collaborator: '过期缓存' }).collaborator, '');
  assert.equal(context.customerPayload({ ownerId: 12 }, { owner: '公海' }).owner, '公海');
});
