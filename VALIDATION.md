# Verification and platform boundaries

Android and iOS share the public curriculum source: 198 faculty/department/cohort/program combinations, 59,326 course classification records and 226 exact science timetable records. iOS reference tests compare classifications against results exported by the Android implementation. Both provide semester credit forecasts, category graduation gaps, projected progress, school grades/GPA, separate manual grade calculations, curriculum selection, local supporting PDFs, week/month calendars, personal event editing, ICS export, course syllabi, notices, foreground sync and selected-only course registration with second confirmation.

Local checks cover same-owner records, cross-quarter deduplication, reused course codes, passed/failed/pending results, curriculum/cohort boundaries, unknown categories, science fallback, ICS escaping and manual GPA. Browser preview records are fictional and cannot contact the school. The macOS workflow runs iPhone XCUITest for calendar, registration selection and academic previews, then builds an unsigned ARM64 device IPA and a simulator app.

Platform-specific implementations differ: Android and iOS use their respective native login web views, system share sheets, PDF pickers and notification APIs. Android-specific OS widgets/services and optional app-email-account infrastructure are not portable APIs. No configured cloud email-account backend is included in either distribution; university authentication is performed on the school's own page.

No physical Apple device is available. Therefore identical behavior on real devices cannot be guaranteed: school SSO/MFA, session persistence, system notification delivery, PDF picking/sharing and real course submission still require device verification. No actual course registration is executed as a test. Simulator tests do not establish university-site compatibility or formal graduation eligibility.

Rules cover reviewed 2024–2026 curriculum cohorts; calendar data covers the reviewed 2026 academic year. Unknown dates, unrecognized categories and individual graduation conditions are not guessed. iOS classroom reads cover the currently available school calendar page. Academic forecasts assume pending courses are passed.

The IPA is unsigned and requires personal Apple signing; no signing key, provisioning profile, school account, real grades or device logs are distributed. Maintainer YIN explicitly authorized publishing snai2514@gmail.com as the contact address.


## 1.0.82 validation

Android release build and lint passed (0 lint errors). JVM suite: 526 tests, 0 failures/errors, 3 skips. iOS Node suite: 31 tests passed, including faculty/cohort checks and theme ZIP validation. Browser preview imported and applied the sample ZIP with a separately colored dialog. Android 1.0.82 was installed as an in-place upgrade on the maintainer device and cold-launched without a new AndroidRuntime crash. These checks do not establish live school registration or all professional graduation conditions. See CREDIT-RULE-AUDIT.md and THEME-PACKS.md.
