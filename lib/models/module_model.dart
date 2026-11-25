enum MonitorModule { screen, download, call }

extension MonitorModuleExt on MonitorModule {
  String get label {
    switch (this) {
      case MonitorModule.screen: return 'Screen Monitor';
      case MonitorModule.download: return 'Download Monitor';
      case MonitorModule.call: return 'Call Recorder';
    }
  }

  String get description {
    switch (this) {
      case MonitorModule.screen:
        return 'Records the screen every time you open a new app and saves the video to Drive.';
      case MonitorModule.download:
        return 'Copies every file you download to your Google Drive automatically.';
      case MonitorModule.call:
        return 'Records your voice during phone calls and saves the audio to Drive.';
    }
  }

  String get icon {
    switch (this) {
      case MonitorModule.screen: return '🖥️';
      case MonitorModule.download: return '⬇️';
      case MonitorModule.call: return '📞';
    }
  }

  String get key => name.toUpperCase();
}

class ServiceStatus {
  final bool active;
  final bool paused;
  final List<String> activeModules;
  final int uploadQueueSize;

  const ServiceStatus({
    this.active = false,
    this.paused = false,
    this.activeModules = const [],
    this.uploadQueueSize = 0,
  });

  factory ServiceStatus.fromMap(Map map) => ServiceStatus(
        active: map['active'] as bool? ?? false,
        paused: map['paused'] as bool? ?? false,
        activeModules: List<String>.from(map['modules'] ?? []),
        uploadQueueSize: map['uploadQueueSize'] as int? ?? 0,
      );
}

class UploadEvent {
  final String type; // uploadComplete | uploadFailed | quotaExceeded | recordingError
  final Map<String, dynamic> data;

  const UploadEvent({required this.type, required this.data});

  factory UploadEvent.fromMap(Map map) => UploadEvent(
        type: map['type'] as String,
        data: Map<String, dynamic>.from(map['data'] ?? {}),
      );
}
