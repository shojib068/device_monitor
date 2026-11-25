# Graph Report - device_monitor  (2026-11-24)

## Corpus Check
- Corpus is ~12,251 words - fits in a single context window. You may not need a graph.

## Summary
- 551 nodes · 813 edges · 30 communities (21 shown, 9 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 17 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Windows Win32 Window
- Android Boot & Call Monitor
- macOS/iOS App Delegate
- Android Monitor Service
- Linux GTK Runner
- App Entry & Theming
- Screen Recording Monitor
- Pubspec Dependencies
- MainActivity Channel Bridge
- Google Drive Uploader
- Monitor Controller (Dart)
- Upload Job DAO
- Permissions Screen
- Dashboard Screen
- Monitor Module Model
- Windows Utils
- Drive Auth Service
- Settings Screen
- Drive Screen
- Module Screen
- Web Manifest
- Windows FlutterWindow
- Stateful Screens Base
- Analysis Options
- Windows FlutterWindow Impl
- Widget Test
- Windows Plugin Registrant

## God Nodes (most connected - your core abstractions)
1. `Win32Window` - 27 edges
2. `MonitorService` - 20 edges
3. `ScreenMonitor` - 18 edges
4. `UploadJobEntity` - 14 edges
5. `CallMonitor` - 13 edges
6. `AppDatabase` - 13 edges
7. `UploadJobDao` - 12 edges
8. `FlutterWindow` - 12 edges
9. `DownloadMonitor` - 9 edges
10. `DriveUploader` - 9 edges

## Surprising Connections (you probably didn't know these)
- `FlutterWindow::OnCreate()` --calls--> `RegisterPlugins()`  [INFERRED]
  windows/runner/flutter_window.cpp → windows/flutter/generated_plugin_registrant.cc
- `wWinMain()` --calls--> `CreateAndAttachConsole()`  [INFERRED]
  windows/runner/main.cpp → windows/runner/utils.cpp
- `MonitorService` --references--> `CallMonitor`  [EXTRACTED]
  android/app/src/main/kotlin/com/devicemonitor/app/MonitorService.kt → android/app/src/main/kotlin/com/devicemonitor/app/CallMonitor.kt
- `MonitorService` --references--> `DownloadMonitor`  [EXTRACTED]
  android/app/src/main/kotlin/com/devicemonitor/app/MonitorService.kt → android/app/src/main/kotlin/com/devicemonitor/app/DownloadMonitor.kt
- `MonitorService` --references--> `ScreenMonitor`  [EXTRACTED]
  android/app/src/main/kotlin/com/devicemonitor/app/MonitorService.kt → android/app/src/main/kotlin/com/devicemonitor/app/ScreenMonitor.kt

## Import Cycles
- None detected.

## Communities (30 total, 9 thin omitted)

### Community 0 - "Windows Win32 Window"
Cohesion: 0.06
Nodes (23): FlutterWindow::OnCreate(), Win32Window::Destroy(), Win32Window::GetClientArea(), Win32Window::GetHandle(), Win32Window::OnCreate(), Win32Window::Show(), EnableFullDpiSupportIfAvailable(), EnableNonClientDpiScaling (+15 more)

### Community 1 - "Android Boot & Call Monitor"
Cohesion: 0.06
Nodes (6): BootReceiver, CallMonitor, AppDatabase, DownloadMonitor, FileObserver, FileObserver

### Community 2 - "macOS/iOS App Delegate"
Cohesion: 0.06
Nodes (13): Cocoa, Flutter, FlutterMacOS, Foundation, AppDelegate, SceneDelegate, RunnerTests, RegisterGeneratedPlugins() (+5 more)

### Community 4 - "Linux GTK Runner"
Cohesion: 0.09
Nodes (14): fl_register_plugins(), main(), first_frame_cb(), my_application_activate(), my_application_class_init(), my_application_dispose(), my_application_init(), my_application_local_command_line() (+6 more)

### Community 5 - "App Entry & Theming"
Cohesion: 0.07
Nodes (18): build, _darkTheme, DeviceMonitorApp, _lightTheme, main, onboardingDone, prefs, startRoute (+10 more)

### Community 7 - "Pubspec Dependencies"
Cohesion: 0.08
Nodes (23): dependencies, cupertino_icons, flutter, flutter_secure_storage, google_sign_in, intl, permission_handler, shared_preferences (+15 more)

### Community 9 - "Google Drive Uploader"
Cohesion: 0.17
Nodes (6): DriveUploader, UploadResult, AuthError, QuotaExceeded, Success, TransientError

### Community 10 - "Monitor Controller (Dart)"
Cohesion: 0.10
Nodes (15): _channel, _events, getStatus, getUploadHistory, _instance, MonitorController, pauseMonitoring, provideMediaProjectionToken (+7 more)

### Community 12 - "Permissions Screen"
Cohesion: 0.12
Nodes (14): build, createState, _getRequiredModules, info, isSpecial, _PermInfo, permission, _permissionsFor (+6 more)

### Community 13 - "Dashboard Screen"
Cohesion: 0.12
Nodes (12): build, _controller, createState, _history, _init, initState, job, _loadHistory (+4 more)

### Community 14 - "Monitor Module Model"
Cohesion: 0.14
Nodes (12): active, activeModules, data, fromMap, key, MonitorModule, MonitorModuleExt, paused (+4 more)

### Community 15 - "Windows Utils"
Cohesion: 0.18
Nodes (4): wWinMain(), CreateAndAttachConsole(), GetCommandLineArguments(), Utf8FromUtf16()

### Community 16 - "Drive Auth Service"
Cohesion: 0.14
Nodes (9): currentAccount, DriveAuthService, getEmail, _googleSignIn, _instance, isSignedIn, signIn, signInSilently (+1 more)

### Community 17 - "Settings Screen"
Cohesion: 0.14
Nodes (13): build, _connectDrive, createState, _driveEmail, initState, _load, _loading, _modules (+5 more)

### Community 18 - "Drive Screen"
Cohesion: 0.18
Nodes (9): build, createState, DriveScreen, _DriveScreenState, _email, _error, _finish, _loading (+1 more)

### Community 19 - "Module Screen"
Cohesion: 0.17
Nodes (7): build, createState, _enabled, _icon, module, onToggle, _proceed

### Community 21 - "Web Manifest"
Cohesion: 0.18
Nodes (10): background_color, description, display, icons, name, orientation, prefer_related_applications, short_name (+2 more)

### Community 22 - "Windows FlutterWindow"
Cohesion: 0.25
Nodes (3): FlutterWindow, flutter_controller_, project_

### Community 23 - "Stateful Screens Base"
Cohesion: 0.32
Nodes (6): DashboardScreen, _DashboardScreenState, ModuleScreen, _ModuleScreenState, SettingsScreen, _SettingsScreenState

### Community 24 - "Analysis Options"
Cohesion: 0.33
Nodes (5): analyzer, exclude, include, linter, rules

### Community 25 - "Windows FlutterWindow Impl"
Cohesion: 0.33
Nodes (3): FlutterWindow::OnDestroy(), FlutterWindow::FlutterWindow(), Win32Window::OnDestroy()

## Knowledge Gaps
- **105 isolated node(s):** `prefs`, `onboardingDone`, `startRoute`, `main`, `build` (+100 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 253 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MonitorService` connect `Android Monitor Service` to `Android Boot & Call Monitor`, `Screen Recording Monitor`, `Windows Utils`?**
  _High betweenness centrality (0.317) - this node is a cross-community bridge._
- **What connects `prefs`, `onboardingDone`, `startRoute` to the rest of the system?**
  _105 weakly-connected nodes found - possible missing edges._
- **Should `Windows Win32 Window` be split into smaller, more focused modules?**
  _Cohesion score 0.06397306397306397 - nodes in this community are weakly interconnected._
- **Why does `ScreenMonitor` connect `Screen Recording Monitor` to `Android Boot & Call Monitor`, `Android Monitor Service`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Should `Android Boot & Call Monitor` be split into smaller, more focused modules?**
  _Cohesion score 0.061683599419448475 - nodes in this community are weakly interconnected._
- **Why does `Win32Window` connect `Windows Win32 Window` to `Windows FlutterWindow Impl`, `Windows Flutter Window Header`, `Windows FlutterWindow`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Should `macOS/iOS App Delegate` be split into smaller, more focused modules?**
  _Cohesion score 0.06342780026990553 - nodes in this community are weakly interconnected._