/* Letter lessons and ordered, corridor-based handwriting checks. No network or font dependencies. */
(function (root) {
  'use strict';
  function curve(points) { return points; }
  function oval(cx, cy, rx, ry, start = -Math.PI / 2, end = -Math.PI * 2.5) {
    return Array.from({ length: 49 }, (_, i) => [cx + Math.cos(start + (end - start) * i / 48) * rx, cy + Math.sin(start + (end - start) * i / 48) * ry]);
  }
  const letters = [
    { upper: 'A', lower: 'a', title: 'Das Tor zur Lichtung', animal: 1,
      story: 'Nila hat ein Tor aus Zweigen gebaut. Zwei schräge Äste und ein kleiner Querast: Hier wartet dein erster Buchstabe.',
      reward: 'Das Tor leuchtet. Der erste Buchstabe begleitet dich!',
      upperStrokes: [[[65, 195], [150, 30], [235, 195]], [[105, 120], [195, 120]]],
      lowerStrokes: [oval(140, 140, 55, 55, 0, -Math.PI * 2), [[195, 85], [195, 195]]] },
    { upper: 'E', lower: 'e', title: 'Drei Äste für Nila', animal: 1,
      story: 'Am Tor fehlen drei kleine Äste. Nila sammelt sie mit dir. Aus einem langen und drei kurzen Ästen entsteht ein neuer Buchstabe.',
      reward: 'Die Äste sitzen fest. Zwei Buchstaben leuchten am Tor.',
      upperStrokes: [[[205, 30], [90, 30], [90, 195], [205, 195]], [[90, 110], [185, 110]]],
      lowerStrokes: [curve([[90, 140], [200, 140], [197, 117], [184, 96], [162, 86], [137, 86], [114, 97], [95, 118], [90, 145], [98, 172], [119, 190], [148, 195], [177, 190], [195, 177]])] },
    { upper: 'I', lower: 'i', title: 'Das Glühwürmchen', animal: 3,
      story: 'Ein Glühwürmchen sucht seinen Platz über einem kleinen Zweig. Hilfst du ihm? Der große Buchstabe steht lang, der kleine trägt einen Punkt.',
      reward: 'Das Glühwürmchen hat seinen Platz gefunden. Es zeigt dir den Weg.',
      upperStrokes: [[[150, 30], [150, 195]]],
      lowerStrokes: [[[150, 90], [150, 195]], [[150, 48], [151, 48]]] },
    { upper: 'O', lower: 'o', title: 'Kreise auf dem Wasser', animal: 5,
      story: 'Taro lässt einen Tropfen in den Bach fallen. Auf dem Wasser wachsen runde Ringe. So rund sieht der nächste Buchstabe aus.',
      reward: 'Ein heller Ring tanzt auf dem Bach. Taro quakt vor Freude.',
      upperStrokes: [oval(150, 112, 72, 82)], lowerStrokes: [oval(150, 140, 55, 55)] },
    { upper: 'U', lower: 'u', title: 'Taros kleines Boot', animal: 5,
      story: 'Taro baut ein Boot. Erst geht es nach unten, dann rund um den Boden und wieder nach oben. Fahre die Form seines Bootes nach.',
      reward: 'Das kleine Boot schwimmt. Bald hilft es der Sternenpost über den Bach.',
      upperStrokes: [[[80, 30], [80, 140], [85, 166], [99, 185], [122, 195], [150, 198], [178, 195], [201, 185], [215, 166], [220, 140], [220, 30]]],
      lowerStrokes: [[[95, 85], [95, 155], [100, 177], [118, 192], [140, 195], [164, 188], [185, 169], [195, 145], [195, 85]], [[195, 85], [195, 195]]] },
    { upper: 'M', lower: 'm', title: 'Zwei kleine Waldberge', animal: 0,
      story: 'Der Fuchs entdeckt zwei Spitzen und zwei kleine Hügel. Ein Buchstabe kann groß und klein ganz verschieden aussehen. Schau genau hin.',
      reward: 'Auf beiden Hügeln leuchtet ein Stern. Der Fuchs nimmt deinen Buchstaben mit.',
      upperStrokes: [[[65, 195], [65, 30], [150, 140], [235, 30], [235, 195]]],
      lowerStrokes: [[[60, 90], [60, 195]], [[60, 130], [72, 105], [90, 88], [111, 88], [130, 101], [140, 126], [140, 195]], [[140, 130], [153, 103], [174, 88], [196, 88], [217, 103], [225, 128], [225, 195]]] },
    { upper: 'N', lower: 'n', title: 'Eine Brücke im Moos', animal: 4,
      story: 'Im Moos fehlt eine kleine Brücke. Der große Buchstabe hat einen schrägen Weg, der kleine einen runden Bogen. Baue beide mit deinem Finger.',
      reward: 'Die Moosbrücke ist fertig. Jetzt kommt jeder kleine Waldbewohner hinüber.',
      upperStrokes: [[[80, 195], [80, 30], [220, 195], [220, 30]]],
      lowerStrokes: [[[95, 90], [95, 195]], [[95, 135], [110, 105], [135, 88], [160, 88], [186, 102], [200, 130], [200, 195]]] },
    { upper: 'L', lower: 'l', title: 'Ein Licht für die Post', animal: 1,
      story: 'Nila stellt eine Laterne an den Weg. Ein langer Stab und ein Fuß: Den großen Buchstaben findest du hier. Der kleine ist ein langer Strich.',
      reward: 'Alle acht Buchstaben leuchten! Nila hat jetzt eine besondere Nachricht für dich.',
      upperStrokes: [[[95, 30], [95, 195], [215, 195]]], lowerStrokes: [[[150, 30], [150, 195]]] }
  ];
  const steps = ['meet', 'find', 'pair', 'writeUpper', 'writeLower', 'reward'];
  class Lessons {
    constructor(saved) {
      if (saved?.version !== 1) saved = null;
      this.completed = Number.isInteger(saved?.completed) && saved.completed >= 0 && saved.completed <= letters.length ? saved.completed : 0;
      this.index = Number.isInteger(saved?.index) && saved.index >= 0 && saved.index <= Math.min(this.completed, letters.length - 1) ? saved.index : Math.min(this.completed, letters.length - 1);
      this.step = steps.includes(saved?.step) ? saved.step : 'meet';
      // A reward can only be restored for an already completed chapter.
      if (this.step === 'reward' && this.index >= this.completed) this.step = 'meet';
    }
    get letter() { return letters[this.index]; }
    get done() { return this.completed === letters.length; }
    choose(index) {
      if (!Number.isInteger(index) || index < 0 || index > Math.min(this.completed, letters.length - 1)) return false;
      this.index = index; this.step = 'meet'; return true;
    }
    advance(answer) {
      const expected = { meet: 'start', find: this.letter.upper, pair: this.letter.lower, writeUpper: 'traced', writeLower: 'traced' };
      if (expected[this.step] !== answer || this.step === 'reward') return false;
      this.step = steps[steps.indexOf(this.step) + 1];
      if (this.step === 'reward') this.completed = Math.max(this.completed, this.index + 1);
      return true;
    }
    save() { return JSON.stringify({ version: 1, completed: this.completed, index: this.index, step: this.step }); }
  }
  function sample(points, spacing = 5) {
    const result = [points[0]];
    for (let i = 1; i < points.length; i++) {
      const a = points[i - 1], b = points[i], count = Math.max(1, Math.ceil(Math.hypot(b[0] - a[0], b[1] - a[1]) / spacing));
      for (let n = 1; n <= count; n++) result.push([a[0] + (b[0] - a[0]) * n / count, a[1] + (b[1] - a[1]) * n / count]);
    }
    return result;
  }
  class Trace {
    constructor(strokes) { this.strokes = strokes.map(p => sample(p)); this.stroke = 0; this.cursor = 0; this.down = false; this.last = null; }
    get done() { return this.stroke === this.strokes.length; }
    get target() { return this.done ? null : this.strokes[this.stroke][this.cursor]; }
    begin(x, y) {
      if (this.done) return false;
      this.cursor = 0;
      if (Math.hypot(x - this.target[0], y - this.target[1]) > 18) return false;
      this.down = true; this.last = [x, y]; return true;
    }
    move(x, y) {
      if (!this.down || this.done || !Number.isFinite(x) || !Number.isFinite(y)) return false;
      const points = this.strokes[this.stroke];
      const count = Math.max(1, Math.ceil(Math.hypot(x - this.last[0], y - this.last[1]) / 4));
      // Huge jumps and off-trail scribbles never finish a stroke.
      if (count > 150) { this.cancel(); return false; }
      const from = this.last;
      for (let n = 1; n <= count; n++) {
        const p = [from[0] + (x - from[0]) * n / count, from[1] + (y - from[1]) * n / count];
        const near = points.slice(Math.max(0, this.cursor - 3), Math.min(points.length, this.cursor + 5));
        if (!near.some(q => Math.hypot(p[0] - q[0], p[1] - q[1]) <= 21)) { this.cancel(); return false; }
        while (this.cursor < points.length - 1 && Math.hypot(p[0] - points[this.cursor + 1][0], p[1] - points[this.cursor + 1][1]) <= 10) this.cursor++;
      }
      this.last = [x, y]; return true;
    }
    end() {
      if (!this.down) return false;
      this.down = false;
      const points = this.strokes[this.stroke];
      if (this.cursor < points.length - 2 || Math.hypot(this.last[0] - points[points.length - 1][0], this.last[1] - points[points.length - 1][1]) > 18) { this.cursor = 0; return false; }
      this.stroke++; this.cursor = 0; return true;
    }
    cancel() { this.down = false; this.cursor = 0; this.last = null; }
  }
  const api = { letters, Lessons, Trace, sample };
  if (typeof module !== 'undefined') module.exports = api; else root.ForestLetters = api;
})(typeof window === 'undefined' ? globalThis : window);
