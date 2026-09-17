# Credit preview

- Select academic year and Q1–Q4 or first/second semester. Q1+Q2 form the first semester; Q3+Q4 form the second.
- Uses saved school timetable credit values. Exact course codes deduplicate repeated meetings and cross-quarter semester courses. Semester credits are assigned once at completion (Q2/Q4); duration is taken from explicit school tags or the existing verified 2026 engineering course mapping.
- Matches saved grades by normalized name, credits, year and completion quarter. Existing passes are not added again; final failures do not become predicted passes. Conflicting/unknown data is excluded and identified in course details.
- Shows saved earned credits + pending-course credits assuming all pending courses pass. This is separate from official graduation recognition and GPA.
- Empty future terms show waiting for school publication/sync, without invented courses or a claimed future total. Partial snapshots show a missing-quarter warning.
- Forecast does not mutate grades, courses or calendar records.

## Multi-department result matching and selectors

- Explicit school term tags take precedence. When a course has no term tag, a unique saved result with the same academic year, normalized course name and credits supplies the completion quarter (including 前期/後期). The existing verified code mapping is only a fallback.
- Matching covers all departments without a department whitelist. Ambiguous same-name results, conflicting credits and multiple course codes competing for one result remain excluded. Passed results never add credits twice; failures remain failures.
- Both credit preview entry points expose first/second semester choices; the detail card places both choices above individual quarter chips so the second semester is not hidden offscreen. The home preview supports academic-year changes, including during holidays.
- The Courses tab filters by a selectable academic year before applying quarter and search filters. Switching year does not fetch or invent school records.
- New regression coverage uses synthetic departmental and prior-year records. This does not establish real-device or all-department data coverage.

Matching now preserves the academic year and quarter from the saved official timetable page rather than discarding those fields before grouping. Course codes are grouped per semester and explicit quarter tag; results are filtered by the observed page quarters before resolving completion. First/second semester retakes and different academic years stay separate. Unknown first-semester records no longer appear in second-semester preview. Duplicate snapshots use the latest saved version. Same-period records that still cannot distinguish two course codes remain pending.

Device verification: 2026 first semester totals 26 scheduled credits, with 25 already earned and no ungraded additions; total preview remains 25. Selecting second semester displays missing Q3/Q4, waiting for school publication/sync and no future total. Five forecast unit tests passed; build and installation succeeded.
