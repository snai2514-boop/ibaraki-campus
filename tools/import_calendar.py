"""Import public reviewed rules, never application storage or student records."""
import json, re
from pathlib import Path
ios = Path(__file__).resolve().parents[1]
root = ios.parent if (ios.parent/'app').exists() else ios/'android'
src = root / 'app/src/main/java/com/tyust/course/academic'
calendar = (src / 'SchoolAcademicCalendar.kt').read_text(encoding='utf-8')
dates = re.findall(r'"((?:\d{2}-\d{2} ?){7})"', calendar.split('data class Semester')[0])
assert len(dates) == 20
offerings = {}
for line in (src / 'SchoolCourseOfferings2026.kt').read_text(encoding='utf-8').splitlines():
    if re.fullmatch(r'[A-Z0-9-]+\|[^|]+\|(MITO|HITACHI|AMI)\|(true|false)', line):
        code, term, campus, irregular = line.split('|')
        offerings[code] = dict(term=term, campus=campus, irregular=irregular == 'true')
assert len(offerings) > 100
out = ios / 'CampusAssistant/Web/calendar-data.js'
out.write_text('/* Public reviewed 2026 rules; see DATA-SOURCES.md. */\n' +
    'globalThis.CampusCalendarData = ' + json.dumps(dict(year=2026, dates=[dates[i:i+5] for i in range(0,20,5)], offerings=offerings), ensure_ascii=False) + ';\n', encoding='utf-8')
print(f'Imported {len(offerings)} public offerings and {len(dates)} calendar rows')
