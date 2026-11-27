#!/usr/bin/env python3
"""Package 1.0.2 locally; no publishing and no private signing material."""
from pathlib import Path
import zipfile,shutil,hashlib
root=Path(__file__).resolve().parents[1];out=Path('/workspace/library-files/rota-v102');out.mkdir(parents=True,exist_ok=True)
for name in ['Rota-1.0.2.apk','Rota-1.0.2.aab']:shutil.copy2(root/'dist'/name,out/name)
for name in ['README.md','SECURITY.md','TEST-REPORT.md']:shutil.copy2(root/name,out/name)
with zipfile.ZipFile(out/'Rota-1.0.2-source.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in root.rglob('*'):
  rel=p.relative_to(root)
  if not p.is_file() or any(x in {'.gradle','build','dist','out','__pycache__','task-out','task-dex','.git'} for x in rel.parts):continue
  if p.suffix in {'.log','.apk','.aab','.jar','.class','.pyc','.idsig'} and str(rel)!='gradle/wrapper/gradle-wrapper.jar':continue
  if p.name=='local.properties' or p.read_bytes()[:4]==b'\x7fELF':continue
  if p.suffix in {'.p12','.jks','.keystore','.pass','.lineage'}:raise RuntimeError('Private signing material must not be shipped')
  z.write(p,Path('Rota-1.0.2')/rel)
with zipfile.ZipFile(out/'Rota-1.0.2-Play-hazirlik.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in (root/'play').rglob('*'):
  if p.is_file():z.write(p,Path('play')/p.relative_to(root/'play'))
 for p in (root/'dist').glob('rota102-*'):
  if p.is_file() and (p.name.endswith('results.log') or p.name in {'rota102-build.log','rota102-apk-signature.log','rota102-bundle-validation.log','rota102-aab-signature.log','rota102-final-apk-sha.txt','rota102-native-tested-apk-sha.txt','rota102-apk-metadata.txt','rota102-signer-digest.txt','rota102-api29-touch-trace.txt'}):z.write(p,Path('verification')/p.name)
 for name in ['README.md','SECURITY.md','TEST-REPORT.md']:z.write(root/name,name)
with open(out/'SHA256SUMS.txt','w') as f:
 for p in sorted(out.iterdir()):
  if p.is_file() and p.name!='SHA256SUMS.txt':f.write(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n')
for p in out.glob('*.zip'):
 with zipfile.ZipFile(p) as z:assert z.testzip() is None
print(out)
