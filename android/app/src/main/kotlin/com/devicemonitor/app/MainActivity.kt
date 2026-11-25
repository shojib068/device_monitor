package com.devicemonitor.app

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.devicemonitor.app.db.AppDatabase
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.devicemonitor/service"
    private val EVENT_CHANNEL = "com.devicemonitor/events"
    private val REQUEST_MEDIA_PROJECTION = 1001

    private var pendingProjectionResult: MethodChannel.Result? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Event channel (Kotlin → Dart)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(args: Any?, sink: EventChannel.EventSink?) {
                    MonitorService.eventSink = sink
                }
                override fun onCancel(args: Any?) {
                    MonitorService.eventSink = null
                }
            })

        // Method channel (Dart → Kotlin)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {

                    "startMonitoring" -> {
                        val modules = call.argument<List<String>>("modules")
                            ?.let { ArrayList(it) } ?: arrayListOf()

                        if (modules.contains("SCREEN")) {
                            // Need MediaProjection permission first
                            pendingProjectionResult = result
                            pendingModules = modules
                            requestMediaProjection()
                        } else {
                            startService(modules, -1, null)
                            result.success(mapOf("success" to true))
                        }
                    }

                    "stopMonitoring" -> {
                        startService(action = MonitorService.ACTION_STOP)
                        result.success(mapOf("success" to true))
                    }

                    "pauseMonitoring" -> {
                        startService(action = MonitorService.ACTION_PAUSE)
                        result.success(mapOf("success" to true))
                    }

                    "resumeMonitoring" -> {
                        startService(action = MonitorService.ACTION_RESUME)
                        result.success(mapOf("success" to true))
                    }

                    "getServiceStatus" -> {
                        scope.launch {
                            val count = AppDatabase.getInstance(this@MainActivity)
                                .uploadJobDao().getPendingCount()
                            launch(Dispatchers.Main) {
                                result.success(mapOf(
                                    "active" to isServiceRunning(),
                                    "paused" to false,
                                    "modules" to listOf<String>(),
                                    "uploadQueueSize" to count
                                ))
                            }
                        }
                    }

                    "getUploadHistory" -> {
                        val limit = call.argument<Int>("limit") ?: 20
                        val offset = call.argument<Int>("offset") ?: 0
                        scope.launch {
                            val jobs = AppDatabase.getInstance(this@MainActivity)
                                .uploadJobDao().getHistory(limit, offset)
                            val list = jobs.map {
                                mapOf(
                                    "id" to it.id,
                                    "filename" to it.filename,
                                    "module" to it.module,
                                    "status" to it.status,
                                    "uploadedAt" to (it.uploadedAt ?: 0L),
                                    "driveUrl" to (it.driveFileId ?: "")
                                )
                            }
                            launch(Dispatchers.Main) { result.success(list) }
                        }
                    }

                    "retryFailedUploads" -> {
                        scope.launch {
                            AppDatabase.getInstance(this@MainActivity)
                                .uploadJobDao().resetFailed()
                            UploadWorker.schedule(this@MainActivity)
                            launch(Dispatchers.Main) {
                                result.success(mapOf("queued" to 1))
                            }
                        }
                    }

                    "setUploadPreferences" -> {
                        val wifiOnly = call.argument<Boolean>("wifiOnly") ?: true
                        // Re-schedule with new constraints
                        UploadWorker.schedule(this, wifiOnly)
                        result.success(mapOf("success" to true))
                    }

                    "signOutDrive" -> {
                        startService(action = MonitorService.ACTION_STOP)
                        result.success(mapOf("success" to true))
                    }

                    else -> result.notImplemented()
                }
            }
    }

    // --- MediaProjection ---

    private var pendingModules: ArrayList<String> = arrayListOf()

    private fun requestMediaProjection() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startService(pendingModules, resultCode, data)
                pendingProjectionResult?.success(mapOf("success" to true))
            } else {
                // Start without screen module
                val withoutScreen = ArrayList(pendingModules.filter { it != "SCREEN" })
                if (withoutScreen.isNotEmpty()) startService(withoutScreen, -1, null)
                pendingProjectionResult?.success(mapOf("success" to false))
            }
            pendingProjectionResult = null
            pendingModules = arrayListOf()
        }
    }

    // --- Helpers ---

    private fun startService(
        modules: ArrayList<String> = arrayListOf(),
        resultCode: Int = -1,
        projData: Intent? = null,
        action: String = MonitorService.ACTION_START
    ) {
        val intent = Intent(this, MonitorService::class.java).apply {
            this.action = action
            if (action == MonitorService.ACTION_START) {
                putStringArrayListExtra(MonitorService.EXTRA_MODULES, modules)
                putExtra(MonitorService.EXTRA_RESULT_CODE, resultCode)
                putExtra(MonitorService.EXTRA_INTENT_DATA, projData)
            }
        }
        startForegroundService(intent)
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MonitorService::class.java.name }
    }
}
