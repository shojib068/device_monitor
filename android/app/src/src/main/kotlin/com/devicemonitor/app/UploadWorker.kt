package com.devicemonitor.app

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.*
import com.devicemonitor.app.db.AppDatabase
import com.devicemonitor.app.drive.DriveUploader
import com.devicemonitor.app.drive.UploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class UploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val TAG = "UploadWorker"
    private val MAX_RETRIES = 5
    private val dao = AppDatabase.getInstance(ctx).uploadJobDao()
    private val uploader = DriveUploader(ctx)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pending = dao.getPending()
        Log.d(TAG, "Processing ${pending.size} pending jobs")

        for (job in pending) {
            dao.updateStatus(job.id, "IN_PROGRESS")

            val result = uploader.upload(
                localPath = job.localPath,
                drivePath = job.drivePath,
                metadata = job.driveMetadata ?: ""
            )

            when (result) {
                is UploadResult.Success -> {
                    dao.markDone(
                        id = job.id,
                        ts = System.currentTimeMillis(),
                        fileId = result.fileId
                    )
                    // Delete local file after successful upload
                    val local = File(job.localPath)
                    if (local.exists()) local.delete()
                    Log.d(TAG, "Done: ${job.filename}")
                    notifySuccess(job.filename)
                }
                is UploadResult.AuthError -> {
                    Log.w(TAG, "Auth error for ${job.filename}")
                    dao.updateStatus(job.id, "PENDING")
                    return@withContext Result.retry()
                }
                is UploadResult.QuotaExceeded -> {
                    Log.e(TAG, "Quota exceeded")
                    dao.updateStatus(job.id, "PENDING")
                    notifyQuotaExceeded()
                    return@withContext Result.failure()
                }
                is UploadResult.TransientError -> {
                    val newCount = job.retryCount + 1
                    if (newCount >= MAX_RETRIES) {
                        dao.updateStatus(job.id, "FAILED")
                        Log.e(TAG, "Giving up on ${job.filename} after $MAX_RETRIES retries")
                    } else {
                        dao.updateRetryCount(job.id, newCount)
                        dao.updateStatus(job.id, "PENDING")
                        Log.w(TAG, "Retry $newCount for ${job.filename}")
                    }
                    return@withContext Result.retry()
                }
            }
        }
        Result.success()
    }

    private fun notifySuccess(filename: String) {
        // Notification posted via MonitorService event channel
        MonitorService.sendEvent(
            applicationContext,
            "uploadComplete",
            mapOf("filename" to filename)
        )
    }

    private fun notifyQuotaExceeded() {
        MonitorService.sendEvent(
            applicationContext,
            "quotaExceeded",
            emptyMap()
        )
    }

    companion object {
        const val WORK_NAME = "device_monitor_upload"

        fun schedule(context: Context, wifiOnly: Boolean = true) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
