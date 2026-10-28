#!/usr/bin/env bash
set -euo pipefail
APP=de.meiapps.leseabenteuer
VERSION="$(sed -n 's/^versionName=//p' version.properties)"
SDK="${ANDROID_SDK_ROOT:?Set SDK}"
"$SDK/build-tools/36.0.0/apksigner" sign --ks .signing/keystore.p12 --ks-key-alias leseabenteuer --ks-pass file:.signing/password --out build/upgrade-fixture.apk dist/MeiLeseAbenteuer-0.1.0.apk
adb install -r build/upgrade-fixture.apk
adb install -r -t build/instrumentation.apk
adb shell pm clear "$APP"
adb shell am instrument -w -e phase seed "$APP.tests/de.meiapps.leseabenteuer.SmokeInstrumentation" > build/android-upgrade-seed.log
grep -q 'PASS:' build/android-upgrade-seed.log
adb shell am force-stop "$APP"
adb install -r "build/MeiLeseAbenteuer-$VERSION.apk"
adb shell am instrument -w -e phase upgrade "$APP.tests/de.meiapps.leseabenteuer.SmokeInstrumentation" > build/android-upgrade.log
grep -q 'PASS:' build/android-upgrade.log
adb shell pm clear "$APP"
adb shell svc wifi disable
adb shell svc data disable
adb shell am instrument -w -e phase play "$APP.tests/de.meiapps.leseabenteuer.SmokeInstrumentation" > build/android-play.log
cat build/android-play.log
if ! grep -Eq 'PASS:' build/android-play.log || grep -Eq 'error=|INSTRUMENTATION_FAILED|Process crashed' build/android-play.log; then exit 1; fi
python3 - <<'PYIMAGES'
import re,base64,pathlib
text=pathlib.Path('build/android-play.log').read_text()
for name,data in re.findall(r'INSTRUMENTATION_STATUS: image_([a-z0-9.-]+\.png)=([A-Za-z0-9+/=]+)',text):
 pathlib.Path('build',name).write_bytes(base64.b64decode(data,validate=True))
assert pathlib.Path('build/home.png').is_file(), 'Missing home screenshot'
PYIMAGES
adb shell am force-stop "$APP"
adb shell am instrument -w -e phase reopen "$APP.tests/de.meiapps.leseabenteuer.SmokeInstrumentation" > build/android-reopen.log
cat build/android-reopen.log
if ! grep -Eq 'PASS:' build/android-reopen.log || grep -Eq 'error=|INSTRUMENTATION_FAILED|Process crashed' build/android-reopen.log; then exit 1; fi
