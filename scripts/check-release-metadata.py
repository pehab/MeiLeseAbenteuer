#!/usr/bin/env python3
"""Check the actual checked-in feed against the APK offered to installed clients."""
import hashlib,json,pathlib,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
v=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line)
m=json.loads((root/'dist/MeiLeseAbenteuer-update.json').read_text())
apk=root/'dist'/('MeiLeseAbenteuer-'+v['versionName']+'.apk')
assert m['schema']==1 and m['versionCode']==int(v['versionCode']) and m['versionName']==v['versionName']
assert m['url']=='https://github.com/pehab/MeiLeseAbenteuer/releases/download/v'+v['versionName']+'/'+apk.name
assert m['size']==apk.stat().st_size and m['sha256']==hashlib.sha256(apk.read_bytes()).hexdigest()
with zipfile.ZipFile(apk) as z:
 for sound in 'maolinuf':
  info=z.getinfo('assets/sounds/'+sound+'.ogg');assert info.compress_type==0;assert z.read(info).startswith(b'OggS')
 assert z.read('assets/AUDIO-CREDITS.txt')
print('PASS: published repository URL, version, APK checksum and offline audio assets')
