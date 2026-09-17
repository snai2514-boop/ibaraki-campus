# Public source data

The iOS app carries reviewed Android 2026 calendar rules and 1,951 public course offering rules. These are institutional catalog data, not a student's timetable.

- Engineering: https://www.eng.ibaraki.ac.jp/common/education/class/2026-subject05.pdf
- Education: https://www.ibaraki.ac.jp/m/uploads/2026/05/r8_04_edu_subject_s.pdf
- Humanities: https://www.hum.ibaraki.ac.jp/pdf/lessonplan.pdf
- Agriculture: https://www.agr.ibaraki.ac.jp/assets/images/summary/pdf/course/2026Class_Schedule_School_JP0427.pdf
- Mirai: https://sites.google.com/view/r6ichiran

`calendar-data.js` is exported from reviewed Android source by `tools/import_calendar.py` when both workspaces are present. Standalone builds consume the committed export.

School adapters and regression fixtures derive from Android 1.0.80. Preview fixtures use fictional DEMO0000 and DEMO/ZZ course codes. No captured school screen, portal response, cookie, device identifier, real grade or student profile is included.
