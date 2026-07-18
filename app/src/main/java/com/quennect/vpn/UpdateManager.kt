package com.quennect.vpn

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL

data class UpdateInfo(
    val updateAvailable: Boolean,
    val remoteVersionCode: Int = 0,
    val remoteVersionName: String = ""
)

object UpdateManager {

    private const val METADATA_URL =
        "https://github.com/startech04/QuennectVPN/releases/download/latest/metadata.json"
    private const val APK_URL =
        "https://github.com/startech04/QuennectVPN/releases/download/latest/app-release.apk"

    fun checkForUpdate(context: Context): UpdateInfo {
        return try {
            val localVersionCode = getLocalVersionCode(context)
            val json = URL(METADATA_URL).readText()
            val obj = JSONObject(json)
            val remoteVersionCode = obj.optInt("versionCode", 0)
            val remoteVersionName = obj.optString("versionName", "unknown")

            if (remoteVersionCode > localVersionCode) {
                UpdateInfo(true, remoteVersionCode, remoteVersionName)
            } else {
                UpdateInfo(false)
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Update check failed", e)
            UpdateInfo(false)
        }
    }

    fun downloadAndInstall(
        context: Context,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(APK_URL))
            .setTitle("Quennect Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "QuennectVPN-update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = dm.enqueue(request)

        Thread {
            var cursor: Cursor? = null
            try {
                var downloading = true
                while (downloading) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    cursor = dm.query(query)
                    cursor?.moveToFirst()
                    val bytesDownloaded = cursor?.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    ) ?: 0
                    val bytesTotal = cursor?.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    ) ?: 1
                    val progress = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                    onProgress(progress)

                    val status = cursor?.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    ) ?: 0
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> downloading = false
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            onError("Download failed")
                            return@Thread
                        }
                    }
                    cursor?.close()
                    cursor = null
                    Thread.sleep(500)
                }

                val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "QuennectVPN-update.apk")
                installApk(context, apkFile)
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            } finally {
                cursor?.close()
            }
        }.start()
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun getLocalVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }
}
