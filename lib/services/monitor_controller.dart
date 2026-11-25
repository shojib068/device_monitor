import 'package:flutter/services.dart';
import '../models/module_model.dart';

class MonitorController {
  static const _channel = MethodChannel('com.devicemonitor/service');
  static const _events = EventChannel('com.devicemonitor/events');

  static final MonitorController _instance = MonitorController._();
  factory MonitorController() => _instance;
  MonitorController._();

  Stream<Map<Object?, Object?>> get serviceEvents =>
      _events.receiveBroadcastStream().cast<Map<Object?, Object?>>();

  Future<bool> startMonitoring(List<MonitorModule> modules) async {
    try {
      final result = await _channel.invokeMapMethod<String, dynamic>(
        'startMonitoring',
        {'modules': modules.map((m) => m.key).toList()},
      );
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }

  Future<bool> stopMonitoring() async {
    try {
      final result =
          await _channel.invokeMapMethod<String, dynamic>('stopMonitoring');
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }

  Future<bool> pauseMonitoring() async {
    try {
      final result =
          await _channel.invokeMapMethod<String, dynamic>('pauseMonitoring');
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }

  Future<bool> resumeMonitoring() async {
    try {
      final result =
          await _channel.invokeMapMethod<String, dynamic>('resumeMonitoring');
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }

  Future<ServiceStatus> getStatus() async {
    try {
      final result =
          await _channel.invokeMapMethod<String, dynamic>('getServiceStatus');
      return ServiceStatus.fromMap(result ?? {});
    } on PlatformException {
      return const ServiceStatus();
    }
  }

  Future<bool> setUploadPreferences(
      {required bool wifiOnly, required int maxFileMb}) async {
    try {
      final result = await _channel.invokeMapMethod<String, dynamic>(
        'setUploadPreferences',
        {'wifiOnly': wifiOnly, 'maxFileMb': maxFileMb},
      );
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }

  Future<List<Map>> getUploadHistory(
      {int limit = 20, int offset = 0}) async {
    try {
      final result = await _channel.invokeListMethod<Map>(
        'getUploadHistory',
        {'limit': limit, 'offset': offset},
      );
      return result ?? [];
    } on PlatformException {
      return [];
    }
  }

  Future<int> retryFailedUploads() async {
    try {
      final result =
          await _channel.invokeMapMethod<String, dynamic>('retryFailedUploads');
      return result?['queued'] as int? ?? 0;
    } on PlatformException {
      return 0;
    }
  }

  Future<bool> signOutDrive() async {
    try {
      final result =
          await _channel.invokeMapMethod<String, dynamic>('signOutDrive');
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }

  Future<bool> provideMediaProjectionToken(
      {required int resultCode, required dynamic intentData}) async {
    try {
      final result = await _channel.invokeMapMethod<String, dynamic>(
        'provideMediaProjectionToken',
        {'resultCode': resultCode, 'data': intentData},
      );
      return result?['success'] == true;
    } on PlatformException {
      return false;
    }
  }
}
