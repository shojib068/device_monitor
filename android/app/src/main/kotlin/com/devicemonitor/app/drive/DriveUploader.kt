package com.devicemonitor.app.drive

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import java.io.File

sealed class UploadResult {
    data class Success(val fileId: String) : UploadResult()
    data class AuthError(val e: Exception) : UploadResult()
    data class QuotaExceeded(val e: Exception) : UploadResult()
    data class TransientError(val e: Exception) : UploadResult()
}

class DriveUploader(private val context: Context) {

    private val TAG = "DriveUploader"
    private val folderCache = mutableMapOf<String, String>()

    private fun buildService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("DeviceMonitor").build()
    }

    fun upload(localPath: String, drivePath: String, metadata: String = ""): UploadResult {
        val file = File(localPath)
        if (!file.exists()) {
            return UploadResult.TransientError(Exception("File not found: $localPath"))
        }

        return try {
            val drive = buildService()
                ?: return UploadResult.AuthError(Exception("Not signed in"))

            val folderPath = drivePath.substringBeforeLast("/")
            val folderId = ensureFolder(drive, folderPath)

            val driveFile = DriveFile().apply {
                name = file.name
                parents = listOf(folderId)
                if (metadata.isNotEmpty()) description = metadata
            }

            val mimeType = mimeTypeFor(file.extension)
            val content = FileContent(mimeType, file)

            val created = drive.files()
                .create(driveFile, content)
                .setFields("id")
                .execute()

            Log.d(TAG, "Uploaded ${file.name} → Drive id ${created.id}")
            UploadResult.Success(created.id)

        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            Log.e(TAG, "Drive API error ${e.statusCode}: ${e.message}")
            when (e.statusCode) {
                401, 403 -> UploadResult.AuthError(e)
                507       -> UploadResult.QuotaExceeded(e)
                else      -> UploadResult.TransientError(e)
            }
        } catch (e: java.net.UnknownHostException) {
            UploadResult.TransientError(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error: ${e.message}")
            UploadResult.TransientError(e)
        }
    }

    private fun ensureFolder(drive: Drive, path: String): String {
        // path example: "DeviceMonitor/screen/Pixel9/2026-11-23"
        folderCache[path]?.let { return it }

        val segments = path.split("/")
        var parentId = "root"

        for (segment in segments) {
            val cacheKey = "$parentId/$segment"
            parentId = folderCache[cacheKey] ?: run {
                val id = findOrCreateFolder(drive, segment, parentId)
                folderCache[cacheKey] = id
                id
            }
        }
        folderCache[path] = parentId
        return parentId
    }

    private fun findOrCreateFolder(drive: Drive, name: String, parentId: String): String {
        val result = drive.files().list()
            .setQ("name='$name' and '$parentId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setFields("files(id)")
            .execute()

        result.files.firstOrNull()?.id?.let { return it }

        val folder = DriveFile().apply {
            this.name = name
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(parentId)
        }
        return drive.files().create(folder).setFields("id").execute().id
    }

    private fun mimeTypeFor(ext: String): String = when (ext.lowercase()) {
        "mp4"  -> "video/mp4"
        "m4a"  -> "audio/mp4"
        "mp3"  -> "audio/mpeg"
        "pdf"  -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png"  -> "image/png"
        "zip"  -> "application/zip"
        else   -> "application/octet-stream"
    }
}
