'use strict';
window.createLetterJourney = function (hooks) {
  const { letters, Lessons, Trace } = ForestLetters;
  const $ = id => document.getElementById(id), host = window.ForestHost;
  let saved;
  try { saved = JSON.parse(host ? host.loadLetters() : localStorage.getItem('forest-letters-1')); } catch (_) { saved = null; }
  const lessons = new Lessons(saved);
  const ns = 'http://www.w3.org/2000/svg';
  let opened = false, map = false, trace, drawing = null, demonstration = 0, speech = '', phoneme;
  const board = $('trace-board');
  function node(tag, attrs) { const e = document.createElementNS(ns, tag); for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v); return e; }
  function path(points) { return points.map((p, i) => (i ? 'L' : 'M') + p.join(',')).join(' '); }
  function glyph(letter, lower = false) {
    const svg = node('svg', { viewBox: '0 0 300 230', 'aria-label': lower ? letter.lower : letter.upper, role: 'img', class: 'lesson-glyph' });
    for (const points of lower ? letter.lowerStrokes : letter.upperStrokes) svg.append(node('path', { d: path(points), fill: 'none', stroke: 'currentColor', 'stroke-width': 14, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }));
    return svg;
  }
  function pair(element) { element.textContent = ''; element.append(glyph(lessons.letter), glyph(lessons.letter, true)); }
  function persist() {
    try { if (host) host.saveLetters(lessons.save()); else localStorage.setItem('forest-letters-1', lessons.save()); }
    catch (_) { $('lesson-feedback').textContent = 'Der Spielstand konnte nicht gespeichert werden.'; }
  }
  function stopAudio() { if (phoneme) { phoneme.pause(); phoneme.currentTime = 0; } hooks.stopSpeech(); }
  function speak(text) { speech = text; stopAudio(); hooks.say(text); }
  function sound() {
    stopAudio();
    if (hooks.muted()) return;
    const key = lessons.letter.lower;
    // E is a vowel and uses the German offline voice; the other seven are existing recordings.
    if (key === 'e') { hooks.say('E'); return; }
    phoneme = new Audio('sounds/' + key + '.ogg');
    phoneme.play().catch(() => speak('Der Laut konnte nicht abgespielt werden. Bitte im Erwachsenenbereich die Stimme prüfen.'));
  }
  function cancelDemo() { demonstration++; board.querySelector('.trace-demo-dot')?.remove(); }
  function drawGuide() {
    cancelDemo(); board.textContent = '';
    for (const y of [30, 85, 195]) board.append(node('path', { d: `M20,${y} L280,${y}`, stroke: '#d5c39b', 'stroke-width': 1, 'stroke-dasharray': '4 5' }));
    trace.strokes.forEach((points, i) => {
      board.append(node('path', { d: path(points), fill: 'none', stroke: i < trace.stroke ? '#51835b' : '#dfd2ae', 'stroke-width': 25, 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'trace-guide' }));
      if (i >= trace.stroke) board.append(node('path', { d: path(points), fill: 'none', stroke: '#b49a65', 'stroke-width': 2, 'stroke-dasharray': '2 7' }));
    });
    if (!trace.done) {
      const start = trace.strokes[trace.stroke][0];
      board.append(node('circle', { cx: start[0], cy: start[1], r: 13, fill: '#365e42', class: 'trace-start' }));
      const number = node('text', { x: start[0], y: start[1] + 4, 'text-anchor': 'middle', fill: '#fff7de', 'font-size': 12, 'font-family': 'sans-serif', 'pointer-events': 'none' });
      number.textContent = trace.stroke + 1; board.append(number);
    }
    $('trace-count').textContent = `Spur ${Math.min(trace.stroke + 1, trace.strokes.length)} von ${trace.strokes.length}`;
    $('lesson-next').disabled = !trace.done;
  }
  function render() {
    map = false; drawing = null; cancelDemo();
    const letter = lessons.letter, step = lessons.step;
    $('lesson').dataset.index = lessons.index; $('lesson').dataset.step = step;
    $('lesson-eyebrow').textContent = `BUCHSTABENPFAD · KAPITEL ${lessons.index + 1} VON 8`;
    $('lesson-title').textContent = letter.title;
    $('lesson-story').textContent = letter.story;
    hooks.paintSprite($('lesson-friend-art'), 'animal', letter.animal);
    $('lesson-trail').textContent = '';
    letters.forEach((item, i) => { const mark = document.createElement('span'); mark.textContent = i < lessons.completed ? '✦' : i === lessons.index ? item.upper : '·'; mark.className = i === lessons.index ? 'current' : i < lessons.completed ? 'earned' : ''; $('lesson-trail').append(mark); });
    for (const [id, show] of Object.entries({ 'letter-meeting': step === 'meet', 'letter-finding': step === 'find' || step === 'pair', 'letter-writing': step.startsWith('write'), 'letter-reward': step === 'reward', 'lesson-map': false })) $(id).hidden = !show;
    $('lesson-next').hidden = false; $('lesson-next').disabled = false; $('letter-sound').hidden = false;
    $('lesson-feedback').textContent = '';
    const hints = {
      meet: 'Hör hin und schau dir beide Formen an. Groß und klein gehören zusammen.',
      find: 'Finde den gleichen großen Buchstaben. Tippe auf seine Spur.',
      pair: 'Welcher kleine Buchstabe gehört dazu? Du hast die beiden schon zusammen gesehen.',
      writeUpper: 'Schreibe den großen Buchstaben. Beginne am grünen Punkt. Folge der Spur bis zum Ende. Bei einer neuen Zahl setzt du neu an.',
      writeLower: 'Jetzt der kleine Buchstabe. Beginne wieder am grünen Punkt und folge der Spur.',
      reward: letter.reward
    };
    $('lesson-next').innerHTML = step === 'reward' ? (lessons.index === 7 ? 'Zur Sternenpost' : 'Weiter auf dem Waldweg') + ' <span>➜</span>' : step === 'meet' ? 'Buchstaben entdecken <span>➜</span>' : 'Weiter <span>➜</span>';
    if (step === 'meet') pair($('letter-pair'));
    if (step === 'find' || step === 'pair') {
      const lower = step === 'pair';
      $('letter-model').textContent = ''; $('letter-model').append(glyph(letter));
      const label = document.createElement('span'); label.textContent = lower ? 'Finde den kleinen dazu' : 'Finde den gleichen'; $('letter-model').append(label);
      $('letter-options').textContent = '';
      // Distractors are visual choices; no knowledge of their sounds is required.
      const previous = letters.filter((_, i) => i < lessons.index).slice(-3);
      const options = ForestStory.shuffled([letter, ...previous].slice(0, 4));
      if (options.length < 3) for (const i of [3, 5]) if (!options.includes(letters[i]) && options.length < 3) options.push(letters[i]);
      ForestStory.shuffled(options).forEach(option => {
        const b = document.createElement('button'); b.className = 'letter-leaf'; b.dataset.answer = lower ? option.lower : option.upper; b.setAttribute('aria-label', `Buchstabe ${b.dataset.answer}`); b.append(glyph(option, lower));
        b.onclick = () => {
          if (!lessons.advance(b.dataset.answer)) { b.classList.remove('wrong'); void b.offsetWidth; b.classList.add('wrong'); $('lesson-feedback').textContent = 'Schau noch einmal. Du hast Zeit.'; speak('Schau noch einmal. Du kannst dir die beiden Formen noch einmal zeigen lassen.'); return; }
          hooks.chime(); persist(); render();
        }; $('letter-options').append(b);
      });
      $('lesson-next').hidden = true;
    }
    if (step.startsWith('write')) {
      trace = new Trace(step === 'writeUpper' ? letter.upperStrokes : letter.lowerStrokes);
      $('trace-label').textContent = step === 'writeUpper' ? 'Der große Buchstabe' : 'Der kleine Buchstabe';
      drawGuide();
    }
    if (step === 'reward') { pair($('reward-pair')); $('letter-reward-text').textContent = letter.reward; hooks.chime(true); }
    $('lesson-feedback').textContent = hints[step];
    speak(step === 'meet' ? letter.story + ' ' + hints[step] : hints[step]);
    hooks.travel(lessons.index);
  }
  function showMap() {
    cancelDemo(); if (trace) trace.cancel(); drawing = null; map = true;
    for (const id of ['letter-meeting', 'letter-finding', 'letter-writing', 'letter-reward']) $(id).hidden = true;
    $('lesson-map').hidden = false; $('lesson-next').hidden = true; $('letter-sound').hidden = true;
    $('lesson-map-buttons').textContent = '';
    letters.forEach((letter, i) => {
      const b = document.createElement('button'); b.className = 'map-letter'; b.dataset.chapter = i; b.disabled = i > lessons.completed; b.setAttribute('aria-label', `Kapitel ${i + 1}: ${letter.upper}${i < lessons.completed ? ', geschafft' : i > lessons.completed ? ', noch geschlossen' : ', als Nächstes'}`);
      b.append(glyph(letter)); const status = document.createElement('span'); status.textContent = i < lessons.completed ? '✦' : i > lessons.completed ? '•' : '➜'; b.append(status);
      b.onclick = () => { lessons.choose(i); persist(); render(); }; $('lesson-map-buttons').append(b);
    });
    $('lesson-to-story').hidden = !lessons.done;
    $('lesson-feedback').textContent = lessons.done ? 'Alle acht Buchstaben sind entdeckt. Die Sternenpost wartet!' : 'Jeder neue Buchstabe öffnet einen weiteren Teil deines Weges.';
    speak($('lesson-feedback').textContent);
  }
  function open(showPath = false) { opened = true; $('lesson').hidden = false; hooks.opened(); if (showPath) showMap(); else render(); }
  function close(finished) { cancelDemo(); stopAudio(); opened = false; $('lesson').hidden = true; if (finished) hooks.finished(); else hooks.exited(); }
  $('lesson-next').onclick = () => {
    if (map) return;
    if (lessons.step === 'reward') {
      if (lessons.index === 7) { if (lessons.done) close(true); return; }
      lessons.choose(lessons.index + 1); persist(); render(); return;
    }
    if (lessons.step.startsWith('write') && !trace.done) return;
    if (lessons.advance(lessons.step === 'meet' ? 'start' : 'traced')) { persist(); render(); }
  };
  $('lesson-path').onclick = () => { if (map) render(); else showMap(); };
  $('lesson-repeat').onclick = () => { if (!map && ['meet', 'find', 'pair'].includes(lessons.step)) { pair($('letter-pair')); $('letter-meeting').hidden = false; speak('Schau dir den großen und den kleinen Buchstaben an. Die beiden gehören zusammen.'); } else speak(speech); };
  $('letter-sound').onclick = sound;
  $('lesson-to-story').onclick = () => { if (lessons.done) close(true); };
  $('trace-reset').onclick = () => { cancelDemo(); trace = new Trace(lessons.step === 'writeUpper' ? lessons.letter.upperStrokes : lessons.letter.lowerStrokes); drawing = null; drawGuide(); $('lesson-feedback').textContent = 'Beginne am grünen Punkt.'; };
  $('trace-demo').onclick = () => {
    cancelDemo(); trace.cancel(); drawing = null; drawGuide(); const run = demonstration;
    if (trace.done) return;
    const points = trace.strokes[trace.stroke], dot = node('circle', { r: 9, fill: '#c08031', class: 'trace-demo-dot', 'pointer-events': 'none' });
    board.append(dot); const start = performance.now();
    function animate(now) {
      if (run !== demonstration || !opened || map) return;
      const i = Math.min(points.length - 1, Math.floor((now - start) / 28)); dot.setAttribute('cx', points[i][0]); dot.setAttribute('cy', points[i][1]);
      if (i < points.length - 1) requestAnimationFrame(animate); else dot.remove();
    }
    requestAnimationFrame(animate); speak('Schau, so geht die nächste Spur. Danach bist du dran.');
  };
  function coords(e) { const p = board.createSVGPoint(); p.x = e.clientX; p.y = e.clientY; const c = p.matrixTransform(board.getScreenCTM().inverse()); return [c.x, c.y]; }
  board.onpointerdown = e => {
    if (!trace || trace.done || drawing !== null) return;
    e.preventDefault(); cancelDemo();
    const p = coords(e);
    if (!trace.begin(...p)) { $('lesson-feedback').textContent = 'Beginne am grünen Punkt mit der Zahl.'; return; }
    drawing = e.pointerId; board.setPointerCapture(e.pointerId);
    const ink = node('path', { d: `M${p.join(',')}`, fill: 'none', stroke: '#4a7e56', 'stroke-width': 12, 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'trace-ink' }); board.append(ink);
  };
  board.onpointermove = e => {
    if (drawing !== e.pointerId) return;
    e.preventDefault(); const p = coords(e);
    if (!trace.move(...p)) { drawing = null; drawGuide(); $('lesson-feedback').textContent = 'Bleibe auf dem hellen Weg. Starte diese Spur noch einmal.'; return; }
    const ink = board.querySelector('.trace-ink'); if (ink) ink.setAttribute('d', ink.getAttribute('d') + ` L${p.join(',')}`);
  };
  board.onpointerup = e => {
    if (drawing !== e.pointerId) return;
    drawing = null; const valid = trace.move(...coords(e)) && trace.end(); drawGuide();
    $('lesson-feedback').textContent = valid ? trace.done ? 'Geschafft! Dein Buchstabe leuchtet. Tippe auf den Pfeil.' : 'Gut! Setze am nächsten grünen Punkt neu an.' : 'Versuche diese Spur noch einmal vom grünen Punkt bis zum Ende.';
    if (valid) { hooks.chime(trace.done); if (trace.done) speak('Dein Buchstabe leuchtet! Weiter mit dem Pfeil.'); }
  };
  board.onpointercancel = () => { if (trace) trace.cancel(); drawing = null; if (trace) drawGuide(); };
  return {
    open, get done() { return lessons.done; }, get active() { return opened; },
    back() { if (!opened) return false; if (!map) showMap(); else close(false); return true; },
    pause() { stopAudio(); cancelDemo(); if (trace) trace.cancel(); drawing = null; if (opened && !map && lessons.step.startsWith('write')) drawGuide(); },
    stopAudio
  };
};
