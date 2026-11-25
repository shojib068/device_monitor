package com.devicemonitor.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val prefs: SharedPreferences =
            context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

        val onboardingDone = prefs.getBoolean("flutter.onboarding_complete", false)
        if (!onboardingDone) return

        val modules = prefs.getStringSet("flutter.selected_modules", emptySet()) ?: emptySet()
        if (modules.isEmpty()) return

        // Screen module requires a fresh MediaProjection grant — start without it on reboot.
        // The user will need to re-grant when they open the app.
        val activeModules = ArrayList(modules.filter { it != "SCREEN" })
        if (activeModules.isEmpty()) {
            Log.d("BootReceiver", "Only SCREEN module was active — skipping auto-restart")
            return
        }

        val serviceIntent = Intent(context, MonitorService::class.java).apply {
            action = MonitorService.ACTION_START
            putStringArrayListExtra(MonitorService.EXTRA_MODULES, activeModules)
        }
        context.startForegroundService(serviceIntent)
        Log.d("BootReceiver", "Started service with modules: $activeModules")
    }
}
