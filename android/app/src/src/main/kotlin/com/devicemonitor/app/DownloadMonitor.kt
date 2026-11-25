package com.devicemonitor.app

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import com.devicemonitor.app.db.AppDatabase
import com.devicemonitor.app.db.UploadJobEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DownloadMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "DownloadMonitor"
    private val MAX_BYTES = 500L * 1024 * 1024   // 500 MB cap
    private val dao = AppDatabase.getInstance(context).uploadJobDao()

    private var observer: FileObserver? = null

    fun start() {
        val downloadsDir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(downloadsDir, CLOSE_WRITE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path != null) onDownload(File(downloadsDir, path))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(downloadsDir.absolutePath, CLOSE_WRITE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path != null) onDownload(File(downloadsDir, path))
                }
            }
        }

        observer?.startWatching()
        Log.d(TAG, "Download monitor watching: $downloadsDir")
    }

    fun stop() {
        observer?.stopWatching()
        observer = null
        Log.d(TAG, "Download monitor stopped")
    }

    private fun onDownload(file: File) {
        scope.launch(Dispatchers.IO) {
            if (!file.exists()) return@launch
            val size = file.length()
            if (size == 0L) return@launch   // still writing

            if (size > MAX_BYTES) {
                Log.w(TAG, "File too large (${size / 1024 / 1024} MB): ${file.name}")
                MonitorService.sendEvent(
                    context, "uploadFailed",
                    mapOf("filename" to file.name, "error" to "File exceeds 500 MB limit")
                )
                return@launch
            }

            // De-duplicate: skip if already queued
            val existing = dao.findPendingByPath(file.absolutePath)
            if (existing != null) {
                Log.d(TAG, "Already queued: ${file.name}")
                return@launch
            }

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val deviceName = Build.MODEL.replace(" ", "_")
            val job = UploadJobEntity(
                id = UUID.randomUUID().toString(),
                localPath = file.absolutePath,
                drivePath = "DeviceMonitor/downloads/$deviceName/$date/${file.name}",
                module = "DOWNLOAD",
                status = "PENDING"
            )
            dao.insert(job)
            UploadWorker.schedule(context)
            Log.d(TAG, "Enqueued download: ${file.name} (${size / 1024} KB)")
        }
    }
}
