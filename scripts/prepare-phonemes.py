#!/usr/bin/env python3
"""Extract isolated consonants from the documented reference samples; retain source attribution.
The source consonant demos contain vowels: these must not be used uncut in letter-sound games.
"""
import pathlib,subprocess,numpy as np,json,hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
SOUNDS=ROOT/'app/src/main/assets/sounds'
SOURCE=ROOT/'audio-originals';SOURCE.mkdir(exist_ok=True)
CUTS={'m':(1.265,1.465),'l':(.26,.40),'n':(.29,.44),'f':(.25,.46)}
records=json.loads((ROOT/'docs/audio-sources.json').read_text())
changes={}
for letter,(start,end) in CUTS.items():
 source=SOURCE/(letter+'.ogg')
 if not source.exists():source.write_bytes((SOUNDS/(letter+'.ogg')).read_bytes())
 raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-f','f32le','-ac','1','-ar','22050','-'])
 x=np.frombuffer(raw,dtype=np.float32)[int(start*22050):int(end*22050)].copy();x-=np.mean(x)
 fade=220;result=x.copy()
 while letter!='m' and len(result)<13230:
  mix=np.linspace(0,1,fade,dtype=np.float32);result=np.concatenate([result[:-fade],result[-fade:]*(1-mix)+x[:fade]*mix,x[fade:]])
 result=result[:13230];result*=min((.15 if letter=='m' else .16)/(np.sqrt(np.mean(result**2))+1e-8),.85/(np.max(np.abs(result))+1e-8))
 edge=110 if letter=='m' else 441
 result[:edge]*=np.linspace(0,1,edge);result[-edge:]*=np.linspace(1,0,edge)
 subprocess.run(['ffmpeg','-y','-v','error','-f','f32le','-ar','22050','-ac','1','-i','-','-c:a','libvorbis','-q:a','5',str(SOUNDS/(letter+'.ogg'))],input=result.astype('<f4').tobytes(),check=True)
 changes[letter]={'source_section_seconds':[start,end],'adaptation':('natural nasal segment without looping or time stretching; gain normalization; 5ms edge fades' if letter=='m' else 'isolated consonant segment repeated with crossfades to 0.6 seconds; gain normalization; 20ms fade in/out'),'sha256':hashlib.sha256((SOUNDS/(letter+'.ogg')).read_bytes()).hexdigest()}
(ROOT/'docs/audio-adaptations.json').write_text(json.dumps(changes,indent=2))
lines=['LAUTAUFNAHMEN · MIT NAMENSNENNUNG','', 'Lizenz der enthaltenen Lautaufnahmen: Creative Commons Namensnennung – Weitergabe unter gleichen Bedingungen 3.0 Unported (CC BY-SA 3.0).','https://creativecommons.org/licenses/by-sa/3.0/','Die Lautdateien und ihre Bearbeitungen stehen unter dieser Lizenz. Zeichnungen und Programmcode sind unabhängig davon.','']
for r in records:
 k=r['letter'];author='Peter Isotalo' if k in 'mlnf' else 'Denelson83'
 lines += [k.upper()+': '+r['file']+' — '+author,'https://commons.wikimedia.org/wiki/File:'+r['file'].replace(' ','_'), ('Bearbeitung: Natürliches Nasalsegment isoliert und normalisiert; keine Schleife, keine künstliche Verlängerung.' if k=='m' else 'Bearbeitung: Konsonant isoliert, verlängert und normalisiert.') if k in CUTS else 'Unveränderte Originalaufnahme.','']
(ROOT/'app/src/main/assets/AUDIO-CREDITS.txt').write_text('\n'.join(lines))
print('Prepared eight licensed phoneme assets; source and adaptation records retained.')
