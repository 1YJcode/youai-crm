const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('http://youai.test/**', route => route.fulfill({ contentType: 'text/html', body: fs.readFileSync('index.html', 'utf8').replace(/<script[^]*?<\/script>/g, '') }));
    await page.goto('http://youai.test/');
    await page.addStyleTag({ path: 'styles.css' });
    await page.addScriptTag({ content: fs.readFileSync('app.js', 'utf8').replace(/initialize\(\);\s*$/, '') });
    await page.evaluate(() => {
      state.auth.user = { id: 1, displayName: '测试', roles: ['ADMIN'] };
      state.auth.token = '';
      state.view = 'customers'; state.customerSearch = ''; state.serviceScenario = '全部';
      customers = [
        { id: 'A', name: '甲客户', phone: '13800000001', owner: '甲销售', stage: '已流失', age: 30, followUpCount: 2, nextFollow: '', createdAt: '2026-10-01', tags: [] },
        { id: 'B', name: '乙客户', phone: '13800000002', owner: '乙销售', stage: '已流失', age: 45, followUpCount: 8, nextFollow: '', createdAt: '2026-11-01', tags: [] }
      ];
      document.body.classList.remove('app-booting');
    });
    for (const section of ['库存资源', '服务库', '过期VIP库']) {
      await page.evaluate(section => { state.customerSection = section; render(); }, section);
      await page.locator('[data-library-filter="name"]').fill('甲客户');
      await page.locator('#applyCustomerFilters').click();
      await page.waitForFunction(() => document.querySelectorAll('tr[data-customer-id]').length === 1);
      assert.equal(await page.locator('tr[data-customer-id]').getAttribute('data-customer-id'), 'A', section);
      await page.locator('#resetCustomerFilters').click();
      await page.waitForFunction(() => document.querySelectorAll('tr[data-customer-id]').length === 2);
      await page.locator('#openInventoryAdvanced, #openServiceAdvanced, #openExpiredVipAdvanced').click();
      await page.locator('.service-advanced-backdrop [data-library-filter="ageMin"]').fill('40');
      await page.locator('#queryServiceAdvanced').click();
      await page.waitForFunction(() => document.querySelectorAll('tr[data-customer-id]').length === 1);
      assert.equal(await page.locator('tr[data-customer-id]').getAttribute('data-customer-id'), 'B', section + ' advanced range');
      await page.locator('#resetCustomerFilters').click();
    }
    await page.evaluate(() => { state.customerSection = '诚意资源'; render(); });
    await page.locator('[data-library-filter="ageMin"]').fill('40');
    await page.locator('#applyCustomerFilters').click();
    assert.equal(await page.locator('[data-resource-customer]').count(), 1);
    assert.equal(await page.locator('[data-resource-customer]').getAttribute('data-resource-customer'), 'B');
    await page.locator('#resetCustomerFilters').click();
    assert.equal(await page.locator('[data-resource-customer]').count(), 2);
    const result = await page.evaluate(async () => {
      state.auth.token = 'fixture';
      const requests = [];
      apiRequest = async path => { requests.push(path); return { content: [{ id: String(requests.length), name: '匹配', owner: '销售', stage: '已流失' }], number: requests.length - 1, size: 100, totalElements: 2, totalPages: 2 }; };
      await applyLibraryFilters({ name: '匹配' });
      const count = currentLibraryFilters().rows.length;
      state.auth.token = '';
      const assertCacheCleared = currentLibraryFilters().rows === null;
      return { requests, count, cacheCleared: assertCacheCleared };
    });
    assert.equal(result.count, 2); assert.equal(result.requests.length, 2); assert.equal(result.cacheCleared, true);
    await page.evaluate(() => { state.view = 'calls'; state.callSection = '坐席管理'; calls = [{ agent: '甲销售', durationSeconds: 20 }]; tasks = [{ owner: '乙销售' }]; render(); });
    await page.locator('#seatAgentFilter').selectOption({ label: '乙销售' });
    await page.locator('#querySeatAgents').click();
    assert.equal(await page.locator('.seat-management-table tbody tr').count(), 1);
    assert.ok((await page.locator('.seat-management-table tbody').innerText()).includes('乙销售'));
    await page.locator('#seatStatusFilter').selectOption({ label: '在线' });
    await page.locator('#querySeatAgents').click();
    assert.ok((await page.locator('.seat-management-table tbody').innerText()).includes('暂无坐席数据'));
    await page.locator('#resetSeatAgents').click();
    assert.equal(await page.locator('.seat-management-table tbody tr').count(), 2);
    for (const [panel, table, columns] of [
      ['contract-filter-panel', 'order-contract-table', [null, 6, null]],
      ['payment-filter-panel', 'order-payment-table', [0, 1, null, 2, null, 9, 10, 8, null]],
      ['refund-filter-panel', 'order-refund-table', [null, null, null, null, null, 11, 13, null, null, null]],
      ['performance-filter-panel', 'order-performance-table', [2, 5, null, 9, 0]],
      ['system-user-filter', 'system-user-table', [3, 6, 7]]
    ]) {
      const passed = await page.evaluate(({panel, table, columns}) => {
        document.querySelector('#app').innerHTML = `<section class="${panel}">${columns.map((_, i) => `<label>条件${i}<input></label>`).join('')}<button>查询</button><button>重置</button></section><table class="${table}"><tbody>${['甲', '乙'].map(prefix => `<tr>${Array.from({length:16}, (_, i) => `<td>${prefix}${i}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
        bindReferenceTableFilters();
        const root = document.querySelector('.' + panel), rows = [...document.querySelectorAll('tbody tr')];
        return columns.every((column, i) => {
          const control = root.querySelectorAll('input')[i];
          if (column === null) return control.disabled;
          control.value = '甲' + column;
          root.querySelector('button').click();
          const matched = !rows[0].hidden && rows[1].hidden;
          root.querySelectorAll('button')[1].click();
          return matched && rows.every(row => !row.hidden) && control.value === '';
        });
      }, {panel, table, columns});
      assert.equal(passed, true, panel + ' each field and reset');
    }
    assert.deepEqual(errors, []);
    console.log('PASS: four libraries query/reset/range filters; advanced filters; all API pages; account cache isolation; seat filters; each supported field and reset on five reference tables; no browser errors');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
