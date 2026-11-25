import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

class PermissionsScreen extends StatefulWidget {
  const PermissionsScreen({super.key});

  @override
  State<PermissionsScreen> createState() => _PermissionsScreenState();
}

class _PermissionsScreenState extends State<PermissionsScreen> {
  bool _requesting = false;

  Future<List<String>> _getRequiredModules() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getStringList('selected_modules') ?? [];
  }

  List<_PermInfo> _permissionsFor(List<String> modules) {
    final perms = <_PermInfo>[];
    // Always needed
    perms.add(const _PermInfo(
      permission: Permission.ignoreBatteryOptimizations,
      title: 'Battery optimisation',
      reason: 'Keeps monitoring running in the background.',
    ));
    if (modules.contains('SCREEN')) {
      perms.add(const _PermInfo(
        permission: Permission.systemAlertWindow,
        title: 'Usage access',
        reason: 'Detects which app is in the foreground so recording can start.',
        isSpecial: true,
      ));
    }
    if (modules.contains('DOWNLOAD') || modules.contains('SCREEN')) {
      perms.add(const _PermInfo(
        permission: Permission.storage,
        title: 'Storage',
        reason: 'Reads downloaded files and saves recordings locally.',
      ));
    }
    if (modules.contains('CALL')) {
      perms.add(const _PermInfo(
        permission: Permission.microphone,
        title: 'Microphone',
        reason: 'Records your voice during phone calls.',
      ));
      perms.add(const _PermInfo(
        permission: Permission.phone,
        title: 'Phone',
        reason: 'Detects when a call starts or ends.',
      ));
    }
    return perms;
  }

  Future<void> _requestAll() async {
    setState(() => _requesting = true);
    final modules = await _getRequiredModules();
    final perms = _permissionsFor(modules);
    for (final p in perms) {
      if (p.isSpecial) {
        await openAppSettings();
      } else {
        await p.permission.request();
      }
    }
    setState(() => _requesting = false);
    if (!mounted) return;
    Navigator.pushNamed(context, '/onboarding/drive');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Permissions')),
      body: FutureBuilder<List<String>>(
        future: _getRequiredModules(),
        builder: (context, snap) {
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final perms = _permissionsFor(snap.data!);
          final cs = Theme.of(context).colorScheme;
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('The app needs these permissions.',
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                Text(
                  'Each one is only used for the feature you chose.',
                  style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13),
                ),
                const SizedBox(height: 24),
                ...perms.map((p) => _PermCard(info: p)),
                const Spacer(),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: _requesting ? null : _requestAll,
                    style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16)),
                    child: _requesting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Grant permissions',
                            style: TextStyle(fontSize: 16)),
                  ),
                ),
                const SizedBox(height: 10),
                Center(
                  child: TextButton(
                    onPressed: () =>
                        Navigator.pushNamed(context, '/onboarding/drive'),
                    child: const Text('Skip for now'),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _PermInfo {
  final Permission permission;
  final String title;
  final String reason;
  final bool isSpecial;

  const _PermInfo({
    required this.permission,
    required this.title,
    required this.reason,
    this.isSpecial = false,
  });
}

class _PermCard extends StatelessWidget {
  final _PermInfo info;
  const _PermCard({required this.info});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: cs.tertiaryContainer,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(Icons.security_outlined,
              color: cs.tertiary, size: 20),
        ),
        title: Text(info.title,
            style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(info.reason,
            style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant)),
        isThreeLine: false,
      ),
    );
  }
}
