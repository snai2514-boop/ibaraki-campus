# Verification boundaries

Local Windows checks: JavaScript syntax, core logic tests, school-adapter regression fixtures, privacy allowlist audit and browser UI preview. Browser fixtures are fictional and never contact the school.

The macOS workflow builds an ARM64 iPhone/iPad app and unsigned IPA, a simulator app, and runs XCUITest for interface launch and calendar/settings navigation. Artifacts contain compiler logs, UI test results and a simulator screenshot. Actual execution results must be checked in GitHub Actions.

No real iPhone is available. School login/MFA, session persistence, notification delivery and real registration still need device verification. No actual course is registered as a test.

Initial features: login, sequential foreground sync, grades/GPA, all four quarter timetables, verified 2026 calendar, week/month navigation, teaching modes, currently displayed calendar classrooms, notices, personal period entries, course selection with second confirmation and uncertain-result protection.

Not yet ported: Android-specific email account login, full graduation-requirement classification, science-faculty fallback rules, calendar export, personal event editing/deletion and Android system integration. Unknown course dates remain explicitly unresolved. Classroom reads do not claim to cover every future month.

No Apple certificate or provisioning profile is included. The unsigned IPA cannot be installed directly; it needs signing for the user's device/account. Simulator success does not establish real-device school compatibility.
