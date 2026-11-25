# Device Monitor — Flutter Android App

Records screen activity, downloads, and calls, then uploads everything to Google Drive.

---

## Setup

### 1. Google Sign-In / Drive API

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a new project → Enable **Google Drive API**
3. Under **Credentials** → Create **OAuth 2.0 Client ID** → Android
4. Enter your package name: `com.devicemonitor.app`
5. Enter your SHA-1 fingerprint:
   ```
   cd android && ./gradlew signingReport
   ```
6. Download `google-services.json` → place in `android/app/`

### 2. Install Flutter dependencies

```bash
flutter pub get
```

### 3. Android permissions note

This app uses `MANAGE_EXTERNAL_STORAGE` and call recording — **not allowed on the Play Store**.
Distribute via sideloading (APK) or MDM/enterprise.

Enable "Install from unknown sources" on the test device.

### 4. Run the app

```bash
flutter run --release
```

---

## Project structure

```
lib/
  main.dart                        App entry, routing
  models/module_model.dart         Module enums, status models
  services/
    monitor_controller.dart        Flutter ↔ native MethodChannel
    drive_auth_service.dart        Google Sign-In wrapper
  screens/
    onboarding/                    4-step onboarding flow
    dashboard_screen.dart          Live status + upload history
    settings_screen.dart           Module toggles + Drive account

android/app/src/main/kotlin/com/devicemonitor/app/
  MainActivity.kt                  MethodChannel + MediaProjection handler
  MonitorService.kt                Foreground service (orchestrator)
  ScreenMonitor.kt                 UsageStats polling + MediaRecorder
  DownloadMonitor.kt               FileObserver on Downloads/
  CallMonitor.kt                   TelephonyManager + MediaRecorder
  UploadWorker.kt                  WorkManager upload job
  BootReceiver.kt                  Auto-restart after reboot
  db/                              Room database (upload queue)
  drive/DriveUploader.kt           Google Drive REST API v3
```

---

## Drive folder layout

```
DeviceMonitor/
  screen/<device>/<date>/<app>_<timestamp>.mp4
  downloads/<device>/<date>/<filename>
  calls/<device>/<date>/call_<timestamp>_<duration>s.m4a
```

---

## Legal note

Call recording laws vary by country. The app shows a consent disclaimer before enabling
the Call Recorder. You are responsible for compliance with local laws wherever this app is used.
