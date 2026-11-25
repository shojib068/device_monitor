// Basic smoke test for Device Monitor.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:device_monitor/main.dart';

void main() {
  testWidgets('App builds and shows the welcome screen', (WidgetTester tester) async {
    await tester.pumpWidget(const DeviceMonitorApp(startRoute: '/'));
    await tester.pumpAndSettle();

    // The app should render without throwing.
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
