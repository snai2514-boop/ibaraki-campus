"""Package distributable APK and corresponding source, never private workspace files."""
from pathlib import Path
import zipfile,hashlib,shutil
root=Path(__file__).resolve().parents[1]
version=next(line.split('=',1)[1].strip() for line in (root/'app/version.properties').read_text(encoding='utf-8').splitlines() if line.startswith('VERSION_NAME='))
out=root/'dist'/f'YIN-Ibaraki-{version}';out.mkdir(parents=True,exist_ok=True)
apk=out/f'教务助手-{version}.apk'
shutil.copy2(root/'app/build/outputs/apk/release/app-release.apk',apk)
for name in ['LICENSE','NOTICE.md']:shutil.copy2(root/name,out/name)
shutil.copy2(root/'docs/release.md',out/'INSTALL-AND-VERIFY.md')
paths=[]
for folder in ['app/src','gradle']:
 paths += [p for p in (root/folder).rglob('*') if p.is_file()]
for name in ['gradlew','gradlew.bat','settings.gradle','build.gradle','gradle.properties','app/build.gradle','app/version.properties','app/campus-proguard.pro','README.md','NOTICE.md','LICENSE','docs/release.md','scripts/build-yin-release.py','scripts/package-yin-release.py']:
 p=root/name
 if p.exists():paths.append(p)
paths += [root/'docs/course-registration.md', root/'scripts/test-school-registration.cjs', root/'scripts/test-school-course-modes.cjs', root/'scripts/test-school-syllabus.cjs']
with zipfile.ZipFile(out/f'YIN-Ibaraki-{version}-source.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(set(paths)):
  rel=p.relative_to(root).as_posix()
  assert not any(x in rel for x in ['.private','local.properties','school-imports-v1','school-student-profile-v1'])
  assert p.suffix not in ['.jks','.keystore','.p12']
  z.write(p,rel)
checks=[]
for p in sorted(out.iterdir()):
 if p.is_file() and p.name!='SHA256SUMS.txt':checks.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name)
(out/'SHA256SUMS.txt').write_text('\n'.join(checks)+'\n',encoding='utf-8')
print(out)
