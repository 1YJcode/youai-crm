const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const test = require('node:test');
const assert = require('node:assert/strict');
const source = readFileSync(require('node:path').join(__dirname, '..', 'app.js'), 'utf8');
const helper = source.match(/^function backgroundViewIsBusy\([^]*?^\}/m)[0];
function busy({focused = false, modal = false, fields = []} = {}) {
  const active = {matches: () => focused};
  const root = {contains: () => focused, querySelector: () => modal ? {} : null, querySelectorAll: () => fields};
  const ctx = vm.createContext({document: {activeElement: active}});
  vm.runInContext(helper, ctx);
  return ctx.backgroundViewIsBusy(root);
}
test('background refresh protects a focused editor without an id', () => assert.equal(busy({focused: true}), true));
test('background refresh protects a draft after focus leaves it', () => assert.equal(busy({fields: [{tagName:'TEXTAREA', value:'draft', defaultValue:''}]}), true));
test('background refresh protects an open dialog', () => assert.equal(busy({modal:true}), true));
test('background refresh protects changed checkbox and select values', () => {
  assert.equal(busy({fields:[{tagName:'INPUT',type:'checkbox',checked:true,defaultChecked:false}]}),true);
  assert.equal(busy({fields:[{tagName:'SELECT',options:[{selected:true,defaultSelected:false}]}]}),true);
});
test('idle unchanged page allows background refresh', () => assert.equal(busy({fields:[{tagName:'INPUT',value:'same',defaultValue:'same'}]}),false));
