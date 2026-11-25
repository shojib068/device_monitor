import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/intl.dart';
import '../models/module_model.dart';
import '../services/monitor_controller.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final _controller = MonitorController();
  ServiceStatus _status = const ServiceStatus();
  List<Map> _history = [];
  bool _starting = false;

  @override
  void initState() {
    super.initState();
    _init();
    _controller.serviceEvents.listen((event) {
      if (!mounted) return;
      final type = event['type'] as String? ?? '';
      if (type == 'statusChanged') {
        setState(() =>
            _status = ServiceStatus.fromMap(Map<String, dynamic>.from(
                event['data'] as Map? ?? {})));
      } else if (type == 'uploadComplete') {
        _loadHistory();
      }
    });
  }

  Future<void> _init() async {
    final status = await _controller.getStatus();
    if (!mounted) return;
    setState(() => _status = status);
    if (!status.active) await _startService();
    await _loadHistory();
  }

  Future<void> _startService() async {
    setState(() => _starting = true);
    final prefs = await SharedPreferences.getInstance();
    final keys = prefs.getStringList('selected_modules') ?? [];
    final modules = MonitorModule.values
        .where((m) => keys.contains(m.key))
        .toList();
    if (modules.isNotEmpty) await _controller.startMonitoring(modules);
    final status = await _controller.getStatus();
    if (!mounted) return;
    setState(() { _status = status; _starting = false; });
  }

  Future<void> _loadHistory() async {
    final h = await _controller.getUploadHistory(limit: 20);
    if (!mounted) return;
    setState(() => _history = h);
  }

  Future<void> _togglePause() async {
    if (_status.paused) {
      await _controller.resumeMonitoring();
    } else {
      await _controller.pauseMonitoring();
    }
    final status = await _controller.getStatus();
    if (!mounted) return;
    setState(() => _status = status);
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Device Monitor'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => Navigator.pushNamed(context, '/settings'),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadHistory,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Status card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: _starting
                                ? cs.outline
                                : _status.active
                                    ? (_status.paused
                                        ? Colors.orange
                                        : Colors.green)
                                    : cs.error,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          _starting
                              ? 'Starting…'
                              : _status.active
                                  ? (_status.paused ? 'Paused' : 'Monitoring active')
                                  : 'Not running',
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        const Spacer(),
                        if (_status.uploadQueueSize > 0)
                          Chip(
                            label: Text('${_status.uploadQueueSize} pending',
                                style: const TextStyle(fontSize: 12)),
                            backgroundColor: cs.tertiaryContainer,
                            padding: EdgeInsets.zero,
                          ),
                      ],
                    ),
                    if (_status.activeModules.isNotEmpty) ...[
                      const SizedBox(height: 12),
                      Wrap(
                        spacing: 8,
                        children: _status.activeModules
                            .map((m) => Chip(
                                  label: Text(m, style: const TextStyle(fontSize: 11)),
                                  backgroundColor: cs.primaryContainer,
                                  padding: EdgeInsets.zero,
                                ))
                            .toList(),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 8),

            // Pause / resume button
            if (_status.active)
              OutlinedButton.icon(
                onPressed: _togglePause,
                icon: Icon(_status.paused ? Icons.play_arrow : Icons.pause),
                label: Text(_status.paused ? 'Resume monitoring' : 'Pause monitoring'),
              ),

            const SizedBox(height: 20),

            Text('Recent uploads',
                style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),

            if (_history.isEmpty)
              Center(
                child: Padding(
                  padding: const EdgeInsets.all(32),
                  child: Column(
                    children: [
                      Icon(Icons.cloud_upload_outlined,
                          size: 48, color: cs.outline),
                      const SizedBox(height: 12),
                      Text('No uploads yet',
                          style: TextStyle(color: cs.onSurfaceVariant)),
                    ],
                  ),
                ),
              )
            else
              ...(_history.map((job) => _UploadRow(job: job))),
          ],
        ),
      ),
    );
  }
}

class _UploadRow extends StatelessWidget {
  final Map job;
  const _UploadRow({required this.job});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final status = job['status'] as String? ?? '';
    final ts = job['uploadedAt'] as int?;
    final timeStr = ts != null
        ? DateFormat('dd MMM, HH:mm')
            .format(DateTime.fromMillisecondsSinceEpoch(ts))
        : '—';

    Color statusColor;
    IconData statusIcon;
    switch (status) {
      case 'DONE':
        statusColor = Colors.green;
        statusIcon = Icons.check_circle_outline;
        break;
      case 'FAILED':
        statusColor = cs.error;
        statusIcon = Icons.error_outline;
        break;
      case 'IN_PROGRESS':
        statusColor = cs.primary;
        statusIcon = Icons.upload_outlined;
        break;
      default:
        statusColor = cs.outline;
        statusIcon = Icons.schedule_outlined;
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(statusIcon, color: statusColor),
        title: Text(
          job['filename'] as String? ?? 'Unknown',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 14),
        ),
        subtitle: Text(
          '${job['module'] ?? ''} · $timeStr',
          style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant),
        ),
        trailing: Chip(
          label: Text(status, style: const TextStyle(fontSize: 10)),
          padding: EdgeInsets.zero,
          backgroundColor: statusColor.withValues(alpha: 0.12),
        ),
      ),
    );
  }
}
