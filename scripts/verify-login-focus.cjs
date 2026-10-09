const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');
const source = readFileSync(join(__dirname, '..', 'app.js'), 'utf8');
let focused;
const element = (name, dataset = {}) => ({
  dataset, classList: { toggle() {}, remove() {} },
  handlers: {}, addEventListener(type, handler) { this.handlers[type] = handler; },
  focus() { focused = name; }
});
const identity = element('identity');
const password = element('password');
const captcha = element('captcha');
const form = element('form', { loginMode: 'account' });
form.querySelector = () => identity;
const accountTab = element('accountTab', { loginMode: 'account' });
const phoneTab = element('phoneTab', { loginMode: 'phone' });
const nodes = new Map([
  ['#app', element('app')], ['.topbar', element('topbar')],
  ['.help-rail', element('help')], ['#workspaceTabs', element('tabs')],
  ['#loginForm', form], ['#loginForm input', identity],
  ['#loginIdentityLabel', element('label')], ['#refreshCaptcha', element('refresh')]
]);
const context = vm.createContext({
  document: {
    body: element('body'), querySelector: selector => nodes.get(selector),
    querySelectorAll: selector => selector === '[data-login-mode]'
      ? [accountTab, phoneTab, form]
      : selector === '.login-tabs button[data-login-mode]' ? [accountTab, phoneTab] : []
  },
  createCaptcha: () => 'ABCD', escapeHtml: value => value,
  submitLogin() {}, refreshLoginCaptcha() {}
});
for (const name of ['showLogin', 'switchLoginMode']) {
  const start = source.indexOf(`function ${name}(`);
  const rest = source.slice(start);
  const end = rest.search(/\n(?:async )?function /);
  vm.runInContext(rest.slice(0, end), context);
}
context.showLogin();
for (const input of [identity, password, captcha]) {
  input.focus();
  // Input clicks bubble to the containing form.
  form.handlers.click?.();
  assert.equal(focused, input === identity ? 'identity' : input === password ? 'password' : 'captcha');
}
phoneTab.handlers.click();
assert.equal(form.dataset.loginMode, 'phone');
assert.equal(identity.name, 'phone');
assert.equal(focused, 'identity');
accountTab.handlers.click();
assert.equal(form.dataset.loginMode, 'account');
assert.equal(identity.name, 'username');
console.log('Login input focus and mode switching passed.');
