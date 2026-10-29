/* Real Chromium interaction tests. npm install playwright@1.51.1; node tests/browser.cjs */
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve('app/src/main/assets/forest');
const output = path.resolve('build'); fs.mkdirSync(output, { recursive: true });
const server = http.createServer((req, res) => {
  const file = path.resolve(root, '.' + decodeURIComponent(new URL(req.url, 'http://localhost').pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  const mime = { '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css', '.webp': 'image/webp' };
  fs.readFile(file, (err, data) => { if (err) res.writeHead(404).end(); else { res.setHeader('Content-Type', mime[path.extname(file)] || 'application/octet-stream'); res.end(data); } });
});
(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}/index.html`;
  const browser = await chromium.launch({ headless: true });
  const errors = [];
  async function game(width, height, seed) {
    const context = await browser.newContext({ viewport: { width, height }, reducedMotion: 'reduce' });
    const page = await context.newPage(); page.on('pageerror', error => errors.push(String(error)));
    await page.route('**/*', route => { assert.ok(route.request().url().startsWith(`http://127.0.0.1:${server.address().port}/`), 'all resources are offline and bundled'); return route.continue(); });
    await page.goto(url); await page.waitForSelector('#game[data-ready=true]');
    assert.equal(await page.locator('#viewport').evaluate(e => e.clientWidth > 100 && e.clientHeight > 100), true, 'world is visibly laid out');
    if (seed) { await page.evaluate(value => localStorage.setItem('forest-chapter-1', JSON.stringify(value)), seed); await page.reload(); await page.waitForSelector('#game[data-ready=true]'); }
    return { page, context };
  }
  const { page, context } = await game(1280, 800);
  const click = async selector => page.locator(selector).click();
  const phase = async () => page.locator('#game').getAttribute('data-phase');
  await page.screenshot({ path: path.join(output, 'forest-intro.png') });
  await click('#begin'); await page.locator('#dialogue').waitFor({ state: 'visible' });
  await click('#dialogue-next'); assert.equal(await phase(), 'river');
  await click('[data-secret="0"]');
  await page.screenshot({ path: path.join(output, 'forest-path.png') });
  await click('#next-place'); await page.locator('#dialogue').waitFor({ state: 'visible' });
  await click('#dialogue-next'); await page.locator('#puzzle').waitFor({ state: 'visible' });
  await click('[data-choice=lamp]'); assert.equal(await phase(), 'river');
  assert.match(await page.locator('#feedback').textContent(), /noch einmal/);
  await click('#puzzle-repeat');
  const beforeHelp = await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-1')));
  assert.equal(beforeHelp.helped.river, false, 'Repeating does not reveal the target');
  await page.screenshot({ path: path.join(output, 'forest-river.png') });
  await click('#help'); assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-1')).helped.river), true);
  await click('#puzzle-back'); assert.equal(await phase(), 'river');
  await click('#next-place'); await page.locator('#dialogue').waitFor({ state: 'visible' }); await click('#dialogue-next');
  await click('[data-choice=lemonade]'); assert.equal(await phase(), 'treehouse');
  // A process restart right after a solved puzzle must not lose the crossing.
  await page.reload(); await page.waitForSelector('#game[data-ready=true]'); assert.equal(await phase(), 'treehouse');
  await click('#begin'); await click('[data-secret="1"]'); await click('#next-place');
  await page.locator('#house-puzzle').waitFor({ state: 'visible' });
  for (const letter of ['M', 'O', 'A']) await click(`[data-letter=${letter}]`);
  assert.equal(await phase(), 'treehouse'); assert.match(await page.locator('#feedback').textContent(), /noch nicht Oma/);
  await click('#help'); assert.match(await page.locator('#feedback').textContent(), /beginne mit O/);
  for (let i = 0; i < 3; i++) await click('#undo-letter');
  await page.screenshot({ path: path.join(output, 'forest-house.png') });
  // First letter is placed with a genuine pointer drag.
  const letter = await page.locator('[data-letter=O]').boundingBox(), slot = await page.locator('.letter-slot').first().boundingBox();
  await page.mouse.move(letter.x + letter.width / 2, letter.y + letter.height / 2); await page.mouse.down();
  await page.mouse.move(slot.x + slot.width / 2, slot.y + slot.height / 2, { steps: 10 }); await page.mouse.up();
  assert.equal(await page.locator('.letter-slot').first().textContent(), 'O');
  for (const letter of ['M', 'A']) await click(`[data-letter=${letter}]`);
  assert.equal(await phase(), 'complete'); await click('#success-next');
  await page.screenshot({ path: path.join(output, 'forest-ending.png') });
  await click('#stay'); await click('[data-secret="2"]');
  assert.match(await page.locator('#discovery-count').textContent(), /3\/3/);
  await page.reload(); await page.waitForSelector('#game[data-ready=true]'); await click('#begin');
  assert.equal(await phase(), 'complete'); assert.equal(await page.locator('#ending').isVisible(), true);
  const save = await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-1')));
  assert.deepEqual([...save.found].sort(), [0, 1, 2]);
  await context.close();
  for (const [width, height, label] of [[844, 390, 'phone'], [1024, 768, 'tablet'], [390, 844, 'portrait']]) {
    const { page: p, context: c } = await game(width, height, { version: 1, phase: 'treehouse', found: [], helped: {} });
    await p.locator('#begin').click(); await p.locator('#next-place').click(); await p.locator('#house-puzzle').waitFor({ state: 'visible' });
    const box = await p.locator('#puzzle').boundingBox(); assert.ok(box.x >= 0 && box.y >= 0 && box.x + box.width <= width + 1 && box.y + box.height <= height + 1, 'puzzle stays in the viewport');
    await p.screenshot({ path: path.join(output, `forest-${label}.png`) });
    for (const letter of ['O', 'M', 'A']) await p.locator(`[data-letter=${letter}]`).click();
    assert.equal(await p.locator('#game').getAttribute('data-phase'), 'complete'); await c.close();
  }
  assert.deepEqual(errors, [], 'no uncaught JavaScript errors');
  await browser.close(); await new Promise(resolve => server.close(resolve));
  console.log('PASS: complete chapter, wrong answers, help, drag, discoveries, reload and 3 screen sizes');
})().catch(error => { console.error(error); server.close(); process.exit(1); });
