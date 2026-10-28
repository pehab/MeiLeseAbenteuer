#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
BT="$SDK/build-tools/36.0.0"
mkdir -p build/instrumentation-classes build/instrumentation-dex
"$BT/aapt2" link -I "$SDK/platforms/android-37.0/android.jar" --manifest tests/AndroidManifest.xml -o build/instrumentation-resources.apk
java -m jdk.compiler/com.sun.tools.javac.Main -Xlint:deprecation -Werror -encoding UTF-8 -source 17 -target 17 -classpath "$SDK/platforms/android-37.0/android.jar:build/classes" -d build/instrumentation-classes tests/SmokeInstrumentation.java
python3 - <<'PY'
import zipfile,pathlib
with zipfile.ZipFile('build/instrumentation.jar','w') as z:
 for p in pathlib.Path('build/instrumentation-classes').rglob('*.class'):z.write(p,p.relative_to('build/instrumentation-classes'))
PY
"$BT/d8" --lib "$SDK/platforms/android-37.0/android.jar" --classpath build/classes.jar --min-api 26 --output build/instrumentation-dex build/instrumentation.jar
cp build/instrumentation-resources.apk build/instrumentation-unsigned.apk
python3 - <<'PY'
import zipfile,pathlib
with zipfile.ZipFile('build/instrumentation-unsigned.apk','a') as z:
 for p in pathlib.Path('build/instrumentation-dex').glob('*.dex'):z.write(p,p.name)
PY
"$BT/zipalign" -f -p 4 build/instrumentation-unsigned.apk build/instrumentation-aligned.apk
"$BT/apksigner" sign --ks .signing/keystore.p12 --ks-key-alias leseabenteuer --ks-pass file:.signing/password --out build/instrumentation.apk build/instrumentation-aligned.apk
