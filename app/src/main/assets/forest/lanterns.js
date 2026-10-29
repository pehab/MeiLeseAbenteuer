/* A small, independently saved reading chapter. Only familiar letters are used. */
(function (root) {
  'use strict';
  const rounds = [
    { kind: 'blend', answer: 'MA', choices: ['MA', 'MI', 'MU'] },
    { kind: 'blend', answer: 'MI', choices: ['MU', 'MA', 'MI'] },
    { kind: 'blend', answer: 'MU', choices: ['MI', 'MU', 'MA'] },
    { kind: 'hear', answer: 'LA', choices: ['LU', 'LA', 'LI'] },
    { kind: 'hear', answer: 'LI', choices: ['LA', 'LI', 'LU'] },
    { kind: 'hear', answer: 'LU', choices: ['LI', 'LA', 'LU'] },
    { kind: 'word', answer: 'MAMA', pieces: ['MA', 'MA', 'MI'], solution: ['MA', 'MA'], spoken: 'Mama' },
    { kind: 'word', answer: 'OMA', pieces: ['MA', 'O', 'MI'], solution: ['O', 'MA'], spoken: 'Oma' },
    { kind: 'word', answer: 'LIMO', pieces: ['MO', 'MA', 'LI'], solution: ['LI', 'MO'], spoken: 'Limo' }
  ];
  class Lanterns {
    constructor(saved) {
      this.completed = saved?.version === 1 && Number.isInteger(saved.completed) && saved.completed >= 0 && saved.completed <= rounds.length ? saved.completed : 0;
      this.index = saved?.version === 1 && Number.isInteger(saved.index) && saved.index >= 0 && saved.index <= this.completed ? saved.index : this.completed;
    }
    get done() { return this.index === rounds.length; }
    get round() { return rounds[this.index]; }
    choose(index) {
      if (!Number.isInteger(index) || index < 0 || index >= rounds.length || index > this.completed) return false;
      this.index = index; return true;
    }
    answer(value) {
      if (this.done || value !== this.round.answer) return false;
      this.completed = Math.max(this.completed, this.index + 1); this.index++; return true;
    }
    save() { return JSON.stringify({ version: 1, completed: this.completed, index: this.index }); }
  }
  const api = { Lanterns, rounds };
  if (typeof module !== 'undefined') module.exports = api; else root.ForestLanterns = api;
})(typeof window === 'undefined' ? globalThis : window);
