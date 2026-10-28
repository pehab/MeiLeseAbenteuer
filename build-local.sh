#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
BT="$SDK/build-tools/36.0.0"
APP_VERSION="$(sed -n 's/^versionName=//p' version.properties)"
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated .signing
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
python3 - <<'PY'
from pathlib import Path
v=dict(line.split('=',1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
s=Path('app/src/main/AndroidManifest.xml').read_text().replace('<manifest ','<manifest package="de.meiapps.leseabenteuer" android:versionCode="'+v['versionCode']+'" android:versionName="'+v['versionName']+'" ',1)
s=s.replace('<queries>','<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="37"/><queries>',1)
Path('build/AndroidManifest.xml').write_text(s)
PY
"$BT/aapt2" link -I "$SDK/platforms/android-37.0/android.jar" --manifest build/AndroidManifest.xml --java build/generated -A app/src/main/assets -o build/resources.apk build/resources.zip
java -m jdk.compiler/com.sun.tools.javac.Main -Xlint:deprecation -Werror -encoding UTF-8 -source 17 -target 17 -classpath "$SDK/platforms/android-37.0/android.jar" -d build/classes $(find app/src/main/java build/generated -name '*.java')
python3 - <<'PY'
from pathlib import Path
import zipfile
with zipfile.ZipFile('build/classes.jar','w') as z:
 for p in Path('build/classes').rglob('*.class'):z.write(p,p.relative_to('build/classes'))
PY
"$BT/d8" --lib "$SDK/platforms/android-37.0/android.jar" --min-api 26 --output build/dex build/classes.jar
cp build/resources.apk build/unsigned.apk
python3 - <<'PY'
import zipfile
from pathlib import Path
with zipfile.ZipFile('build/unsigned.apk','a') as z:
 for p in Path('build/dex').glob('*.dex'):z.write(p,p.name)
PY
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f .signing/keystore.p12 ]; then
 python3 -c 'import secrets,pathlib;pathlib.Path(".signing/password").write_text(secrets.token_hex(24))'
 "${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}/bin/keytool" -genkeypair -keystore .signing/keystore.p12 -storetype PKCS12 -storepass:file .signing/password -alias leseabenteuer -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=MeiLeseAbenteuer, OU=meiApps, C=DE" >/dev/null
 chmod 600 .signing/*
fi
"$BT/apksigner" sign --ks .signing/keystore.p12 --ks-key-alias leseabenteuer --ks-pass file:.signing/password --out "build/MeiLeseAbenteuer-$APP_VERSION.apk" build/aligned.apk
"$BT/apksigner" verify --verbose "build/MeiLeseAbenteuer-$APP_VERSION.apk"
