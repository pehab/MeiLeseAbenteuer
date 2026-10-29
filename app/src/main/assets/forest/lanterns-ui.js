'use strict';
window.createLanternJourney = function (hooks) {
  const $ = id => document.getElementById(id), host = window.ForestHost;
  const { Lanterns, rounds } = ForestLanterns;
  let saved;
  try { saved = JSON.parse(host ? host.loadLanterns() : localStorage.getItem('forest-lanterns-1')); } catch (_) { saved = null; }
  const chapter = new Lanterns(saved);
  let opened = false, map = false, reviewing = false, solved = false, selected = [], current, phoneme, generation = 0;
  function stopAudio() { generation++; if (phoneme) { phoneme.pause(); phoneme.onended = null; phoneme.onerror = null; phoneme = null; } hooks.stopSpeech(); $('lantern-sounds').classList.remove('playing'); }
  function speak(text) { stopAudio(); hooks.say(text); }
  function persist() {
    try { if (host) host.saveLanterns(chapter.save()); else localStorage.setItem('forest-lanterns-1', chapter.save()); }
    catch (_) { $('lantern-feedback').textContent = 'Dein Fortschritt konnte nicht gespeichert werden.'; }
  }
  function sound(letter, after) {
    const token = generation;
    phoneme = new Audio('sounds/' + letter.toLowerCase() + '.ogg');
    phoneme.onended = () => { if (token === generation) after(); };
    phoneme.onerror = () => { if (token === generation) { stopAudio(); hooks.say('Der Laut konnte nicht abgespielt werden. Bitte prüfe die Stimme unter Für Große.'); } };
    phoneme.play().catch(error => { if (token === generation && error.name !== 'AbortError') { stopAudio(); hooks.say('Der Laut konnte nicht abgespielt werden.'); } });
  }
  function listen() {
    stopAudio(); if (hooks.muted() || solved || chapter.done) return;
    if (current.kind === 'word') { hooks.say(current.spoken); return; }
    if (current.kind === 'hear') { hooks.say(current.answer.toLowerCase()); return; }
    const token = generation;
    $('lantern-sounds').classList.add('playing');
    sound(current.answer[0], () => sound(current.answer[1], () => {
      if (token !== generation) return;
      $('lantern-sounds').classList.remove('playing'); hooks.say(current.answer.toLowerCase());
    }));
  }
  function lights() {
    $('lantern-lights').textContent = '';
    rounds.forEach((_, i) => { const light = document.createElement('span'); light.className = 'sound-lantern' + (i < chapter.completed ? ' lit' : ''); light.textContent = '✦'; light.setAttribute('aria-label', `Laterne ${i + 1}${i < chapter.completed ? ' leuchtet' : ' wartet'}`); $('lantern-lights').append(light); });
    $('lantern-count').textContent = `${chapter.completed} von ${rounds.length} Laternen leuchten`;
  }
  function check(answer) {
    if (solved || chapter.done) return;
    if (!chapter.answer(answer)) { $('lantern-feedback').textContent = 'Hör noch einmal hin. Du kannst die Bausteine zurücklegen oder dir helfen lassen.'; speak('Hör noch einmal hin. Du hast Zeit.'); return; }
    solved = true; stopAudio(); persist(); lights();
    $('lantern').classList.add('celebrating');
    $('lantern-feedback').textContent = `${current.spoken || current.answer} – eine weitere Laterne leuchtet!`;
    $('lantern-next').hidden = false; $('lantern-next').textContent = reviewing ? 'Zur Aufgabenauswahl ❧' : 'Zum nächsten Licht ➜'; $('lantern-help').disabled = true;
    $('lantern-listen').disabled = true; $('lantern-undo').disabled = true;
    $('lantern-options').querySelectorAll('button').forEach(b => { b.disabled = true; });
    hooks.chime(true); hooks.say(`${current.spoken || current.answer.toLowerCase()}. Deine Laterne leuchtet!`);
  }
  function wordSlots() {
    $('lantern-slots').textContent = '';
    current.solution.forEach((_, i) => { const slot = document.createElement('span'); slot.className = 'sound-slot'; slot.textContent = selected[i] === undefined ? '·' : current.pieces[selected[i]]; $('lantern-slots').append(slot); });
    $('lantern-undo').disabled = !selected.length;
  }
  function render() {
    stopAudio(); map = false; $('lantern-map').hidden = true; solved = false; selected = []; current = chapter.round;
    $('lantern').dataset.round = chapter.index;
    $('lantern').classList.remove('celebrating'); lights();
    $('lantern-feedback').textContent = '';
    $('lantern-next').hidden = true; $('lantern-help').disabled = false;
    $('lantern-listen').disabled = false;
    $('lantern-activity').hidden = chapter.done; $('lantern-finale').hidden = !chapter.done;
    if (chapter.done) { $('lantern-title').textContent = 'Ein Licht für den Waldweg'; speak('Alle Klanglaternen leuchten! Du hast Laute verbunden und Wörter gebaut. Nila zeigt dir den hellen Waldweg.'); return; }
    $('lantern-title').textContent = current.kind === 'word' ? 'Ein Wort für Nila' : current.kind === 'blend' ? 'Zwei Laute finden zusammen' : 'Welche Klanglaterne hörst du?';
    const instruction = current.kind === 'word' ? `Nila sucht ${current.spoken}. Höre das Wort und baue es aus den Bausteinen. Du kannst jeden Baustein antippen.` : current.kind === 'blend' ? 'Tippe auf Lautweg hören. Höre beide Laute und wie sie zusammen klingen. Finde die passende Klanglaterne.' : 'Tippe auf Noch einmal hören. Welche Klanglaterne hörst du?';
    $('lantern-instruction').textContent = instruction;
    $('lantern-sounds').textContent = '';
    if (current.kind === 'blend') current.answer.split('').forEach(letter => { const b = document.createElement('button'); b.className = 'sound-letter'; b.textContent = letter; b.setAttribute('aria-label', `Laut ${letter} hören`); b.onclick = () => { stopAudio(); if (!hooks.muted() && !solved) sound(letter, () => {}); }; $('lantern-sounds').append(b); });
    $('lantern-sounds').hidden = current.kind !== 'blend';
    $('lantern-slots').hidden = current.kind !== 'word'; $('lantern-undo').hidden = current.kind !== 'word';
    $('lantern-listen').textContent = current.kind === 'blend' ? '♫ Lautweg hören' : '♫ Noch einmal hören';
    $('lantern-options').textContent = '';
    const choices = current.kind === 'word' ? current.pieces.map((text, id) => ({ text, id })) : current.choices.map(text => ({ text }));
    ForestStory.shuffled(choices).forEach(({ text, id }) => {
      const b = document.createElement('button'); b.className = 'sound-choice'; b.textContent = text;
      b.dataset.sound = text; if (id !== undefined) b.dataset.piece = id;
      b.onclick = () => {
        if (solved) return;
        if (current.kind !== 'word') { check(text); return; }
        if (selected.includes(id) || selected.length >= current.solution.length) return;
        selected.push(id); b.disabled = true; wordSlots();
        if (selected.length === current.solution.length) check(selected.map(i => current.pieces[i]).join(''));
      };
      $('lantern-options').append(b);
    });
    if (current.kind === 'word') wordSlots();
    speak(instruction);
  }
  function showMap() {
    stopAudio(); map = true; $('lantern').classList.remove('celebrating');
    $('lantern-activity').hidden = true; $('lantern-finale').hidden = true; $('lantern-map').hidden = false;
    $('lantern-title').textContent = 'Deine Klanglaternen'; lights();
    $('lantern-map-options').textContent = '';
    rounds.forEach((round, index) => {
      const b = document.createElement('button'); b.className = 'lantern-map-choice'; b.dataset.lanternTask = index;
      b.disabled = index > chapter.completed;
      b.textContent = `${index + 1} · ${round.answer} ${index < chapter.completed ? '✦' : index === chapter.completed ? '➜' : '·'}`;
      b.setAttribute('aria-label', `${round.answer}${index < chapter.completed ? ', noch einmal üben' : index === chapter.completed ? ', als Nächstes' : ', noch geschlossen'}`);
      b.onclick = () => { reviewing = index < chapter.completed; if (chapter.choose(index)) { persist(); render(); } };
      $('lantern-map-options').append(b);
    });
    speak('Hier sind deine Klanglaternen. Tippe auf eine erledigte Aufgabe zum Üben oder auf die nächste offene Aufgabe. Deine leuchtenden Laternen bleiben erhalten.');
  }
  function close() { if (!opened) return false; stopAudio(); opened = false; $('lantern').hidden = true; hooks.closed(); return true; }
  $('lantern-back').onclick = close;
  $('lantern-listen').onclick = listen;
  $('lantern-help').onclick = () => {
    if (solved || chapter.done) return;
    const hint = current.kind === 'word' ? `${current.spoken}: ${current.solution.join(' + ')}. Lege die Bausteine in dieser Reihenfolge.` : `${current.answer[0]} + ${current.answer[1]} → ${current.answer}`;
    $('lantern-feedback').textContent = hint; speak(current.spoken || current.answer.toLowerCase());
  };
  $('lantern-undo').onclick = () => { if (solved || !selected.length) return; const id = selected.pop(); $('lantern-options').querySelector(`[data-piece="${id}"]`).disabled = false; $('lantern-feedback').textContent = ''; wordSlots(); };
  $('lantern-next').onclick = () => { if (reviewing) showMap(); else render(); };
  $('lantern-path').onclick = showMap;
  $('lantern-finish').onclick = close;
  $('lantern-replay').onclick = showMap;
  return {
    open() { opened = true; hooks.opened(); $('lantern').hidden = false; hooks.paintSprite($('lantern-nila'), 'animal', 1); if (chapter.completed === rounds.length) showMap(); else { reviewing = chapter.index < chapter.completed; render(); } },
    back: close, stopAudio, get active() { return opened; }
  };
};
