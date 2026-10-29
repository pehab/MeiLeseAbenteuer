/* Real Chromium interaction tests. npm install playwright@1.51.1; node tests/browser.cjs */
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { letters } = require('../app/src/main/assets/forest/letters.js');
const root = path.resolve('app/src/main/assets/forest');
const output = path.resolve('build'); fs.mkdirSync(output, { recursive: true });
const server = http.createServer((req, res) => {
  const file = path.resolve(root, '.' + decodeURIComponent(new URL(req.url, 'http://localhost').pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  const mime = { '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css', '.webp': 'image/webp', '.ogg': 'audio/ogg' };
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
    if (seed) { await page.evaluate(value => { localStorage.setItem('forest-chapter-2', JSON.stringify(value)); localStorage.setItem('forest-letters-1', JSON.stringify({ version: 1, completed: 8, index: 7, step: 'reward' })); }, seed); await page.reload(); await page.waitForSelector('#game[data-ready=true]'); }
    return { page, context };
  }
  const { page, context } = await game(1280, 800);
  const click = async selector => page.locator(selector).click();
  const phase = async () => page.locator('#game').getAttribute('data-phase');
  await page.evaluate(() => localStorage.setItem('forest-chapter-1', JSON.stringify({ version: 1, phase: 'complete', found: [0, 1, 2] })));
  await page.reload(); await page.waitForSelector('#game[data-ready=true]');
  assert.equal(await phase(), 'meeting', 'a finished 0.4 prototype does not skip the new story');
  await page.screenshot({ path: path.join(output, 'forest-intro.png') });
  await click('#begin');
  await page.locator('#lesson').waitFor({ state: 'visible' });
  assert.equal(await phase(), 'meeting', 'the word story waits for the letters');
  await click('#lesson-path');
  assert.equal(await page.locator('[data-chapter="1"]').isDisabled(), true);
  await click('[data-chapter="0"]');
  async function traceForm(p, strokes, wobbly = false) {
    for (const points of strokes) {
      const screen = await p.locator('#trace-board').evaluate((e, pts) => pts.map(([x, y]) => { const point = e.createSVGPoint(); point.x = x; point.y = y; const q = point.matrixTransform(e.getScreenCTM()); return [q.x, q.y]; }), wobbly ? points.map(([x, y]) => [x + 18, y + 17]) : points);
      await p.mouse.move(...screen[0]); await p.mouse.down();
      for (let i = 1; i < screen.length; i++) await p.mouse.move(...screen[i], { steps: Math.max(1, Math.ceil(Math.hypot(screen[i][0] - screen[i-1][0], screen[i][1] - screen[i-1][1]) / 12)) });
      await p.mouse.up();
    }
    assert.equal(await p.locator('#lesson-next').isDisabled(), false, 'a fully traced letter enables continuing');
  }
  for (let i = 0; i < letters.length; i++) {
    const letter = letters[i];
    assert.equal(await page.locator('#lesson').getAttribute('data-index'), String(i));
    await click('#letter-sound');
    await click('#lesson-next');
    const wrong = await page.locator(`#letter-options button:not([data-answer="${letter.upper}"])`).first();
    await wrong.click(); assert.equal(await page.locator('#lesson').getAttribute('data-step'), 'find');
    await page.locator(`#letter-options [data-answer="${letter.upper}"]`).click();
    await page.locator(`#letter-options [data-answer="${letter.lower}"]`).click();
    assert.equal(await page.locator('#lesson-next').isDisabled(), true);
    if (i === 0) {
      await page.screenshot({ path: path.join(output, 'letters-writing.png') });
      assert.equal(await page.locator('.trace-goal-star').textContent(), '★');
      await click('#trace-demo'); await page.waitForTimeout(400);
      assert.equal(await page.locator('#lesson-next').isDisabled(), true, 'watching the demonstration does not solve the exercise');
      await page.reload(); await page.waitForSelector('#game[data-ready=true]'); await click('#begin');
      assert.equal(await page.locator('#lesson').getAttribute('data-step'), 'writeUpper', 'resume the exercise after a restart');
    }
    await traceForm(page, letter.upperStrokes, true); await click('#lesson-next');
    await traceForm(page, letter.lowerStrokes, true); await click('#lesson-next');
    assert.equal(await page.locator('#lesson').getAttribute('data-step'), 'reward');
    assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('forest-letters-1')).completed), i + 1);
    await click('#lesson-next');
  }
  await page.locator('#dialogue').waitFor({ state: 'visible' });
  await click('#dialogue-next'); assert.equal(await phase(), 'river');
  await click('[data-secret="0"]');
  await page.screenshot({ path: path.join(output, 'forest-path.png') });
  await click('#next-place'); await page.locator('#dialogue').waitFor({ state: 'visible' });
  await click('#dialogue-next'); await page.locator('#puzzle').waitFor({ state: 'visible' });
  await click('[data-choice=lamp]'); assert.equal(await phase(), 'river');
  assert.match(await page.locator('#feedback').textContent(), /noch einmal/);
  await click('#puzzle-repeat');
  const beforeHelp = await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-2')));
  assert.equal(beforeHelp.helped.river, false, 'Repeating does not reveal the target');
  await page.screenshot({ path: path.join(output, 'forest-river.png') });
  await click('#help'); assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-2')).helped.river), true);
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
  const save = await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-2')));
  assert.deepEqual([...save.found].sort(), [0, 1, 2]);
  assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('forest-chapter-1')).phase), 'complete', 'the original prototype data stays unchanged');
  // Continue into the new chapter with the real story and letter gates already completed.
  await click('#start-lanterns');
  await page.locator('#lantern').waitFor({ state: 'visible' });
  assert.equal(await page.locator('#lantern').getAttribute('data-round'), '0');
  await page.screenshot({ path: path.join(output, 'lanterns-blending.png') });
  // Deterministic audio lifecycle: verify recording order and cancellation separately from the reading interaction.
  await page.evaluate(() => {
    window.lanternAudio = [];
    window.Audio = class {
      constructor(src) { this.src = src; }
      play() { window.lanternAudio.push(this.src); this.timer = setTimeout(() => this.onended?.(), 250); return Promise.resolve(); }
      pause() { clearTimeout(this.timer); }
    };
  });
  await click('#lantern-listen');
  await page.waitForTimeout(650);
  assert.deepEqual(await page.evaluate(() => window.lanternAudio), ['sounds/m.ogg', 'sounds/a.ogg']);
  await page.waitForTimeout(100);
  await page.evaluate(() => { window.lanternAudio = []; });
  await click('#lantern-listen'); await click('#sound'); await page.waitForTimeout(400);
  assert.equal(await page.evaluate(() => window.lanternAudio.length), 1, 'muting cancels the second queued phoneme');
  await click('#sound');
  const lanternRounds = require('../app/src/main/assets/forest/lanterns.js').rounds;
  for (const [i, round] of lanternRounds.entries()) {
    assert.equal(await page.locator('#lantern').getAttribute('data-round'), String(i));
    const progress = async () => page.evaluate(() => JSON.parse(localStorage.getItem('forest-lanterns-1') || '{"completed":0}').completed);
    if (i < 6) {
      const wrong = round.choices.find(text => text !== round.answer);
      await click(`#lantern-options [data-sound="${wrong}"]`);
      assert.equal(await progress(), i);
      await click('#lantern-help'); assert.equal(await progress(), i, 'help never solves a round');
      await click(`#lantern-options [data-sound="${round.answer}"]`);
    } else {
      if (i === 6) {
        await click('#lantern-options [data-sound="MI"]');
        await page.locator('#lantern-options [data-sound="MA"]:enabled').first().click();
        assert.equal(await progress(), i, 'wrong syllables do not complete a word');
        await click('#lantern-undo'); await click('#lantern-undo');
        await page.screenshot({ path: path.join(output, 'lanterns-word.png') });
      }
      for (const piece of round.solution) await page.locator(`#lantern-options [data-sound="${piece}"]:enabled`).first().click();
    }
    assert.equal(await progress(), i + 1);
    // A solved round is saved before the reward's Next button, including on Android Back.
    if (i === 1) {
      await page.reload(); await page.waitForSelector('#game[data-ready=true]');
      await click('#begin'); await click('#start-lanterns');
      assert.equal(await page.locator('#lantern').getAttribute('data-round'), '2');
    } else await click('#lantern-next');
  }
  assert.equal(await page.locator('#lantern-finale').isVisible(), true);
  assert.equal(await page.locator('.sound-lantern.lit').count(), 9);
  await page.screenshot({ path: path.join(output, 'lanterns-complete.png') });
  await click('#lantern-finish'); assert.equal(await page.locator('#ending').isVisible(), true);
  await click('#start-lanterns'); assert.equal(await page.locator('#lantern-finale').isVisible(), true);
  await click('#lantern-replay'); assert.equal(await page.locator('#lantern').getAttribute('data-round'), '0');
  await page.keyboard.press('Escape'); assert.equal(await page.locator('#ending').isVisible(), true);
  assert.equal(await phase(), 'complete');
  assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('forest-letters-1')).completed), 8, 'replaying lanterns preserves the earlier chapters');
  await context.close();
  for (const [width, height, label] of [[844, 390, 'phone'], [1024, 768, 'tablet'], [390, 844, 'portrait']]) {
    const { page: p, context: c } = await game(width, height, { version: 1, phase: 'treehouse', found: [], helped: {} });
    await p.locator('#begin').click(); await p.locator('#next-place').click(); await p.locator('#house-puzzle').waitFor({ state: 'visible' });
    const box = await p.locator('#puzzle').boundingBox(); assert.ok(box.x >= 0 && box.y >= 0 && box.x + box.width <= width + 1 && box.y + box.height <= height + 1, 'puzzle stays in the viewport');
    await p.screenshot({ path: path.join(output, `forest-${label}.png`) });
    for (const letter of ['O', 'M', 'A']) await p.locator(`[data-letter=${letter}]`).click();
    assert.equal(await p.locator('#game').getAttribute('data-phase'), 'complete');
    await p.locator('#success-next').click(); await p.locator('#start-lanterns').click();
    for (const completed of [0, 3, 6, 9]) {
      await p.evaluate(value => localStorage.setItem('forest-lanterns-1', JSON.stringify({ version: 1, completed: value })), completed);
      await p.reload(); await p.waitForSelector('#game[data-ready=true]'); await p.locator('#begin').click(); await p.locator('#start-lanterns').click();
      const panel = await p.locator('#lantern').boundingBox();
      assert.equal(await p.locator('#lantern').evaluate(e => e.scrollHeight <= e.clientHeight + 1), true, 'the complete lantern panel fits without hiding its back button');
      assert.ok(panel.x >= 0 && panel.y >= 0 && panel.x + panel.width <= width + 1 && panel.y + panel.height <= height + 1);
      for (const id of completed === 9 ? ['lantern-finish'] : ['lantern-listen', 'lantern-help']) {
        const control = p.locator('#' + id); await control.scrollIntoViewIfNeeded();
        const bounds = await control.boundingBox(); assert.ok(bounds.y >= panel.y && bounds.y + bounds.height <= panel.y + panel.height + 1, 'lantern controls remain reachable inside their panel');
      }
      if (completed === 6) await p.screenshot({ path: path.join(output, `lanterns-${label}.png`) });
    }
    await c.close();
  }
  for (const [width, height, label] of [[844, 390, 'phone'], [390, 844, 'portrait']]) {
    const { page: p, context: c } = await game(width, height);
    await p.locator('#begin').click(); await p.locator('#lesson-next').click();
    await p.locator('[data-answer=A]').click(); await p.locator('[data-answer=a]').click();
    if (label === 'portrait') {
      const partial = await p.locator('#trace-board').evaluate(e => [[65,195],[105,118],[132,65]].map(([x,y]) => {const q=e.createSVGPoint();q.x=x+15;q.y=y+12;const v=q.matrixTransform(e.getScreenCTM());return [v.x,v.y];}));
      await p.mouse.move(...partial[0]); await p.mouse.down();
      for (const point of partial.slice(1)) await p.mouse.move(...point, {steps:10});
      await p.mouse.up();
      const mark = await p.locator('.trace-start').getAttribute('cy'); assert.ok(Number(mark) < 150, 'lifting keeps the completed part of the A');
      const resume = await p.locator('#trace-board').evaluate(e => {const m=e.querySelector('.trace-start'),q=e.createSVGPoint();q.x=Number(m.getAttribute('cx'));q.y=Number(m.getAttribute('cy'));const v=q.matrixTransform(e.getScreenCTM());return [v.x,v.y];});
      await p.mouse.move(...resume); await p.mouse.down();
      const finish = await p.locator('#trace-board').evaluate(e => [[150,30],[235,195]].map(([x,y])=>{const q=e.createSVGPoint();q.x=x+15;q.y=y+12;const v=q.matrixTransform(e.getScreenCTM());return [v.x,v.y];}));
      for (const point of finish) await p.mouse.move(...point, {steps:20});
      await p.mouse.up();
      assert.match(await p.locator('#trace-count').textContent(), /Spur 2/, 'the destination confirms the first A stroke automatically');
      await p.locator('#trace-reset').click();
    }
    const box = await p.locator('#trace-board').boundingBox();
    assert.ok(box.x >= 0 && box.y >= 0 && box.x + box.width <= width + 1 && box.y + box.height <= height + 1);
    for (const id of ['trace-demo', 'trace-reset']) { const control = await p.locator('#' + id).boundingBox(); assert.ok(control.y >= 0 && control.y + control.height < height, 'writing help stays on screen'); assert.equal(await p.locator('#' + id).evaluate(e => e.getBoundingClientRect().bottom <= document.getElementById('lesson-body').getBoundingClientRect().bottom + 1), true, 'writing help is not clipped by its panel'); }
    await p.screenshot({ path: path.join(output, `letters-${label}.png`) });
    await traceForm(p, letters[0].upperStrokes, true); await p.locator('#lesson-next').click();
    await traceForm(p, letters[0].lowerStrokes, true); await p.locator('#lesson-next').click();
    await p.reload(); await p.waitForSelector('#game[data-ready=true]'); await p.locator('#begin').click();
    assert.equal(await p.locator('#lesson').getAttribute('data-step'), 'reward');
    await c.close();
  }
  assert.deepEqual(errors, [], 'no uncaught JavaScript errors');
  await browser.close(); await new Promise(resolve => server.close(resolve));
  console.log('PASS: nine lantern rounds, duplicate syllables, audio cancellation, three new layouts, all eight letters, real pointer writing of both cases, locked chapters, restart, three screen sizes and the complete word story');
})().catch(error => { console.error(error); server.close(); process.exit(1); });
