# Undergraduate curriculum adaptation

Scope: Ibaraki University, 2024–2026 curriculum cohorts; current student years 1–4. The user excluded 2023 entrants from this release.

## Selection and boundaries

- Student year stays the school's reported value. Curriculum cohort comes from `要件年月`, never inferred from student number/current grade. `入学年月日` is stored separately.
- Faculty/department/program and cohort select a rule. `学環` is recognized alongside `学部`.
- An explicitly chosen rule is persisted per SHA-256 school account identifier. It is labeled as self-selected and can be reset to school metadata. This does not modify the school profile.
- Unknown cohort/program shows pending requirements. The old Information Engineering classification and CAP/rule calculators are gated to the reviewed 2026 Information Engineering profile.
- New faculty summaries display verified minimums, not a complete automated graduation judgment. Specific required-course mappings, program completion, teacher licenses, thesis admission and individual recognition still require detailed adaptation and actual school category fixtures. Do not describe all departments as fully automatically audited.
- Other accounts' grade snapshots cannot use the current profile's rules.
- Evidence PDF import: native document picker, max 32 MiB, `%PDF-` header, atomic private no-backup storage, keyed by account + scope. No upload/email sending, and no automatic trust/activation of document content. The PDF is supporting material, not executable rules.

## Official sources reviewed (2026-09-16)

- Engineering: https://www.eng.ibaraki.ac.jp/education/class/ — 2024/2025/2026 PDFs, printed p6. General24, required71/elective21/free8 for four departments; urban civil54/38 vs architecture69/23. Individual program requirements remain relevant even when totals match.
- Science: https://www.sci.ibaraki.ac.jp/collegelife/curriculum/ — 2024/2025/2026 PDFs, graduation table. Program-dependent basic/standard/advanced/elective minimums; biology standard+advanced is a **merged46** cell, not standard46 plus advanced24. Geoscience technical program95 professional/free5 vs standard86/free14. Integrated/interdisciplinary programs currently show aggregate requirements only.
- Humanities: https://www.hum.ibaraki.ac.jp/reference/for-enrolled.html — 24/25/26requirements.pdf, respective department graduation tables. General22, professional75 or77, free27 or25; major/minor and level-specific restrictions not replaced by totals.
- Education: https://www.edu.ibaraki.ac.jp/students/zaigaku/ — 2024–2026, printed pp13/18. General22; education practice88/free14, subject A79/free23, B83/free19, special92/free10, nursing81/free21. Licenses/practicum conditions remain required.
- Agriculture: https://www.agr.ibaraki.ac.jp/teaching/course/ — 2024 original and 2025/2026 `freshman2.pdf` updates. General24; professional86 or regional coexistence90 from2025; free minimum0 does NOT mean total requirement110. Overall124 still applies; category minima may not sum to total. 2024 old names/tracks are retained.
- Collaborative Regional Innovation: https://www.mirai.ibaraki.ac.jp/students/ — all 2024/2025/2026 guides publicly retrievable via official Google links. General26/professional88/free10. Co-op/thesis options need separate checks.

Downloaded PDFs and text extracts are under build/curriculum/multi (not shipped in APK). Source URLs are available in the app. Corrected agriculture source links override older search-index PDFs.

## Login preview/layout

`IbarakiAccountActivity --ez previewLogin true` displays the login layout without clearing the user's profile or school session. Back/Return to calendar restores local access. Preview suppresses daily auto-sync while visible.

The title, central content and login button have been enlarged; button minimum72dp, title38sp, central heading30sp, CTA22sp. Support footer stays at bottom. Verified on Xiaomi1440x3200 Chinese. Existing registered devices keep Chinese; no independent app account introduced.

## Remaining verification

School-specific calendar dates and venue reading still have the older first-year engineering assumptions; full cross-campus calendar compatibility is not implied by curriculum selection. Other-program authentic school pages have not been tested. PDF evidence import is storage only; it does not parse arbitrary handbooks into verified rules.
