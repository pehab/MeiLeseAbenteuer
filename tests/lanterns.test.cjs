const { test } = require('node:test');
const assert = require('node:assert/strict');
const { Lanterns, rounds } = require('../app/src/main/assets/forest/lanterns.js');

test('Reading follows guided blends, heard syllables and then meaningful words', () => {
  assert.deepEqual(rounds.map(r => r.kind), ['blend', 'blend', 'blend', 'hear', 'hear', 'hear', 'word', 'word', 'word']);
  for (const round of rounds) {
    assert.match(round.answer, /^[AEIOUMNL]+$/);
    if (round.kind === 'word') {
      assert.equal(round.solution.join(''), round.answer);
      const available = [...round.pieces];
      for (const piece of round.solution) { const index = available.indexOf(piece); assert.ok(index >= 0); available.splice(index, 1); }
    } else assert.equal(round.choices.filter(s => s === round.answer).length, 1);
  }
});
test('Wrong answers and help cannot light a lantern; every success survives a restart', () => {
  let chapter = new Lanterns();
  for (const [index, round] of rounds.entries()) {
    assert.equal(chapter.completed, index);
    for (const bad of ['', 'wrong', round.answer.toLowerCase(), round.answer + ' ']) assert.equal(chapter.answer(bad), false);
    assert.equal(chapter.completed, index);
    assert.equal(chapter.answer(round.answer), true);
    chapter = new Lanterns(JSON.parse(chapter.save()));
    assert.equal(chapter.completed, index + 1);
  }
  assert.equal(chapter.done, true); assert.equal(chapter.answer('LIMO'), false);
});
test('Malformed and future saves cannot unlock lanterns', () => {
  for (const save of [null, [], 'done', { completed: 9 }, { version: 2, completed: 9 }, ...[-1, 10, 1.2, '9', true, null].map(completed => ({ version: 1, completed }))]) assert.equal(new Lanterns(save).completed, 0);
});
