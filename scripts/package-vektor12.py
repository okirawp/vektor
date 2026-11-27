#!/usr/bin/env python3
from pathlib import Path
import zipfile,shutil,hashlib,base64,html
root=Path(__file__).resolve().parents[1];out=Path('/workspace/library-files/vektor-v120');out.mkdir(exist_ok=True,parents=True)
for n in ['Vektor-1.2.0.apk','Vektor-1.2.0.aab']:shutil.copy2(root/'dist'/n,out/n)
for n in ['README.md','SECURITY.md','TEST-REPORT.md','THIRD-PARTY.md']:shutil.copy2(root/n,out/n)
with zipfile.ZipFile(out/'Vektor-1.2.0-source.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in root.rglob('*'):
  rel=p.relative_to(root)
  if not p.is_file() or any(a in {'.gradle','build','dist','out','__pycache__','task-out','task-dex','.git'} for a in rel.parts):continue
  if p.suffix in {'.log','.apk','.aab','.jar','.class','.pyc','.idsig','.img','.qcow2','.rota','.dex','.so'} and str(rel)!='gradle/wrapper/gradle-wrapper.jar':continue
  if p.name=='local.properties' or p.read_bytes()[:4]==b'\x7fELF':continue
  if p.suffix in {'.p12','.jks','.keystore','.pass','.lineage'}:raise RuntimeError('Private file')
  z.write(p,Path('Vektor-1.2.0')/rel)
with zipfile.ZipFile(out/'Vektor-1.2.0-validation.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in (root/'dist').glob('vektor12-*.log'):z.write(p,p.name)
 z.write(root/'app/build/test-results/testReleaseUnitTest/TEST-dev.kolmapper.ExperienceTest.xml','experience-tests.xml')
 for p in (root/'dist/screenshots').glob('vektor12-*.png'):
  if p.stat().st_size:z.write(p,Path('screenshots')/p.name)
 z.write(root/'TEST-REPORT.md','TEST-REPORT.md')
with zipfile.ZipFile(out/'Vektor-1.2.0-Play-preparation.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in (root/'play').rglob('*'):
  if p.is_file():z.write(p,p.relative_to(root))
 z.write(root/'THIRD-PARTY.md','THIRD-PARTY.md')
figs=[]
for name,label in [('main','Koyu tema'),('light','Açık tema'),('exit','Çıkış onayı'),('help','Adım adım rehber'),('privacy','Gizlilik'),('editor','Düzenleyici'),('game-guide','Oyun içi rehber')]:
 p=root/f'dist/screenshots/vektor12-{name}.png'
 if p.exists() and p.stat().st_size:
  figs.append('<figure><figcaption>'+label+'</figcaption><img src="data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode()+'"></figure>')
(out/'Vektor-1.2.0-gorunum.html').write_text('<!doctype html><meta charset="utf-8"><title>Vektör 1.2.0</title><style>body{background:#0f131b;color:#f0f3f9;font:16px system-ui;margin:32px}figure{margin:24px 0}img{max-width:100%;max-height:800px}figcaption{margin:16px 0}</style><h1>Vektör 1.2.0</h1><p>Android 10 emülatöründen değiştirilmemiş ekran görüntüleri.</p>'+''.join(figs))
with (out/'SHA256SUMS.txt').open('w') as f:
 for p in sorted(out.iterdir()):
  if p.is_file() and p.name!='SHA256SUMS.txt':f.write(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n')
for p in out.glob('*.zip'):
 with zipfile.ZipFile(p) as z:assert z.testzip() is None
print('PASS delivery archive integrity:',out)
