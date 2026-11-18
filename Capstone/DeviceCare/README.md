# Device Care – Android

Native Android device monitoring app built with Kotlin, Jetpack Compose, Material 3 and TensorFlow Lite.

## Screens
- Home dashboard with live resource cards and quick navigation
- Battery dashboard: charge %, charging state, approximate remaining runtime when the device exposes charge-counter/current readings, temperature, voltage, OS-reported condition and history
- Storage analyzer: total/used/free storage plus accessible photos, videos, audio, this app's private data/cache and an estimated system/other remainder
- RAM monitor and trend
- CPU monitor for the Device Care app process
- Device information
- App manager shortcut to Android settings
- Battery care guide
- Storage care guide and Android storage settings shortcut
- Network/Wi-Fi settings shortcut
- On-device AI insights
- Performance history
- Privacy center
- Settings shortcut

## Important Android limitations
- Media breakdown requests optional Android media permissions. Categories that Android does not allow the app to read are shown as not accessible.
- System & other is the unclassified remainder, not a file-by-file inventory of protected system and other apps' private data.
- Battery runtime is an estimate based on the battery charge counter and current draw where supported. Some devices do not expose these values. It is not a guarantee.
- Android's battery health intent is a coarse OS status, not a battery-capacity health percentage.
- CPU is estimated for this app's process only; Android restricts third-party access to total device CPU on many devices.
- Android manages RAM; this app does not force-stop other apps or promise to free system memory.
- History is in-memory session history and resets when the app process ends.
- The AI model is trained on synthetic resource scenarios. Model confidence is not a validated hardware-failure probability.

## Build
Open this folder in Android Studio and build the app debug variant, or use the repository GitHub Actions workflow. The workflow trains/packages the TensorFlow Lite model and uploads a debug APK artifact.
