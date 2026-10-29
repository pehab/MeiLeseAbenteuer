#!/usr/bin/env python3
"""A development branch must keep the previous public feed internally consistent."""
import hashlib,json,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
v=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line)
m=json.loads((root/'dist/MeiLeseAbenteuer-update.json').read_text())
apk=root/'dist'/('MeiLeseAbenteuer-'+m['versionName']+'.apk')
assert m['schema']==1 and 0 < m['versionCode'] <= int(v['versionCode'])
assert m['url']=='https://github.com/pehab/MeiLeseAbenteuer/releases/download/v'+m['versionName']+'/'+apk.name
assert m['size']==apk.stat().st_size and m['sha256']==hashlib.sha256(apk.read_bytes()).hexdigest()
print('PASS: existing public update feed still matches its published APK')
