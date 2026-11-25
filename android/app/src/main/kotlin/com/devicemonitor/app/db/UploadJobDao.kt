package com.devicemonitor.app.db

import androidx.room.*

@Dao
interface UploadJobDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(job: UploadJobEntity)

    @Query("SELECT * FROM upload_jobs WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<UploadJobEntity>

    @Query("SELECT * FROM upload_jobs ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistory(limit: Int, offset: Int): List<UploadJobEntity>

    @Query("UPDATE upload_jobs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE upload_jobs SET status = :status, uploadedAt = :ts, driveFileId = :fileId WHERE id = :id")
    suspend fun markDone(id: String, status: String = "DONE", ts: Long, fileId: String)

    @Query("UPDATE upload_jobs SET retryCount = :count WHERE id = :id")
    suspend fun updateRetryCount(id: String, count: Int)

    @Query("UPDATE upload_jobs SET status = 'PENDING', retryCount = 0 WHERE status = 'FAILED'")
    suspend fun resetFailed()

    @Query("SELECT COUNT(*) FROM upload_jobs WHERE status IN ('PENDING','IN_PROGRESS')")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM upload_jobs WHERE localPath = :path AND status = 'PENDING' LIMIT 1")
    suspend fun findPendingByPath(path: String): UploadJobEntity?
}
