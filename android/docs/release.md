# YIN release

Maintainer: YIN <snai2514@gmail.com>. License: GPL-3.0; see NOTICE.md.

## Build and keys
- JDK 17, Android SDK compile platform 37 and the repository Gradle wrapper.
- Run `python scripts/build-yin-release.py`. It creates a dedicated RSA release key once in `.private/` and reuses it for updates. Never delete, publish, commit, or send `.private/` to recipients. Back up that directory privately; losing it prevents ordinary signed updates.
- The key's identity label is YIN; identity claims are not independently certified by Android. Publish the certificate SHA-256 alongside trusted APK downloads.
- The release app ID is `com.tyust.course.ibaraki`; development remains `com.tyust.course`. They coexist because a new release key cannot replace the prior debug-signed installation. The release requires its own initial school login and never bundles student data.
- Preserve upstream GPL notices and distribute corresponding source alongside APKs. Modified distributions must not claim they are YIN's original release.

## Supported installation target
- Universal APK, Android 7.0/API24 and later. Install from a trusted file source and allow that source's installation permission when Android asks.
- School login requires internet and an enabled, reasonably current Android System WebView (or device WebView provider). Notifications require system permission on Android13+; manufacturers control banners and background restrictions.
- A full update is attempted on each foreground visit, not an always-running background service. Expired school sessions may require school authentication again.
- New academic years/faculties are selectable but only verified curriculum/calendar rules are applied; unverified rules remain pending.
- Physical testing is limited to Xiaomi14; all-phone compatibility is not a verified claim. Devices without Android APK support cannot install this package.
- No school credentials, student files, private keys, or browser sessions belong in distribution archives.
