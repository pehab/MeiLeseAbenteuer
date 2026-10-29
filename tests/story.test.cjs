const { test } = require('node:test');
const assert = require('node:assert/strict');
const { Story, shuffled } = require('../app/src/main/assets/forest/story.js');

test('A chapter requires its encounters in order; wrong and repeated answers never unlock the next scene', () => {
  const story = new Story();
  assert.equal(story.advance('OMA'), false);
  assert.equal(story.advance('lemonade'), false);
  assert.equal(story.advance('accept'), true);
  assert.equal(story.advance('accept'), false);
  assert.equal(story.advance('lamp'), false);
  assert.equal(story.phase, 'river');
  assert.equal(story.advance('lemonade'), true);
  for (const answer of ['MOA', 'OAM', 'AMI', 'OMA ', 'oma']) assert.equal(story.advance(answer), false);
  assert.equal(story.phase, 'treehouse');
  assert.equal(story.advance('OMA'), true);
  assert.equal(story.advance('OMA'), false);
  assert.equal(story.phase, 'complete');
});
test('Exiting after either solved puzzle resumes at the next location, including assistance and discoveries', () => {
  const original = new Story(); original.advance('accept'); original.help(); original.discover(2); original.advance('lemonade');
  const resumed = new Story(JSON.parse(original.save()));
  assert.equal(resumed.phase, 'treehouse'); assert.deepEqual(resumed.helped, { river: true, treehouse: false });
  assert.deepEqual(resumed.found, [2]); resumed.help(); resumed.advance('OMA');
  const complete = new Story(JSON.parse(resumed.save()));
  assert.equal(complete.phase, 'complete'); assert.equal(complete.helped.treehouse, true);
});
test('Missing, malformed and future-format saves cannot unlock a chapter', () => {
  for (const save of [undefined, null, [], 'complete', { phase: 'complete' }, { version: 99, phase: 'complete' }, { version: 1, phase: 'bad' }]) {
    assert.equal(new Story(save).phase, 'meeting');
  }
  const story = new Story({ version: 1, phase: 'river', found: [2, 2, -1, 3, '0', 0, 1], helped: null });
  assert.deepEqual(story.found, [2, 0, 1]); assert.equal(story.discover(2), false); assert.equal(story.discover(7), false);
});
test('Assistance and optional secrets do not substitute for reading a word', () => {
  const story = new Story(); [0, 1, 2].forEach(n => story.discover(n)); story.help();
  assert.equal(story.phase, 'meeting'); story.advance('accept'); story.help();
  assert.equal(story.phase, 'river'); assert.equal(story.helped.river, true);
});
test('Shuffled choices retain every unique option and do not mutate their source', () => {
  const source = ['O', 'M', 'A', 'I']; const mixed = shuffled(source, () => 0);
  assert.deepEqual(source, ['O', 'M', 'A', 'I']); assert.deepEqual([...mixed].sort(), [...source].sort()); assert.notDeepEqual(mixed, source);
});
