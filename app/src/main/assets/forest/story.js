/* The chapter's save format is separate from every previous learning prototype. */
(function (root) {
  'use strict';
  const phases = ['meeting', 'river', 'treehouse', 'complete'];
  class Story {
    constructor(saved) {
      if (saved?.version !== 1) saved = null;
      this.phase = phases.includes(saved?.phase) ? saved.phase : 'meeting';
      this.found = [...new Set((Array.isArray(saved?.found) ? saved.found : []).filter(n => [0, 1, 2].includes(n)))];
      this.helped = { river: !!saved?.helped?.river, treehouse: !!saved?.helped?.treehouse };
    }
    advance(event) {
      const expected = { meeting: 'accept', river: 'lemonade', treehouse: 'OMA' };
      if (expected[this.phase] !== event) return false;
      this.phase = phases[phases.indexOf(this.phase) + 1];
      return true;
    }
    discover(id) {
      if (![0, 1, 2].includes(id) || this.found.includes(id)) return false;
      this.found.push(id); return true;
    }
    help() { if (this.phase in this.helped) this.helped[this.phase] = true; }
    save() { return JSON.stringify({ version: 1, phase: this.phase, found: this.found, helped: this.helped }); }
  }
  function shuffled(items, random = Math.random) {
    const result = [...items];
    for (let i = result.length - 1; i > 0; i--) { const j = Math.floor(random() * (i + 1)); [result[i], result[j]] = [result[j], result[i]]; }
    return result;
  }
  const api = { Story, shuffled };
  if (typeof module !== 'undefined') module.exports = api;
  else root.ForestStory = api;
})(typeof window === 'undefined' ? globalThis : window);
