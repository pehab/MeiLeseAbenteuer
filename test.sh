#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
node --check app/src/main/assets/forest/forest.js
node --check app/src/main/assets/forest/letters.js
node --check app/src/main/assets/forest/lessons-ui.js
node --check app/src/main/assets/forest/lanterns.js
node --check app/src/main/assets/forest/lanterns-ui.js
node --test tests/story.test.cjs tests/letters.test.cjs tests/lanterns.test.cjs
