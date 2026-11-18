# Device Care — Mobile Device Performance and Resource Management

Native Android app built with Kotlin, Jetpack Compose, Material 3 and TensorFlow Lite.

## Features
- Live battery percentage and charging state
- System RAM and internal storage usage
- Device Care process CPU-time estimate (not total system CPU)
- TensorFlow Lite three-class resource-risk classifier (healthy / monitor / high risk)
- On-device model inference; no telemetry upload
- AI insight screen with class scores and actionable resource guidance
- Live RAM, battery and storage trend charts
- Session battery-change summary
- Adaptive statistical anomaly detector as a transparent secondary signal

## Architecture
- Android UI: Kotlin + Jetpack Compose
- Device metrics: Android BatteryManager, ActivityManager and StatFs
- ML training: Python, TensorFlow/Keras
- ML runtime: TensorFlow Lite Android interpreter
- CI: GitHub Actions trains the model, packages the .tflite asset, then builds a debug APK

## Machine-learning model
The training script is in `ml/train_device_model.py`. It generates reproducible synthetic examples with five normalized inputs: battery level, RAM usage, storage usage, app-process CPU estimate and charging state. A small dense neural network is trained to classify resource pressure into three classes and exported to `app/src/main/assets/device_health_model.tflite`.

The labels are generated from explicit threshold rules. This is an academic proof-of-concept model, not a validated predictor of hardware failure, battery health or device lifespan. Synthetic validation accuracy measures agreement with those generated labels only. Real-world evaluation requires consented, labelled device telemetry from multiple models and Android versions.

## Build locally
Requirements: Android Studio, JDK 17, Android SDK 35, Python 3.11 and pip.

First train and generate the model asset:
```bash
python -m pip install tensorflow-cpu==2.16.1 numpy==1.26.4
python ml/train_device_model.py --output app/src/main/assets/device_health_model.tflite
```

Then open this folder in Android Studio, sync Gradle and run the app. Command line:
```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
Workflow: `.github/workflows/devicecare-android-build.yml`.
Every matching push, pull request or manual dispatch:
1. Trains and exports the TensorFlow Lite model.
2. Passes the model asset to the Android build job.
3. Builds the debug APK.
4. Uploads `device-care-debug-apk` as a workflow artifact.

Open the repository's **Actions** tab, select **Device Care Android APK**, open the latest run, and download the artifact after the build succeeds.

## Platform accuracy notes
Android restricts system-wide CPU and per-app battery telemetry. The CPU card estimates CPU time used by this app process, not total device CPU. Battery percentage and charging state are read from the Android battery broadcast; they are not a battery capacity/health diagnosis. Session charts are in-memory and reset when the app process ends. Android does not permit ordinary third-party apps to force-stop other apps or arbitrarily clear their memory; Device Care provides guidance rather than unsafe "RAM booster" claims.

All monitoring and model inference run locally on the phone.
