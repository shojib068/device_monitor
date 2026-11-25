package com.devicemonitor.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_jobs")
data class UploadJobEntity(
    @PrimaryKey val id: String,
    val localPath: String,
    val drivePath: String,
    val module: String,
    val status: String,          // PENDING | IN_PROGRESS | DONE | FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val uploadedAt: Long? = null,
    val driveFileId: String? = null,
    val driveMetadata: String? = null,
    val filename: String = localPath.substringAfterLast("/")
)
