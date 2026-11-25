package com.devicemonitor.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devicemonitor.app.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MonitorService : Service() {

    private val TAG = "MonitorService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var screenMonitor: ScreenMonitor? = null
    private var downloadMonitor: DownloadMonitor? = null
    private var callMonitor: CallMonitor? = null
    private var isPaused = false

    companion object {
        const val CHANNEL_ID = "device_monitor_service"
        const val NOTIF_ID = 1

        const val ACTION_START  = "com.devicemonitor.START"
        const val ACTION_STOP   = "com.devicemonitor.STOP"
        const val ACTION_PAUSE  = "com.devicemonitor.PAUSE"
        const val ACTION_RESUME = "com.devicemonitor.RESUME"
        const val EXTRA_MODULES = "modules"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_INTENT_DATA = "intentData"

        // Singleton reference so MainActivity can send events back to Flutter
        @Volatile var eventSink: io.flutter.plugin.common.EventChannel.EventSink? = null

        fun sendEvent(context: Context, type: String, data: Map<String, Any>) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                eventSink?.success(mapOf("type" to type, "data" to data))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> handleStart(intent)
            ACTION_STOP   -> handleStop()
            ACTION_PAUSE  -> handlePause()
            ACTION_RESUME -> handleResume()
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val modules = intent.getStringArrayListExtra(EXTRA_MODULES) ?: return
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val projData = intent.getParcelableExtra<Intent>(EXTRA_INTENT_DATA)

        startForeground(NOTIF_ID, buildNotification(modules))

        if ("SCREEN" in modules && projData != null && resultCode != -1) {
            screenMonitor = ScreenMonitor(this, scope).also {
                it.start(resultCode, projData)
            }
        }
        if ("DOWNLOAD" in modules) {
            downloadMonitor = DownloadMonitor(this, scope).also { it.start() }
        }
        if ("CALL" in modules) {
            callMonitor = CallMonitor(this, scope).also { it.start() }
        }

        sendStatusUpdate(modules)
        Log.d(TAG, "Service started with modules: $modules")
    }

    private fun handleStop() {
        screenMonitor?.stop(); screenMonitor = null
        downloadMonitor?.stop(); downloadMonitor = null
        callMonitor?.stop(); callMonitor = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        sendEvent(this, "statusChanged", mapOf("active" to false, "paused" to false, "modules" to emptyList<String>()))
        Log.d(TAG, "Service stopped")
    }

    private fun handlePause() {
        isPaused = true
        screenMonitor?.stop()
        // keep monitors registered but not recording
        sendStatusUpdate(activeModuleNames())
    }

    private fun handleResume() {
        isPaused = false
        sendStatusUpdate(activeModuleNames())
    }

    private fun activeModuleNames(): List<String> {
        return listOfNotNull(
            if (screenMonitor != null) "SCREEN" else null,
            if (downloadMonitor != null) "DOWNLOAD" else null,
            if (callMonitor != null) "CALL" else null
        )
    }

    private fun sendStatusUpdate(modules: List<String>) {
        scope.launch(Dispatchers.Main) {
            val pendingCount = AppDatabase.getInstance(this@MonitorService)
                .uploadJobDao().getPendingCount()
            sendEvent(
                this@MonitorService, "statusChanged",
                mapOf(
                    "active" to true,
                    "paused" to isPaused,
                    "modules" to modules,
                    "uploadQueueSize" to pendingCount
                )
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Device Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Active monitoring notification" }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(modules: List<String>): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val moduleText = modules.joinToString(" · ")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Monitor — Active")
            .setContentText(moduleText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
