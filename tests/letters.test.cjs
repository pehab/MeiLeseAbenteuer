const { test } = require('node:test');
const assert = require('node:assert/strict');
const { letters, Lessons, Trace } = require('../app/src/main/assets/forest/letters.js');

test('All eight chapters require recognition of both cases and writing before the words unlock', () => {
  const lessons = new Lessons();
  assert.equal(letters.map(x => x.lower).join(''), 'aeioumnl');
  for (let i = 0; i < 8; i++) {
    assert.equal(lessons.choose(i + 1), false);
    assert.equal(lessons.advance('traced'), false);
    assert.equal(lessons.advance('start'), true);
    assert.equal(lessons.advance('wrong'), false);
    assert.equal(lessons.step, 'find');
    assert.equal(lessons.advance(letters[i].upper), true);
    assert.equal(lessons.advance(letters[i].upper), false);
    assert.equal(lessons.advance(letters[i].lower), true);
    assert.equal(lessons.completed, i);
    assert.equal(lessons.advance('traced'), true);
    assert.equal(lessons.completed, i);
    assert.equal(lessons.advance('traced'), true);
    assert.equal(lessons.completed, i + 1);
    assert.equal(lessons.done, i === 7);
    assert.equal(lessons.advance('traced'), false);
    assert.equal(new Lessons(JSON.parse(lessons.save())).completed, i + 1);
    if (i < 7) assert.equal(lessons.choose(i + 1), true);
  }
  assert.equal(lessons.choose(0), true);
  assert.equal(lessons.done, true, 'replaying never removes earned chapters');
});

test('Saved lesson resumes the exact exercise and invalid or future saves stay locked', () => {
  for (const saved of [null, {}, { version: 99, completed: 8 }, { version: 1, completed: -1 }, { version: 1, completed: 9 }, { version: 1, completed: '8' }]) assert.equal(new Lessons(saved).done, false);
  const lessons = new Lessons({ version: 1, completed: 3, index: 3, step: 'writeLower' });
  assert.equal(lessons.letter.upper, 'O');
  assert.equal(lessons.step, 'writeLower');
  assert.equal(lessons.advance('traced'), true);
  assert.equal(new Lessons(JSON.parse(lessons.save())).step, 'reward');
  assert.equal(new Lessons({ version: 1, completed: 0, index: 6, step: 'reward' }).step, 'meet');
});

test('Every upper and lower letter can be written stroke by stroke in the intended direction', () => {
  for (const letter of letters) for (const form of ['upperStrokes', 'lowerStrokes']) {
    const trace = new Trace(letter[form]);
    assert.equal(trace.done, false);
    for (const points of trace.strokes) {
      assert.equal(trace.begin(...points[0]), true, letter.upper + ' ' + form + ' start');
      for (const point of points) assert.equal(trace.move(...point), true, letter.upper + ' ' + form + ' corridor');
      assert.equal(trace.end(), true, letter.upper + ' ' + form + ' end');
    }
    assert.equal(trace.done, true);
  }
});

test('A tap, shortcut, reverse stroke, interrupted touch or scribble cannot finish a letter', () => {
  const trace = new Trace(letters[0].upperStrokes);
  assert.equal(trace.begin(10, 10), false);
  trace.begin(65, 195); assert.equal(trace.end(), false);
  trace.begin(65, 195); assert.equal(trace.move(235, 195), false); assert.equal(trace.end(), false);
  trace.begin(65, 195); assert.equal(trace.move(0, 0), false);
  assert.equal(trace.stroke, 0);
  assert.equal(trace.done, false);
  assert.equal(trace.begin(235, 195), false);
  trace.begin(65, 195); trace.move(80, 165); trace.cancel(); assert.equal(trace.end(), false);
  // A circular shape also needs its whole circumference, not just a tap on its shared endpoints.
  const circle = new Trace(letters[3].upperStrokes);
  circle.begin(...circle.target); assert.equal(circle.end(), false);
  assert.equal(circle.done, false);
});

test('Completed strokes survive a mistake on a later stroke; a dot is a valid separate stroke', () => {
  const trace = new Trace(letters[2].lowerStrokes);
  trace.begin(...trace.target); for (const point of trace.strokes[0]) trace.move(...point); assert.equal(trace.end(), true);
  assert.equal(trace.stroke, 1); assert.equal(trace.done, false);
  assert.equal(trace.begin(0, 0), false); assert.equal(trace.stroke, 1);
  trace.begin(...trace.target); trace.move(...trace.target); assert.equal(trace.end(), true);
  assert.equal(trace.done, true);
});

test('Round letters guide counterclockwise and the n/m downstrokes begin at the top', () => {
  for (const form of ['upperStrokes', 'lowerStrokes']) {
    const points = letters[3][form][0];
    assert.ok(points[1][0] < points[0][0]);
  }
  for (const index of [5, 6]) {
    const stem = letters[index].lowerStrokes[0];
    assert.ok(stem[0][1] < stem[1][1]);
  }
});
