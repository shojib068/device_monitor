import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'screens/onboarding/welcome_screen.dart';
import 'screens/dashboard_screen.dart';
import 'screens/settings_screen.dart';
import 'screens/onboarding/module_screen.dart';
import 'screens/onboarding/permissions_screen.dart';
import 'screens/onboarding/drive_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  final onboardingDone = prefs.getBool('onboarding_complete') ?? false;
  runApp(DeviceMonitorApp(startRoute: onboardingDone ? '/dashboard' : '/'));
}

class DeviceMonitorApp extends StatelessWidget {
  final String startRoute;
  const DeviceMonitorApp({super.key, required this.startRoute});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Device Monitor',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.system,
      theme: _lightTheme(),
      darkTheme: _darkTheme(),
      initialRoute: startRoute,
      routes: {
        '/': (_) => const WelcomeScreen(),
        '/onboarding/modules': (_) => const ModuleScreen(),
        '/onboarding/permissions': (_) => const PermissionsScreen(),
        '/onboarding/drive': (_) => const DriveScreen(),
        '/dashboard': (_) => const DashboardScreen(),
        '/settings': (_) => const SettingsScreen(),
      },
    );
  }

  ThemeData _lightTheme() => ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1A73E8),
          brightness: Brightness.light,
        ),
        cardTheme: const CardThemeData(elevation: 2),
      );

  ThemeData _darkTheme() => ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1A73E8),
          brightness: Brightness.dark,
        ),
        cardTheme: const CardThemeData(elevation: 2),
      );
}
