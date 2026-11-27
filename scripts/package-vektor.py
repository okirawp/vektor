#!/usr/bin/env python3
"""Local Vektör delivery and self-contained native screenshot review; never publishes."""
from pathlib import Path
import zipfile,shutil,hashlib,base64,html
root=Path(__file__).resolve().parents[1];out=Path('/workspace/library-files/vektor-v110');out.mkdir(parents=True,exist_ok=True)
for name in ['Vektor-1.1.0.apk','Vektor-1.1.0.aab']:shutil.copy2(root/'dist'/name,out/name)
for name in ['README.md','SECURITY.md','TEST-REPORT.md']:shutil.copy2(root/name,out/name)
def data_url(path):return 'data:image/png;base64,'+base64.b64encode(path.read_bytes()).decode()
views=[('Tablet · koyu','tablet-dark'),('Tablet · açık','tablet-light'),('Telefon','phone-dark'),('Büyük yazı · %130','phone-large-text'),('Dar yatay ekran','compact-landscape'),('Oyun seçimi','picker'),('Oyun içi ikon','playing'),('Düzenleyici','editor'),('Tekrar ayarları','settings'),('Basılı tut ayarları','settings-hold')]
figures=[]
for label,name in views:
 path=root/f'dist/screenshots/vektor-final-{name}.png';assert path.exists(),path
 figures.append('<figure><figcaption>'+html.escape(label)+'</figcaption><img loading="lazy" alt="'+html.escape(label)+'" src="'+data_url(path)+'"></figure>')
review='''<!doctype html><html lang="tr"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Vektör — Yeni görünüm</title><style>*{box-sizing:border-box}body{margin:0;padding:40px 24px 64px;background:#0f131b;color:#f0f3f9;font:15px/1.65 system-ui,sans-serif}main{max-width:1280px;margin:auto}header{display:flex;align-items:center;gap:18px;margin-bottom:20px}header img{width:68px;height:68px;border-radius:18px}h1{font-size:32px;letter-spacing:-1px;margin:0}p{color:#9aa8bc;max-width:820px}section{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,420px),1fr));gap:24px;margin-top:32px}figure{margin:0;border:1px solid #2a3445;border-radius:18px;padding:16px;background:#181f2b}figcaption{margin-bottom:14px;font-weight:600}figure img{display:block;max-width:100%;max-height:730px;width:auto;height:auto;margin:auto;border-radius:8px}.wide{grid-column:1/-1}</style><main><header><img alt="Vektör logosu" src="'''+data_url(root/'assets/vektor/icon-512.png')+'''"><div><h1>Vektör</h1><span>Yeni kimlik · 1.1.0</span></div></header><p>Rota’nın yeni görünümü. Bu sayfadaki uygulama ekranları Android 10 emülatöründen alındı; çizilmiş arayüz maketleri değildir. Touch Probe, kontrolleri denemek için kullanılan test oyunudur. Mevcut oyunlarının atamaları güncellemede korunur.</p><p>İki tema, tek sayfalık oyun kütüphanesi, kayıtlı düzen kartları, nötr oyun ikonu ve içeriğe göre boyutlanan kontrol ayarları. Çalışan eşleme motoru ve .rota yedek biçimi korunmuştur.</p><section>'''+''.join(figures)+'''</section></main></html>'''
(out/'Vektor-1.1.0-gorunum.html').write_text(review)
# Native screenshots are copied unchanged, never retouched or composited.
for source,target in [('tablet-dark','screenshot-tablet-dark.png'),('tablet-light','screenshot-tablet-light.png'),('phone-dark','screenshot-phone-dark.png'),('phone-large-text','screenshot-phone-large-text.png'),('picker','screenshot-library.png'),('editor','screenshot-editor.png'),('settings','screenshot-settings.png')]:shutil.copy2(root/f'dist/screenshots/vektor-final-{source}.png',root/'play/graphics'/target)
with zipfile.ZipFile(out/'Vektor-1.1.0-source.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in root.rglob('*'):
  rel=p.relative_to(root)
  if not p.is_file() or any(x in {'.gradle','build','dist','out','__pycache__','task-out','task-dex','.git'} for x in rel.parts):continue
  if p.suffix in {'.log','.apk','.aab','.jar','.class','.pyc','.idsig','.img','.qcow2','.rota','.dex','.so'} and str(rel)!='gradle/wrapper/gradle-wrapper.jar':continue
  if p.name=='local.properties' or p.read_bytes()[:4]==b'\x7fELF':continue
  if p.suffix in {'.p12','.jks','.keystore','.pass','.lineage'}:raise RuntimeError('Private signing material must not be shipped')
  z.write(p,Path('Vektor-1.1.0')/rel)
logs=['unit-results.log','contrast-results.log','preservation-results.log','api29-storage-results.log','manual-ui-results.log','modal-results.log','build.log','apk-signature.log','bundle-validation.log','aab-signature.log','final-apk-sha.txt','native-tested-apk-sha.txt','apk-metadata.txt']
with zipfile.ZipFile(out/'Vektor-1.1.0-Play-hazirlik.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in (root/'play').rglob('*'):
  if p.is_file():z.write(p,Path('play')/p.relative_to(root/'play'))
 for suffix in logs:
  p=root/'dist'/('vektor11-'+suffix);assert p.exists(),p;z.write(p,Path('verification')/p.name)
 for name in ['README.md','SECURITY.md','TEST-REPORT.md']:z.write(root/name,name)
 z.write(root/'docs/VEKTOR-DESIGN.md','VEKTOR-DESIGN.md')
with (out/'SHA256SUMS.txt').open('w') as f:
 for p in sorted(out.iterdir()):
  if p.is_file() and p.name!='SHA256SUMS.txt':f.write(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n')
for p in out.glob('*.zip'):
 with zipfile.ZipFile(p) as z:assert z.testzip() is None
for line in (out/'SHA256SUMS.txt').read_text().splitlines():
 digest,name=line.split('  ',1);assert hashlib.sha256((out/name).read_bytes()).hexdigest()==digest
print(out)
print('PASS archive integrity and delivery checksums')
