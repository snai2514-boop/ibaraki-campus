"""Audit the exact source allowlist before upload or packaging. No device data read."""
from pathlib import Path
import re, json, hashlib, sys
root=Path(__file__).resolve().parents[1]
roots=['CampusAssistant','CampusAssistant.xcodeproj','Tests','tools','ci','.github','docs']
allowed=[]
for dirname in roots:
    allowed.extend(p for p in (root/dirname).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
allowed.extend(root/name for name in ['README.md','LICENSE','NOTICE.md','.gitignore','DATA-SOURCES.md','VALIDATION.md','THEME-PACKS.md','CREDIT-RULE-AUDIT.md','IOS-INSTALL.md','FEATURE-PREVIEW.md'] if (root/name).exists())
errors=[]
for path in allowed:
    rel=path.relative_to(root).as_posix()
    if any(s in path.parts for s in ['build','dist','xcuserdata','node_modules','.private']):errors.append(rel+' forbidden path')
    if path.suffix in ['.p12','.mobileprovision','.key','.pem','.log','.ipa']:errors.append(rel+' forbidden extension')
    if path.suffix=='.png':continue
    text=path.read_text(encoding='utf-8').replace('snai2514@gmail.com', '') # Maintainer explicitly authorized public contact.
    patterns=[r'gh[pousr]_[A-Za-z0-9]{20,}',r'github_pat_[A-Za-z0-9_]{20,}',r'-----BEGIN [A-Z ]*PRIVATE KEY-----',r'(?i)(?:C:|D:)[/\\](?:Users|ibaraki-campus-assistant)',r'(?i)(?:set-cookie|authorization):\s*\S+',r'(?i)[A-Z0-9._%+-]+@(?:gmail|outlook|hotmail|icloud)\.com']
    for pattern in patterns:
        if re.search(pattern,text):errors.append(rel+' potential private data pattern')
if errors:
    print('\n'.join(errors));sys.exit(1)
if '--manifest' in sys.argv:
    print(json.dumps([{'path':p.relative_to(root).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size} for p in sorted(allowed)],ensure_ascii=False))
elif '--payload' in sys.argv:
    import base64
    print(json.dumps([{'path':('.github/workflows/ios-build.yml' if p.relative_to(root).as_posix()=='ci/ios-build.yml' else p.relative_to(root).as_posix()),'content':base64.b64encode(p.read_bytes()).decode() if p.suffix=='.png' else p.read_text(encoding='utf-8'),'binary':p.suffix=='.png'} for p in sorted(allowed)],ensure_ascii=False))
else:print(f'PASS: {len(allowed)} allowlisted source files; no credentials, session data, private paths, or build/device records found.')
