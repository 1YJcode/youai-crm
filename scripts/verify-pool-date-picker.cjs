const assert = require('node:assert/strict');
const vm = require('node:vm');

async function main() {
  const base = process.argv[2] || 'http://localhost:4173';
  const html = await (await fetch(base)).text();
  const script = html.match(/src="(app\.js[^\"]*)"/)[1];
  const source = await (await fetch(new URL(script, base))).text();
  const handlers = new Map();
  const state = {
    customerDateRangePickerOpen: false, customerStartDate: '', customerEndDate: '',
    customerPage: 3, poolCustomerPage: 3
  };
  const element = (key, dataset = {}) => ({
    dataset, addEventListener(type, handler) { handlers.set(key, handler); }
  });
  const context = vm.createContext({
    state, document: {
      querySelector(selector) {
        if (selector === '.customer-list-page .date-range-display') return element('open');
        if (selector === '[data-clear-customer-date-range]') return element('clear');
        return null;
      },
      querySelectorAll(selector) {
        return selector === '[data-customer-date]'
          ? [element('start', {customerDate: '2026-10-01'}), element('end', {customerDate: '2026-10-08'})]
          : [];
      }
    },
    customerDateRangeMonthValue: date => date.slice(0, 7),
    localDateValue: () => '2026-10-08', render() {}, toast(message) { throw Error(message); }
  });
  const start = source.indexOf('  const dateRangeDisplay =');
  const end = source.indexOf('  document.querySelectorAll("[data-customer-scene]")', start);
  assert(start >= 0 && end > start);
  vm.runInContext(source.slice(start, end), context);
  const event = {preventDefault() {}, stopPropagation() {}};
  assert(handlers.has('open'), 'Pool date display must have a click handler');
  handlers.get('open')(event);
  assert.equal(state.customerDateRangePickerOpen, true);
  handlers.get('start')(event);
  handlers.get('end')(event);
  assert.equal(state.customerStartDate, '2026-10-01');
  assert.equal(state.customerEndDate, '2026-10-08');
  assert.equal(state.poolCustomerPage, 1);
  assert.equal(state.customerDateRangePickerOpen, false);

  const labelStart = source.indexOf('<label class="field customer-date-range">');
  const labelEnd = source.indexOf('</label>', labelStart) + '</label>'.length;
  const markupContext = vm.createContext({state, isPool: true, icon: () => '', customerDateRangePickerView: () => '<div id="dateRangePopover"></div>'});
  state.customerDateRangePickerOpen = true;
  const markup = vm.runInContext('`' + source.slice(labelStart, labelEnd) + '`', markupContext);
  assert(markup.includes('2026-10-01') && markup.includes('2026-10-08'));
  assert(markup.includes('id="dateRangePopover"'), 'Pool must render the date popup');
  handlers.get('clear')(event);
  assert.equal(state.customerStartDate, '');
  assert.equal(state.customerEndDate, '');
  assert(source.includes('poolEntryStart: isPoolPage ? state.customerStartDate : ""'));
  console.log('PASS: served pool date click, popup, selection, page reset, clear and query mapping');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
