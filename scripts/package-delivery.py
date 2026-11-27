#!/usr/bin/env python3
"""Bundle the reviewed release and source without keys, build caches or test drivers."""
from pathlib import Path
import shutil,zipfile,hashlib
root=Path(__file__).resolve().parents[1]
out=Path('/workspace/library-files/rota-v05');out.mkdir(exist_ok=True)
files={'dist/Rota-0.5-final.apk':'Rota-0.5.apk','README.md':'README.md','SECURITY.md':'SECURITY.md','TEST-REPORT.md':'TEST-REPORT.md','dist/unit-v05-results.log':'unit-v05-results.log','dist/core-v05-results.log':'native-v05-results.log','dist/final-smoke-results.log':'final-smoke-results.log','dist/analog-direction-results.log':'analog-direction-results.log','dist/physical-orb-results.log':'physical-orb-results.log','dist/learning-flow-results.log':'learning-flow-results.log','dist/theme-results.log':'theme-results.log','dist/signature-v05.txt':'signature-v05.txt','verify-v05-final-release.log':'build.log','dist/mixed-input-trace.txt':'mixed-input-trace.txt'}
for required in ['dist/core-v05-results.log','dist/final-smoke-results.log','dist/analog-direction-results.log','dist/physical-orb-results.log','dist/learning-flow-results.log','dist/theme-results.log']:
 if 'ALL CHECKS PASSED' not in (root/required).read_text():raise RuntimeError('Required verification incomplete: '+required)
for src,dst in files.items():
 if (root/src).exists():shutil.copyfile(root/src,out/dst)
for name in ['rota-main-dark.png','rota-main-light.png']:
 p=root/'dist/screenshots'/name
 if p.exists():shutil.copyfile(p,out/name)
with zipfile.ZipFile(out/'Rota-0.5-source.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for path in root.rglob('*'):
  rel=path.relative_to(root)
  if not path.is_file() or any(p in {'.gradle','.git','build','dist','out','__pycache__'} for p in rel.parts):continue
  if path.suffix in {'.log','.apk','.idsig','.p12','.keystore','.jks','.pass','.pyc'} or path.name=='local.properties':continue
  archive.write(path,'rota/'+str(rel))
apk=out/'Rota-0.5.apk';(out/'SHA256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'  '+apk.name+'\n')
print(out)
