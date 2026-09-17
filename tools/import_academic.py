"""Import only public Android curriculum exports and published course catalogs."""
from pathlib import Path
import json, re
ios=Path(__file__).resolve().parents[1]
root=ios.parent if (ios.parent/'app').exists() else ios/'android'
web=ios/'CampusAssistant/Web'
data=json.loads((ios/'build/curricula.json').read_text(encoding='utf-8'))
source=root/'app/src/main/java/com/tyust/course/academic'
data['science']=[line.split('|') for line in (source/'SchoolScienceOfferings2026.kt').read_text(encoding='utf-8').splitlines() if re.fullmatch(r'[^|]+\|[0-9.]+\|[1-6]\|[1-6]\|(?:[1-4]Q|前期|後期)',line)]
catalog=(root/'app/src/main/assets/course-categories.tsv').read_text(encoding='utf-8')
# A compact local table, with repeated URLs and labels interned. No network fetch.
strings=[]; indexes={}
def intern(s):
    if s not in indexes: indexes[s]=len(strings); strings.append(s)
    return indexes[s]
rows=[[intern(c) for c in line.split('\t')] for line in catalog.splitlines() if line and not line.startswith('#')]
data['catalog']={'strings':strings,'rows':rows}
languages=json.loads((root/'app/src/main/assets/languages.json').read_text(encoding='utf-8'))
(web/'academic-data.js').write_text('/* Public curriculum rules exported from Android; no student data. */\nglobalThis.CampusAcademicData='+json.dumps(data,ensure_ascii=False,separators=(',',':'))+';\n',encoding='utf-8')
(web/'languages.js').write_text('globalThis.CampusLanguages='+json.dumps(languages,ensure_ascii=False,separators=(',',':'))+';\n',encoding='utf-8')
print(f'Exported {len(data["curricula"])} curriculum scopes, {len(rows)} public course classifications, {len(data["science"])} science offerings')
