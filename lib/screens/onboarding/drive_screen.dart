import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../services/drive_auth_service.dart';

class DriveScreen extends StatefulWidget {
  const DriveScreen({super.key});

  @override
  State<DriveScreen> createState() => _DriveScreenState();
}

class _DriveScreenState extends State<DriveScreen> {
  bool _loading = false;
  String? _error;
  String? _email;

  Future<void> _signIn() async {
    setState(() { _loading = true; _error = null; });
    final account = await DriveAuthService().signIn();
    if (!mounted) return;
    if (account != null) {
      setState(() { _email = account.email; _loading = false; });
    } else {
      setState(() { _error = 'Sign-in failed. Try again.'; _loading = false; });
    }
  }

  Future<void> _finish() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('onboarding_complete', true);
    if (!mounted) return;
    Navigator.pushNamedAndRemoveUntil(context, '/dashboard', (_) => false);
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Connect Google Drive')),
      body: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Spacer(),
            Center(
              child: Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Icon(Icons.drive_folder_upload_outlined,
                    size: 44, color: cs.primary),
              ),
            ),
            const SizedBox(height: 32),
            Text(
              'Where recordings go',
              style: Theme.of(context)
                  .textTheme
                  .headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Text(
              'All screen recordings, downloads, and call audio are uploaded to a "DeviceMonitor" folder in your Google Drive — organised by date.',
              style: TextStyle(color: cs.onSurfaceVariant, height: 1.5),
            ),
            const SizedBox(height: 32),
            if (_email != null) ...[
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: cs.secondaryContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(Icons.check_circle_outline,
                        color: cs.secondary, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text('Connected as $_email',
                          style: TextStyle(color: cs.onSecondaryContainer)),
                    ),
                  ],
                ),
              ),
            ] else if (_error != null) ...[
              Text(_error!,
                  style: TextStyle(color: cs.error, fontSize: 13)),
              const SizedBox(height: 8),
            ],
            const Spacer(),
            if (_email == null) ...[
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _loading ? null : _signIn,
                  icon: _loading
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.login),
                  label: const Text('Sign in with Google'),
                  style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14)),
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: TextButton(
                  onPressed: _finish,
                  child: const Text('Skip — I\'ll connect later'),
                ),
              ),
            ] else ...[
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _finish,
                  style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16)),
                  child: const Text('Finish setup',
                      style: TextStyle(fontSize: 16)),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
