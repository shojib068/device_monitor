import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../models/module_model.dart';

class ModuleScreen extends StatefulWidget {
  const ModuleScreen({super.key});

  @override
  State<ModuleScreen> createState() => _ModuleScreenState();
}

class _ModuleScreenState extends State<ModuleScreen> {
  final Map<MonitorModule, bool> _enabled = {
    MonitorModule.screen: true,
    MonitorModule.download: true,
    MonitorModule.call: false,
  };

  Future<void> _proceed() async {
    final prefs = await SharedPreferences.getInstance();
    final selected =
        _enabled.entries.where((e) => e.value).map((e) => e.key.key).toList();
    await prefs.setStringList('selected_modules', selected);
    if (!mounted) return;
    Navigator.pushNamed(context, '/onboarding/permissions');
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Choose features')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Turn on the features you want.',
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text('You can change these any time in Settings.',
                style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
            const SizedBox(height: 24),
            ...MonitorModule.values.map((m) => _ModuleCard(
                  module: m,
                  enabled: _enabled[m]!,
                  onToggle: (val) => setState(() => _enabled[m] = val),
                )),
            if (_enabled[MonitorModule.call] == true) ...[
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: cs.errorContainer.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(Icons.warning_amber_rounded,
                        color: cs.error, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'Call recording laws vary by region. You are responsible for compliance with local laws.',
                        style: TextStyle(fontSize: 12, color: cs.onErrorContainer),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const Spacer(),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed:
                    _enabled.values.any((v) => v) ? _proceed : null,
                style: FilledButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16)),
                child: const Text('Continue', style: TextStyle(fontSize: 16)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ModuleCard extends StatelessWidget {
  final MonitorModule module;
  final bool enabled;
  final ValueChanged<bool> onToggle;

  const _ModuleCard(
      {required this.module,
      required this.enabled,
      required this.onToggle});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: SwitchListTile(
        value: enabled,
        onChanged: onToggle,
        title: Text('${module.icon}  ${module.label}',
            style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(module.description,
              style: TextStyle(fontSize: 13, color: cs.onSurfaceVariant)),
        ),
        secondary: Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: enabled
                ? cs.primaryContainer
                : cs.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            _icon(module),
            color: enabled ? cs.primary : cs.onSurfaceVariant,
            size: 22,
          ),
        ),
        isThreeLine: true,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      ),
    );
  }

  IconData _icon(MonitorModule m) {
    switch (m) {
      case MonitorModule.screen: return Icons.screen_share_outlined;
      case MonitorModule.download: return Icons.download_outlined;
      case MonitorModule.call: return Icons.call_outlined;
    }
  }
}
