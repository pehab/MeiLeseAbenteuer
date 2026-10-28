#!/usr/bin/env python3
"""Fetch reference IPA recordings and preserve complete Commons licensing metadata."""
import json,subprocess,urllib.parse,pathlib,concurrent.futures
FILES={'m':'Bilabial nasal.ogg','a':'Open front unrounded vowel.ogg','o':'Close-mid back rounded vowel.ogg','l':'Alveolar lateral approximant.ogg','i':'Close front unrounded vowel.ogg','n':'Alveolar nasal.ogg','u':'Close back rounded vowel.ogg','f':'Voiceless labiodental fricative.ogg'}
ROOT=pathlib.Path(__file__).resolve().parents[1]
def get(item):
 k,f=item
 url='https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode({'action':'query','format':'json','prop':'imageinfo','iiprop':'url|extmetadata','titles':'File:'+f})
 j=json.loads(subprocess.check_output(['curl','-fsSL','--max-time','40',url]));i=next(iter(j['query']['pages'].values()))['imageinfo'][0]
 dest=ROOT/'app/src/main/assets/sounds'/f'{k}.ogg'
 subprocess.run(['curl','-fsSL','--max-time','40',i['url'].split('?')[0],'-o',str(dest)],check=True)
 return dict(letter=k,file=f,**i)
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool: records=list(pool.map(get,FILES.items()))
(ROOT/'docs/audio-sources.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
for r in records:print(r['letter'],r['extmetadata'].get('ImageDescription',{}).get('value','')[:130])
