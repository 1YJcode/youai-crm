const {chromium} = require('playwright');
const http = require('node:http');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const port = Number(process.argv[2]);
if (!Number.isInteger(port) || port < 1024) throw Error('isolated test backend port required');
const server = http.createServer((req,res) => {
  if (req.url.startsWith('/api/')) {
    const upstream = http.request({hostname:'127.0.0.1',port,path:req.url,method:req.method,headers:req.headers}, response => {
      res.writeHead(response.statusCode,response.headers); response.pipe(res);
    });
    upstream.on('error', () => {res.writeHead(502);res.end();}); req.pipe(upstream); return;
  }
  const pathname = req.url.split('?')[0];
  if (pathname === '/config.js') {res.setHeader('Content-Type','text/javascript');res.end("window.YOUAI_API_BASE='/api';");return;}
  const files = {'/knowledge.js':['knowledge.js','text/javascript'], '/recordings.js':['recordings.js','text/javascript'], '/app.js':['app.js','text/javascript'], '/styles.css':['styles.css','text/css'], '/logo-youai.png':['logo-youai.png','image/png']};
  if (files[pathname]) {const [file,type]=files[pathname];res.setHeader('Content-Type',type);res.end(fs.readFileSync(file));return;}
  res.setHeader('Content-Type','text/html; charset=utf-8');
  res.end(fs.readFileSync('index.html'));
});
(async () => {
  await new Promise(resolve => server.listen(0,'127.0.0.1',resolve));
  let browser;
  try {
    browser=await chromium.launch({channel:'msedge',headless:true});
    const page=await browser.newPage({viewport:{width:1200,height:900}});
    const errors=[]; page.on('pageerror',error=>errors.push(error.message));
    await page.route('https://cdn.sheetjs.com/**', route => route.fulfill({contentType:'text/javascript',body:''}));
    await page.goto(`http://127.0.0.1:${server.address().port}`);
    await page.locator('#loginForm input[name=username]').fill('admin');
    await page.locator('#loginForm input[name=password]').fill('Admin@123');
    await page.locator('#loginForm input[name=captcha]').fill((await page.locator('#refreshCaptcha').textContent()).trim());
    await page.locator('#loginSubmit').click();
    if(process.argv[3]==='knowledge') {
      await page.locator('[data-view=analytics]').first().click();
      await page.locator('[data-hover-subnav="知识库"]').click();
      await page.locator('[data-knowledge-new]').click();
      await page.locator('.knowledge-dialog input[name=title]').fill('知识库浏览器验证');
      await page.locator('.knowledge-dialog input[name=category]').fill('服务流程');
      await page.locator('.knowledge-dialog textarea').fill('请先确认客户需求。\n<script>window.knowledgeInjected=true</script>');
      await page.getByRole('button',{name:'保存知识',exact:true}).click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-list]').textContent.includes('知识库浏览器验证'));
      await page.getByLabel('搜索知识').fill('确认客户需求');
      await page.locator('.knowledge-search button').filter({hasText:'搜索'}).click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-status]').textContent==='共 1 篇知识');
      await page.locator('[data-knowledge-id]').first().click();
      assert.match(await page.locator('.knowledge-dialog textarea').inputValue(),/确认客户需求/);
      assert.equal(await page.evaluate(()=>window.knowledgeInjected),undefined);
      await page.locator('.knowledge-dialog textarea').fill('已更新：回访前先确认客户可联系时间。');
      await page.getByRole('button',{name:'保存知识',exact:true}).click();
      await page.locator('.knowledge-search button[type=reset]').click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-list]').textContent.includes('回访前'));
      await page.locator('[data-knowledge-id]').first().click();
      await page.screenshot({path:'.tmp_knowledge_browser.png',fullPage:true});
      await page.locator('[data-delete]').click();await page.locator('[data-delete]').click();
      await page.waitForFunction(()=>document.querySelector('[data-knowledge-status]').textContent.includes('还没有内容'));
      assert.deepEqual(errors,[]);
      console.log('PASS: full application knowledge navigation, creation, body search, literal script text, editing and confirmed deletion');
      return;
    }
    await page.locator('[data-view=calls]').first().click();
    await page.locator('[data-hover-subnav="通话记录"]').click();
    await page.locator('[data-call-recording]').first().click();
    await page.waitForFunction(()=>document.querySelector('[data-ai-status]').textContent.includes('请先上传'));
    const wav=Buffer.alloc(16044); wav.write('RIFF');wav.writeUInt32LE(16036,4);wav.write('WAVEfmt ',8);wav.writeUInt32LE(16,16);
    wav.writeUInt16LE(1,20);wav.writeUInt16LE(1,22);wav.writeUInt32LE(8000,24);wav.writeUInt32LE(16000,28);
    wav.writeUInt16LE(2,32);wav.writeUInt16LE(16,34);wav.write('data',36);wav.writeUInt32LE(16000,40);
    await page.locator('.recording-dialog input[type=file]').setInputFiles({name:'test.wav',mimeType:'audio/wav',buffer:wav});
    await page.locator('.recording-dialog form input[type=checkbox]').check();
    await page.getByRole('button',{name:'上传录音',exact:true}).click();
    await page.waitForFunction(()=>document.querySelector('audio').src.startsWith('blob:'));
    await page.evaluate(async()=>{const audio=document.querySelector('audio');await audio.play();audio.pause();});
    await page.locator('[data-ai-consent]').check();await page.locator('[data-analyze]').click();
    await page.waitForFunction(()=>!document.querySelector('[data-review]').hidden);
    assert.match(await page.locator('[data-transcript]').textContent(),/周五下午/);
    await page.locator('[data-review-text]').fill('已核对：周五下午回访，先短信确认时间。');
    await page.locator('[data-save-review]').click();
    await page.waitForFunction(()=>document.querySelector('[data-review-status]').textContent.includes('核对结果已保存'));
    await page.locator('[data-close]').click();await page.locator('[data-call-recording]').first().click();
    await page.waitForFunction(()=>document.querySelector('[data-review-text]').value.includes('先短信确认'));
    assert.match(await page.locator('[data-review-status]').textContent(),/admin/);
    assert.deepEqual(errors,[]);
    console.log('PASS: browser → JWT login → real upload/playback → async mock-AI analysis → real review persistence and reload');
  } finally {if(browser) await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
