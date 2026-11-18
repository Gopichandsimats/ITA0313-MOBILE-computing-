# Device Care — Mobile Device Performance and Resource Management

A native Android application built with Kotlin and Jetpack Compose. It monitors available device resource metrics and provides privacy-first, on-device insights.

## Current features
- Live battery percentage and charging state
- System RAM usage using Android ActivityManager
- Internal storage usage using StatFs
- Device Care app-process CPU time estimate (not total system CPU)
- Adaptive history-based anomaly detection
- Resource recommendations and a live dashboard
- Local-only processing; no account or telemetry upload

## Technology
- Kotlin, Android SDK, Jetpack Compose, Material 3
- Android platform APIs for battery, memory and storage
- On-device adaptive statistical anomaly detector

## Build
Open this folder in Android Studio (Giraffe or newer) with JDK 17 and Android SDK 35, then sync Gradle and run the app.

For command-line build:
```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
The workflow at `.github/workflows/android-build.yml` builds a debug APK and uploads it as the `device-care-debug-apk` artifact on pushes, pull requests and manual runs.

## Accuracy and privacy notes
Android restricts access to some system-wide telemetry. The CPU figure in this version is the Device Care process CPU estimate, not total device CPU utilization. Resource readings and anomaly analysis remain on-device. The adaptive detector uses recent readings as a baseline and should be treated as decision support, not as a guarantee of hardware fault prediction.

## Roadmap
- Add historical charts and Room persistence
- Add thermal status and supported battery health fields
- Add optional user-controlled alerts
- Expand tests across Android versions and device manufacturers
