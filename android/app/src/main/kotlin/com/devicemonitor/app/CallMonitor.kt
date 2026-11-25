package com.devicemonitor.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.devicemonitor.app.db.AppDatabase
import com.devicemonitor.app.db.UploadJobEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CallMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "CallMonitor"
    private val dao = AppDatabase.getInstance(context).uploadJobDao()

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var callStartTime = 0L
    private var isRecording = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                TelephonyManager.EXTRA_STATE_OFFHOOK -> onCallStarted()
                TelephonyManager.EXTRA_STATE_IDLE    -> onCallEnded()
                // RINGING: ignored — only record once answered
            }
        }
    }

    fun start() {
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        Log.d(TAG, "Call monitor started")
    }

    fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        if (isRecording) onCallEnded()
        Log.d(TAG, "Call monitor stopped")
    }

    private fun onCallStarted() {
        if (isRecording) return
        callStartTime = System.currentTimeMillis()
        val outFile = buildOutputFile()

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentFile = outFile
            isRecording = true
            Log.d(TAG, "Call recording started")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call recording: ${e.message}")
            recorder.release()
            outFile.delete()
        }
    }

    private fun onCallEnded() {
        if (!isRecording) return
        val durationSec = (System.currentTimeMillis() - callStartTime) / 1_000
        isRecording = false

        val file = currentFile
        currentFile = null

        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Short call, discarding: ${e.message}")
            file?.delete()
            cleanupRecorder()
            return
        }
        cleanupRecorder()

        if (file == null || !file.exists()) return

        // Rename to include duration
        val dir = file.parentFile ?: return
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US).format(Date(callStartTime))
        val namedFile = File(dir, "call_${ts}_${durationSec}s.m4a")
        file.renameTo(namedFile)

        enqueueUpload(namedFile, durationSec)
        Log.d(TAG, "Call ended, duration ${durationSec}s")
    }

    private fun cleanupRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun buildOutputFile(): File {
        val dir = File(context.cacheDir, "calls").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US).format(Date())
        return File(dir, "call_tmp_$ts.m4a")
    }

    private fun enqueueUpload(file: File, durationSec: Long) {
        scope.launch(Dispatchers.IO) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val deviceName = Build.MODEL.replace(" ", "_")
            val job = UploadJobEntity(
                id = UUID.randomUUID().toString(),
                localPath = file.absolutePath,
                drivePath = "DeviceMonitor/calls/$deviceName/$date/${file.name}",
                module = "CALL",
                status = "PENDING",
                driveMetadata = "duration=${durationSec}s"
            )
            dao.insert(job)
            UploadWorker.schedule(context)
        }
    }
}
