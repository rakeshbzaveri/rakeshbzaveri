# Ditto Mobile

A standalone Android clipboard history app. It keeps local text history, supports search and pinning, and provides a floating launcher. It does not replace your keyboard.

## Build

GitHub Actions builds a debug APK automatically when source changes are pushed. Download the `DittoMobile-debug-apk` artifact from the Actions run.

## Android clipboard limitation

Android 10 and later restrict background clipboard reads. Ditto Mobile attempts background capture where the device permits it and can capture the current clipboard when opened. For dependable capture on restrictive devices, open the app after copying.

## Privacy

Clipboard history is stored locally in the app's private SQLite database. The app requests no internet permission and has no cloud sync.

Package: `com.rakesh.dittomobile`
Minimum Android: 8.0 (API 26)
