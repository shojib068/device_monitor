import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/module_model.dart';
import '../services/drive_auth_service.dart';
import '../services/monitor_controller.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  Map<MonitorModule, bool> _modules = {};
  bool _wifiOnly = true;
  String? _driveEmail;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    final keys = prefs.getStringList('selected_modules') ?? [];
    final email = await DriveAuthService().getEmail();
    if (!mounted) return;
    setState(() {
      _modules = {
        for (var m in MonitorModule.values) m: keys.contains(m.key)
      };
      _wifiOnly = prefs.getBool('wifi_only') ?? true;
      _driveEmail = email;
    });
  }

  Future<void> _saveModules() async {
    final prefs = await SharedPreferences.getInstance();
    final keys = _modules.entries
        .where((e) => e.value)
        .map((e) => e.key.key)
        .toList();
    await prefs.setStringList('selected_modules', keys);
    // Restart monitoring with updated modules
    final modules =
        _modules.entries.where((e) => e.value).map((e) => e.key).toList();
    await MonitorController().stopMonitoring();
    if (modules.isNotEmpty) {
      await MonitorController().startMonitoring(modules);
    }
  }

  Future<void> _setWifiOnly(bool val) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('wifi_only', val);
    setState(() => _wifiOnly = val);
    await MonitorController()
        .setUploadPreferences(wifiOnly: val, maxFileMb: 500);
  }

  Future<void> _signOut() async {
    setState(() => _loading = true);
    await DriveAuthService().signOut();
    await MonitorController().signOutDrive();
    if (!mounted) return;
    setState(() { _driveEmail = null; _loading = false; });
  }

  Future<void> _connectDrive() async {
    final account = await DriveAuthService().signIn();
    if (!mounted) return;
    setState(() => _driveEmail = account?.email);
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          const _SectionHeader('Active modules'),
          ...MonitorModule.values.map((m) => SwitchListTile(
                title: Text('${m.icon}  ${m.label}'),
                subtitle: Text(m.description,
                    style: TextStyle(
                        fontSize: 12, color: cs.onSurfaceVariant)),
                value: _modules[m] ?? false,
                onChanged: (val) {
                  setState(() => _modules[m] = val);
                  _saveModules();
                },
                isThreeLine: true,
              )),
          const Divider(height: 32),
          const _SectionHeader('Upload settings'),
          SwitchListTile(
            title: const Text('Wi-Fi only'),
            subtitle: Text('Only upload when connected to Wi-Fi.',
                style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant)),
            value: _wifiOnly,
            onChanged: _setWifiOnly,
          ),
          const Divider(height: 32),
          const _SectionHeader('Google Drive'),
          if (_driveEmail != null)
            ListTile(
              leading: const Icon(Icons.account_circle_outlined),
              title: Text(_driveEmail!),
              trailing: TextButton(
                onPressed: _loading ? null : _signOut,
                child: const Text('Sign out'),
              ),
            )
          else
            ListTile(
              leading: const Icon(Icons.login),
              title: const Text('Connect Google Drive'),
              trailing: FilledButton.tonal(
                onPressed: _connectDrive,
                child: const Text('Connect'),
              ),
            ),
          const Divider(height: 32),
          const _SectionHeader('About'),
          const ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Device Monitor'),
            subtitle: Text('v1.0.0'),
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String text;
  const _SectionHeader(this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
      child: Text(text,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: Theme.of(context).colorScheme.primary,
            letterSpacing: 0.5,
          )),
    );
  }
}
