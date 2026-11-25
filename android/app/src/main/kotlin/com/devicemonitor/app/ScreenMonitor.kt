package com.devicemonitor.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.devicemonitor.app.db.AppDatabase
import com.devicemonitor.app.db.UploadJobEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScreenMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "ScreenMonitor"
    private val POLL_MS = 2_000L
    private val MIN_FILE_BYTES = 10_240L

    private val thread = HandlerThread("ScreenMonitorThread").apply { start() }
    private val handler = Handler(thread.looper)
    private val dao = AppDatabase.getInstance(context).uploadJobDao()

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var currentFile: File? = null
    private var lastPackage: String? = null
    private var isRunning = false

    private val usageStats by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val projManager by lazy {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun start(resultCode: Int, data: Intent) {
        mediaProjection = projManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection stopped")
                stopCurrentRecording()
            }
        }, handler)
        isRunning = true
        schedulePoll()
        Log.d(TAG, "Screen monitor started")
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        stopCurrentRecording()
        mediaProjection?.stop()
        mediaProjection = null
        thread.quitSafely()
        Log.d(TAG, "Screen monitor stopped")
    }

    private fun schedulePoll() {
        if (!isRunning) return
        handler.postDelayed({
            pollForegroundApp()
            schedulePoll()
        }, POLL_MS)
    }

    private fun pollForegroundApp() {
        val end = System.currentTimeMillis()
        val stats = usageStats.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            end - 5_000, end
        )
        val current = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return

        if (current == context.packageName) return   // our own app
        if (current == lastPackage) return            // same app, no change

        onAppSwitch(current)
    }

    private fun onAppSwitch(newPackage: String) {
        stopCurrentRecording()
        startRecording(newPackage)
        lastPackage = newPackage
    }

    private fun startRecording(packageName: String) {
        val outFile = buildOutputFile(packageName)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getMetrics(metrics)

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()

        try {
            recorder.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(metrics.widthPixels, metrics.heightPixels)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(4_000_000)
                setOutputFile(outFile.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                metrics.widthPixels, metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface, null, handler
            )

            recorder.start()
            mediaRecorder = recorder
            currentFile = outFile
            Log.d(TAG, "Recording started: $packageName")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            recorder.release()
            outFile.delete()
        }
    }

    private fun stopCurrentRecording() {
        val file = currentFile ?: return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Stop error (short segment): ${e.message}")
            file.delete()
            cleanupRecorder()
            return
        }
        virtualDisplay?.release()
        virtualDisplay = null
        cleanupRecorder()

        if (file.exists() && file.length() > MIN_FILE_BYTES) {
            enqueueUpload(file)
        } else {
            file.delete()
        }
        currentFile = null
    }

    private fun cleanupRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun buildOutputFile(pkg: String): File {
        val dir = File(context.cacheDir, "screen").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US).format(Date())
        val safePkg = pkg.replace(".", "_").take(40)
        return File(dir, "screen_${safePkg}_$ts.mp4")
    }

    private fun enqueueUpload(file: File) {
        scope.launch(Dispatchers.IO) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val deviceName = Build.MODEL.replace(" ", "_")
            val job = UploadJobEntity(
                id = UUID.randomUUID().toString(),
                localPath = file.absolutePath,
                drivePath = "DeviceMonitor/screen/$deviceName/$date/${file.name}",
                module = "SCREEN",
                status = "PENDING"
            )
            dao.insert(job)
            UploadWorker.schedule(context)
            Log.d(TAG, "Enqueued upload: ${file.name}")
        }
    }
}
