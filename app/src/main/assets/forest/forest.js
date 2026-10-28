'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const { Story, shuffled } = ForestStory;
  const host = window.ForestHost;
  // Detect flex layout spacing rather than relying on a WebView version number.
  const flexProbe = document.createElement('div');
  flexProbe.style.cssText = 'display:flex;flex-direction:column;row-gap:1px;position:absolute;visibility:hidden';
  for (let i = 0; i < 2; i++) { const child = document.createElement('i'); child.style.height = '1px'; flexProbe.append(child); }
  document.body.append(flexProbe);
  document.body.classList.toggle('no-flex-gap', flexProbe.scrollHeight !== 3);
  flexProbe.remove();
  let restored;
  try { restored = JSON.parse(host ? host.load() : localStorage.getItem('forest-chapter-2')); } catch (_) { restored = null; }
  const story = new Story(restored);
  let started = false, modal = false, busy = false, muted = false, paused = false, lastSpeech = '', audio, walking = null;
  let foxX = 450, foxY = 800, camera = 0, targetCamera = 0, scale = 1, viewWidth = 1600, lastFrame = 0;
  let nextDialogue = null, successAction = null, picked = [], letters = [], drag = null, pan = null, toastTimer, speechTimer;
  const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const atlas = new Image(), pictures = new Image();
  const animalRects = [[0, 0, 620, 540], [620, 0, 390, 540], [1010, 0, 526, 540], [0, 540, 560, 484], [560, 540, 470, 484], [1030, 540, 506, 484]];
  const pictureIndexes = { lamp: 3, book: 15, lemonade: 12, grandmother: 9 };
  function paintSprite(canvas, kind, value) {
    const source = kind === 'animal' ? atlas : pictures;
    const rect = kind === 'animal' ? animalRects[value] : [pictureIndexes[value] % 4 * source.width / 4, Math.floor(pictureIndexes[value] / 4) * source.height / 4, source.width / 4, source.height / 4];
    canvas.width = rect[2]; canvas.height = rect[3];
    canvas.getContext('2d').drawImage(source, ...rect, 0, 0, rect[2], rect[3]);
    canvas.style.objectFit = 'contain';
  }
  function paintAll() {
    document.querySelectorAll('canvas[data-animal]').forEach(c => paintSprite(c, 'animal', +c.dataset.animal));
    document.querySelectorAll('canvas[data-picture]').forEach(c => paintSprite(c, 'picture', c.dataset.picture));
  }
  function save() {
    const value = story.save();
    try { if (host) host.save(value); else localStorage.setItem('forest-chapter-2', value); }
    catch (_) { toast('Der Spielstand konnte nicht gespeichert werden.'); }
  }
  function say(text) {
    lastSpeech = text;
    clearTimeout(speechTimer);
    if (muted || paused) return;
    if (host) { host.speak(text); return; }
    if (window.speechSynthesis) {
      speechSynthesis.cancel();
      const voice = speechSynthesis.getVoices().find(v => v.lang.startsWith('de') && v.localService);
      if (!voice) return;
      const utterance = new SpeechSynthesisUtterance(text); utterance.voice = voice; utterance.lang = 'de-DE'; utterance.rate = .85;
      speechSynthesis.speak(utterance);
    }
  }
  function stopSpeech() { if (host) host.stopSpeech(); else window.speechSynthesis?.cancel(); }
  function initAudio() {
    if (!audio && window.AudioContext) audio = new AudioContext();
    if (!muted && !paused) audio?.resume().catch(() => {});
  }
  function chime(success = false) {
    if (!audio || muted || paused) return;
    const now = audio.currentTime;
    (success ? [523.25, 659.25, 783.99, 1046.5] : [659.25, 880]).forEach((frequency, i) => {
      const oscillator = audio.createOscillator(), gain = audio.createGain();
      oscillator.type = 'sine'; oscillator.frequency.value = frequency;
      gain.gain.setValueAtTime(0, now + i * .11); gain.gain.linearRampToValueAtTime(.075, now + i * .11 + .025); gain.gain.exponentialRampToValueAtTime(.001, now + i * .11 + .65);
      oscillator.connect(gain); gain.connect(audio.destination); oscillator.start(now + i * .11); oscillator.stop(now + i * .11 + .7);
    });
  }
  function toast(message) {
    $('toast').textContent = message; $('toast').classList.add('visible');
    clearTimeout(toastTimer); toastTimer = setTimeout(() => $('toast').classList.remove('visible'), 3600);
  }
  function update() {
    $('game').dataset.phase = story.phase;
    $('game').classList.toggle('river-done', ['treehouse', 'complete'].includes(story.phase));
    $('game').classList.toggle('complete', story.phase === 'complete');
    $('house-stop').classList.toggle('locked', ['meeting', 'river'].includes(story.phase));
    $('rabbit').querySelector('.attention').hidden = story.phase !== 'meeting';
    $('frog').querySelector('.attention').hidden = story.phase !== 'river';
    $('objective-text').textContent = { meeting: 'Nila hat etwas für dich.', river: 'Bringe die Sternenpost über den Bach.', treehouse: 'Finde den Namen für die Sternenpost.', complete: 'Im Baumhaus leuchtet wieder ein Licht.' }[story.phase];
    $('next-place').innerHTML = { meeting: 'Zu Nila <span>➜</span>', river: 'Zum Bach <span>➜</span>', treehouse: 'Zum Baumhaus <span>➜</span>', complete: 'Zum Baumhaus <span>➜</span>' }[story.phase];
    $('discovery-count').textContent = `✧ ${story.found.length}/3`;
    document.querySelectorAll('[data-secret]').forEach(b => { b.classList.toggle('found', story.found.includes(+b.dataset.secret)); b.disabled = story.found.includes(+b.dataset.secret); });
    $('fox').querySelector('.postbag').hidden = story.phase === 'meeting' || story.phase === 'complete';
    $('objective').hidden = !started || modal;
    $('explore-hint').hidden = !started || modal;
    $('next-place').hidden = !started || modal || busy;
  }
  function resize() {
    scale = $('game').clientHeight / 900;
    viewWidth = $('game').clientWidth / scale;
    targetCamera = Math.max(0, Math.min(2500 - viewWidth, foxX - viewWidth * .43));
    camera = Math.max(0, Math.min(Math.max(0, 2500 - viewWidth), camera));
  }
  function follow(x = foxX) { targetCamera = Math.max(0, Math.min(Math.max(0, 2500 - viewWidth), x - viewWidth * .43)); }
  function moveTo(x, y, after) {
    if (busy || modal) return;
    initAudio(); busy = true; walking = { fromX: foxX, fromY: foxY, toX: x, toY: y, duration: reduceMotion ? .3 : Math.max(1, Math.abs(x - foxX) / 230), elapsed: 0, after };
    $('fox').classList.toggle('facing-left', x < foxX); $('fox').classList.add('walking'); update();
  }
  function frame(now) {
    const dt = lastFrame ? Math.min(.05, (now - lastFrame) / 1000) : 0; lastFrame = now;
    if (!paused && !document.hidden) {
      if (walking) {
        walking.elapsed += dt; const t = Math.min(1, walking.elapsed / walking.duration), eased = t * t * (3 - 2 * t);
        foxX = walking.fromX + (walking.toX - walking.fromX) * eased;
        foxY = walking.fromY + (walking.toY - walking.fromY) * eased;
        follow();
        if (t === 1) { const done = walking.after; walking = null; busy = false; $('fox').classList.remove('walking'); update(); if (done) done(); }
      }
      camera += (targetCamera - camera) * Math.min(1, dt * (reduceMotion ? 30 : 4.5));
      $('fox').style.left = `${foxX - 135}px`; $('fox').style.top = `${foxY - 235}px`;
      $('world').style.transform = `translate3d(${-camera * scale}px,0,0) scale(${scale})`;
    }
    requestAnimationFrame(frame);
  }
  function dialogue(speaker, text, button, action, animal = 1) {
    modal = true; $('dialogue').hidden = false; $('speaker').textContent = speaker; $('dialogue-text').textContent = text;
    $('dialogue-next').innerHTML = `${button} <span>➜</span>`; nextDialogue = action;
    paintSprite($('speaker-art'), 'animal', animal); update(); say(text);
  }
  function closeDialogue() { $('dialogue').hidden = true; modal = false; const action = nextDialogue; nextDialogue = null; stopSpeech(); update(); if (action) action(); }
  function nila() {
    if (!started || modal || busy) return;
    if (story.phase === 'meeting') dialogue('NILA · DIE STERNENBOTIN', 'Da bist du ja! Diese Sternenpost muss zu Oma ins Baumhaus. Kommst du mit? Taro kennt einen Weg über den Bach.', 'Ich helfe dir', () => { story.advance('accept'); save(); update(); });
    else dialogue('NILA', story.phase === 'complete' ? 'Du hast die Post gebracht! Schau nur, wie schön es im Baumhaus leuchtet.' : 'Die Sternenpost ist bei dir gut aufgehoben. Ich passe hier auf die übrigen Briefe auf.', 'Weiter', null);
  }
  function frog() {
    if (!started || modal || busy) return;
    if (story.phase === 'meeting') { toast('Nila möchte dir zuerst etwas erzählen.'); say('Schau erst einmal bei Nila vorbei.'); return; }
    if (story.phase === 'river') dialogue('TARO · DER FÄHRMANN', 'Quak! Natürlich nehme ich dich mit. Mein Wunsch steht auf diesem Zettel. Findest du, was ich meine?', 'Zettel ansehen', openRiver, 5);
    else dialogue('TARO', 'Quak! Eine gute Überfahrt für eine wichtige Nachricht. Am Baumhaus wartet Oma auf dich.', 'Weiter', null, 5);
  }
  function destination() {
    if (modal || busy) return;
    if (story.phase === 'meeting') moveTo(470, 800, nila);
    else if (story.phase === 'river') moveTo(1120, 820, frog);
    else moveTo(2080, 565, () => { if (story.phase === 'treehouse') openHouse(); else showEnding(); });
  }
  function puzzle(kind, eyebrow, title, instruction) {
    modal = true; busy = false; $('game').dataset.puzzle = kind; $('puzzle').hidden = false; $('game').classList.add('puzzling');
    $('river-puzzle').hidden = kind !== 'river'; $('house-puzzle').hidden = kind !== 'house';
    $('puzzle-success').hidden = true; $('puzzle-eyebrow').textContent = eyebrow; $('puzzle-title').textContent = title;
    $('feedback').textContent = ''; $('help').disabled = false; update(); say(instruction);
  }
  function closePuzzle() { if (busy) return; $('puzzle').hidden = true; $('game').classList.remove('puzzling'); $('game').dataset.puzzle = ''; modal = false; picked = []; stopSpeech(); update(); }
  const riverInstruction = 'Lies das Wort auf Taros Wunschzettel. Tippe auf das passende Bild. Wenn du Hilfe brauchst, tippe auf Hilf mir.';
  const houseInstruction = 'Die Sternenpost ist für Oma. Setze das Wort Oma aus den Buchstaben zusammen. Tippe die Buchstaben an oder ziehe sie auf den Brief.';
  function openRiver() {
    puzzle('river', 'AM BACH · EIN WUNSCHZETTEL', 'Was möchte Taro? ', riverInstruction);
    $('object-choices').textContent = '';
    shuffled([['lamp', 'Lampe'], ['book', 'Buch'], ['lemonade', 'Limo']]).forEach(([kind, label]) => {
      const button = document.createElement('button'); button.className = 'object-choice'; button.setAttribute('aria-label', label); button.dataset.choice = kind;
      const canvas = document.createElement('canvas'); paintSprite(canvas, 'picture', kind); button.append(canvas);
      button.onclick = () => {
        if (busy || $('game').dataset.puzzle !== 'river') return;
        if (!story.advance(kind)) { feedback(button, 'Schau noch einmal auf das Wort. Du kannst dir helfen lassen.'); return; }
        button.classList.add('selected'); save(); update();
        carryPicture(canvas, document.querySelector('.puzzle-frog'), () => success('Genau, eine Limo!', 'Taro freut sich. Jetzt kann die Sternenpost über den Bach.', () => {
          closePuzzle(); moveTo(1560, 760, () => { dialogue('TARO', 'Festhalten! Die Sternenpost ist drüben. Da oben ist schon das Baumhaus!', 'Zum Baumhaus', () => moveTo(2080, 565, openHouse), 5); });
        }));
      }; $('object-choices').append(button);
    });
  }
  function openHouse() {
    if (story.phase !== 'treehouse') return;
    picked = []; letters = shuffled(['O', 'M', 'A', 'I']);
    puzzle('house', 'AM BAUMHAUS · DIE STERNENPOST', 'Für wen ist der Brief?', houseInstruction);
    renderLetters();
  }
  function renderLetters() {
    $('letter-slots').textContent = '';
    for (let i = 0; i < 3; i++) { const slot = document.createElement('div'); slot.className = `letter-slot${i === picked.length ? ' next' : ''}`; slot.textContent = picked[i] ?? ''; slot.setAttribute('aria-label', `Platz ${i + 1}: ${picked[i] ?? 'leer'}`); $('letter-slots').append(slot); }
    $('letter-choices').textContent = '';
    letters.forEach(letter => {
      const button = document.createElement('button'); button.className = 'letter-tile'; button.textContent = letter; button.dataset.letter = letter; button.setAttribute('aria-label', `Buchstabe ${letter}`); button.disabled = picked.includes(letter) || busy;
      button.classList.toggle('used', picked.includes(letter));
      button.onclick = () => addLetter(letter);
      button.onpointerdown = e => { if (button.disabled) return; drag = { letter, x: e.clientX, y: e.clientY }; button.setPointerCapture(e.pointerId); };
      button.onpointermove = e => {
        if (!drag || drag.letter !== letter || Math.hypot(e.clientX - drag.x, e.clientY - drag.y) < 10) return;
        if (!drag.ghost) { drag.ghost = button.cloneNode(true); drag.ghost.className = 'letter-tile drag-ghost'; $('game').append(drag.ghost); }
        drag.ghost.style.left = `${e.clientX}px`; drag.ghost.style.top = `${e.clientY}px`;
      };
      button.onpointerup = e => {
        if (!drag || drag.letter !== letter) return;
        const moved = Math.hypot(e.clientX - drag.x, e.clientY - drag.y) > 15;
        const box = $('letter-slots').getBoundingClientRect(); drag.ghost?.remove(); drag = null;
        if (moved) { button.onclick = null; if (e.clientX >= box.left && e.clientX <= box.right && e.clientY >= box.top - 15 && e.clientY <= box.bottom + 15) addLetter(letter); else button.onclick = () => addLetter(letter); }
      };
      button.onpointercancel = () => { drag?.ghost?.remove(); drag = null; };
      $('letter-choices').append(button);
    });
    $('undo-letter').disabled = !picked.length || busy;
  }
  function addLetter(letter) {
    if (busy || picked.length >= 3 || picked.includes(letter) || $('game').dataset.puzzle !== 'house') return;
    initAudio(); picked.push(letter); renderLetters(); chime();
    $('feedback').textContent = '';
    if (picked.length === 3) {
      if (story.advance(picked.join(''))) {
        save(); update(); success('Die Post ist für Oma!', 'Der Name stimmt. Jetzt findet die Sternenpost ihr Ziel.', () => { closePuzzle(); showEnding(); });
      } else {
        feedback($('letter-slots'), 'Das ist noch nicht Oma. Lege Buchstaben zurück und versuche es noch einmal.');
      }
    }
  }
  function feedback(element, message) {
    element.classList.remove('wrong'); void element.offsetWidth; element.classList.add('wrong');
    $('feedback').textContent = message; say(message);
  }
  function carryPicture(canvas, destination, after) {
    if (reduceMotion || !canvas.animate) { after(); return; }
    busy = true;
    const from = canvas.getBoundingClientRect(), to = destination.getBoundingClientRect();
    const gift = document.createElement('canvas'); gift.width = canvas.width; gift.height = canvas.height;
    gift.getContext('2d').drawImage(canvas, 0, 0);
    gift.className = 'carried-gift';
    gift.style.cssText = `left:${from.left}px;top:${from.top}px;width:${from.width}px;height:${from.height}px`;
    $('game').append(gift);
    const flight = gift.animate([
      { transform: 'translate(0,0) scale(1)', opacity: 1 },
      { transform: `translate(${to.left + to.width / 2 - from.left - from.width / 2}px,${to.top + to.height / 2 - from.top - from.height / 2}px) scale(.45) rotate(-12deg)`, opacity: .9 }
    ], { duration: 650, easing: 'ease-in-out', fill: 'forwards' });
    flight.onfinish = () => { gift.remove(); after(); };
  }
  function success(title, text, action) {
    busy = true; $('puzzle-success').hidden = false; $('success-title').textContent = title; $('success-text').textContent = text;
    successAction = action; chime(true); say(`${title} ${text}`); update();
    $('success-next').focus({ preventScroll: true });
  }
  function showEnding() {
    modal = true; $('ending').hidden = false; $('ending').querySelector('.end-discoveries').textContent = `${story.found.length} von 3 Glühwürmchen entdeckt. Im Wald kannst du weiter nach ihnen suchen.`;
    follow(2050); update(); burst(); chime(true); say('Die Sternenpost ist angekommen! Oma freut sich, und im Baumhaus wird es hell. Das hast du geschafft.');
  }
  function burst() {
    if (reduceMotion) return;
    for (let i = 0; i < 35; i++) { const bit = document.createElement('i'); bit.className = 'burst'; bit.style.cssText = `left:${50 + Math.random() * 30}%;top:${20 + Math.random() * 40}%;--bx:${Math.random() * 250 - 125}px;--by:${Math.random() * 260 - 100}px;animation-delay:${Math.random() * .5}s`; $('game').append(bit); setTimeout(() => bit.remove(), 2200); }
  }
  function beginStory() {
    initAudio(); started = true; $('intro').hidden = true; update();
    if (story.phase === 'meeting') moveTo(470, 800, nila);
    else if (story.phase === 'complete') showEnding();
    else say(story.phase === 'river' ? 'Die Sternenpost wartet. Taro, der Frosch, ist am Bach.' : 'Du bist fast da! Bringe die Sternenpost zum Baumhaus.');
  };
  const journey = createLetterJourney({
    paintSprite, say, stopSpeech, chime, muted: () => muted,
    opened() { modal = true; $('intro').hidden = true; update(); },
    exited() { modal = false; started = false; $('intro').hidden = false; update(); },
    finished() { modal = false; beginStory(); },
    travel(index) { foxX = 450 + index * 180; foxY = 800 - Math.max(0, foxX - 1300) * .3; follow(); }
  });
  $('begin').onclick = () => { if (journey.done) beginStory(); else journey.open(); };
  $('rabbit').onclick = nila; $('frog').onclick = frog; $('grandma').onclick = () => { if (started && story.phase === 'complete' && !modal && !busy) showEnding(); };
  $('river-stop').onclick = () => { if (!started || modal || busy) return; if (story.phase === 'meeting') nila(); else moveTo(1120, 820, frog); };
  $('house-stop').onclick = () => { if (!started || modal || busy) return; if (['meeting', 'river'].includes(story.phase)) { toast('Taro hilft dir zuerst über den Bach.'); say('Taro hilft dir zuerst über den Bach.'); } else destination(); };
  $('next-place').onclick = destination;
  $('chapter').onclick = () => { if (!modal || journey.active) journey.open(true); };
  $('dialogue-next').onclick = closeDialogue; $('repeat-dialogue').onclick = () => say($('dialogue-text').textContent);
  $('puzzle-back').onclick = closePuzzle;
  $('puzzle-repeat').onclick = () => say($('puzzle-success').hidden ? ($('game').dataset.puzzle === 'river' ? riverInstruction : houseInstruction) : `${$('success-title').textContent} ${$('success-text').textContent}`);
  $('help').onclick = () => {
    if (busy) return; story.help(); save();
    if ($('game').dataset.puzzle === 'river') { $('feedback').textContent = 'Auf dem Zettel steht LIMO. Suche die Limo.'; say('Auf dem Zettel steht Limo. Suche das Bild mit der Limo.'); }
    else { const matches = 'OMA'.startsWith(picked.join('')), next = matches ? 'OMA'[picked.length] : undefined; $('feedback').textContent = `Oma: O – M – A. ${next ? 'Als Nächstes: ' + next : 'Lege die Buchstaben zurück und beginne mit O.'}`; say(matches ? 'Oma. Am Anfang hörst du o. Die Buchstaben stehen hier als Hilfe.' : 'Lege die Buchstaben zurück. Beginne mit o.'); document.querySelectorAll('.letter-tile').forEach(b => b.classList.toggle('hinted', b.dataset.letter === next)); }
  };
  $('undo-letter').onclick = () => { if (!busy && picked.length) { picked.pop(); renderLetters(); $('feedback').textContent = ''; } };
  $('success-next').onclick = () => { const action = successAction; successAction = null; if (action) { busy = false; action(); } };
  $('stay').onclick = () => { $('ending').hidden = true; modal = false; update(); say('Schau dich um. Vielleicht entdeckst du noch ein Glühwürmchen.'); };
  $('replay').onclick = () => { story.phase = 'meeting'; story.found = []; story.helped = { river: false, treehouse: false }; save(); location.reload(); };
  document.querySelectorAll('[data-secret]').forEach(b => b.onclick = () => {
    if (!started || modal || busy || !story.discover(+b.dataset.secret)) return;
    save(); update(); chime(); toast(`Ein Glühwürmchen! ${story.found.length} von 3 entdeckt.`);
    if (story.found.length === 3) { burst(); say('Du hast alle drei Glühwürmchen gefunden!'); }
  });
  $('sound').onclick = () => {
    muted = !muted; $('sound').setAttribute('aria-pressed', String(muted)); $('sound').setAttribute('aria-label', muted ? 'Ton einschalten' : 'Ton ausschalten');
    if (host) host.setMuted(muted); else localStorage.setItem('forest-muted', String(muted));
    if (muted) { journey.stopAudio(); stopSpeech(); audio?.suspend(); } else { initAudio(); if (lastSpeech) say(lastSpeech); }
  };
  $('grownups').onclick = () => { if (host) host.openParents(); else toast('Die Android-App bietet hier Sprachtest, Updates und einen geschützten Neustart.'); };
  window.forestBack = () => { if (journey.back()) return true; if (!$('dialogue').hidden) { nextDialogue = null; closeDialogue(); return true; } if (!$('puzzle').hidden) { if (busy) { const action = successAction; successAction = null; busy = false; if (action) action(); } else closePuzzle(); return true; } if (!$('ending').hidden) { $('stay').click(); return true; } return false; };
  window.forestPaused = value => { paused = value; if (value) { journey.pause(); stopSpeech(); audio?.suspend(); } else { lastFrame = 0; if (!muted) audio?.resume().catch(() => {}); } };
  document.addEventListener('visibilitychange', () => window.forestPaused(document.hidden));
  $('viewport').onpointerdown = e => { if (!started || modal || busy || e.target.closest('button')) return; pan = { x: e.clientX, last: e.clientX, y: e.clientY, camera, moved: false }; $('viewport').setPointerCapture(e.pointerId); };
  $('viewport').onpointermove = e => { if (!pan) return; if (Math.abs(e.clientX - pan.x) > 10) pan.moved = true; if (pan.moved) targetCamera = Math.max(0, Math.min(Math.max(0, 2500 - viewWidth), pan.camera - (e.clientX - pan.x) / scale)); };
  $('viewport').onpointerup = e => {
    if (!pan) return; const gesture = pan; pan = null;
    if (gesture.moved || modal || busy) return;
    const x = Math.max(250, Math.min(story.phase === 'meeting' ? 840 : story.phase === 'river' ? 1190 : 2200, e.clientX / scale + camera));
    const y = x < 1250 ? 800 : 800 - Math.max(0, x - 1300) * .3;
    moveTo(x, y);
  };
  $('viewport').onpointercancel = () => { pan = null; };
  document.addEventListener('keydown', e => { if (e.key === 'Escape') window.forestBack(); });
  window.addEventListener('resize', resize);
  function loaded(image, url) { return new Promise((resolve, reject) => { image.onload = resolve; image.onerror = () => reject(new Error(url)); image.src = url; }); }
  Promise.all([loaded(atlas, 'art/woodland_animals.webp'), loaded(pictures, 'art/learning_pictures.webp')]).then(() => {
    paintAll();
    for (let i = 0; i < 35; i++) { const mote = document.createElement('i'); mote.className = 'mote'; mote.style.cssText = `left:${Math.random() * 2500}px;top:${200 + Math.random() * 600}px;--duration:${5 + Math.random() * 6}s;--delay:-${Math.random() * 8}s`; $('particles').append(mote); }
    if (story.phase === 'river') { foxX = 900; foxY = 800; }
    if (['treehouse', 'complete'].includes(story.phase)) { foxX = 1900; foxY = 620; }
    if (journey.done && story.phase !== 'meeting') { $('begin').innerHTML = 'Abenteuer fortsetzen <span>➜</span>'; $('continue-note').hidden = false; }
    try { muted = host ? host.isMuted() : localStorage.getItem('forest-muted') === 'true'; } catch (_) { muted = false; }
    $('sound').setAttribute('aria-pressed', String(muted)); $('sound').setAttribute('aria-label', muted ? 'Ton einschalten' : 'Ton ausschalten');
    if (!journey.done) $('begin').innerHTML = 'Zum Buchstabenpfad <span>➜</span>';
    resize(); camera = targetCamera; update(); $('loading').hidden = true; $('game').dataset.ready = 'true'; requestAnimationFrame(frame);
  }).catch(() => { $('loading').textContent = 'Der Wald konnte nicht geladen werden. Bitte starte die App neu.'; });
})();
