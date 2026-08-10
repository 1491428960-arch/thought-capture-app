package com.thoughtcapture.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class VersionManager(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val repoDir: File
) {
    data class VersionInfo(
        val version: String,
        val versionCode: Int,
        val apkUrl: String,
        val changelog: String,
        val minVersionCode: Int
    )

    private var downloadId: Long = -1

    /**
     * 从 repo 根目录读取 version.json，对比本地 version_code。
     * @return VersionInfo 如果线上版本 > 本地，否则 null。
     */
    suspend fun checkForUpdate(): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val versionFile = File(repoDir, "version.json")
            if (!versionFile.exists()) return@withContext null

            val json = JSONObject(versionFile.readText())
            val info = VersionInfo(
                version = json.getString("version"),
                versionCode = json.getInt("version_code"),
                apkUrl = json.getString("apk_url"),
                changelog = json.optString("changelog", ""),
                minVersionCode = json.optInt("min_version_code", 0)
            )

            val localCode = prefs.remoteVersionCode
            if (info.versionCode > localCode) info else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 使用 DownloadManager 下载 APK。
     * @return 下载是否成功入队
     */
    fun startDownload(info: VersionInfo): Boolean {
        return try {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("想法捕捉 ${info.version}")
                .setDescription("正在下载更新…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "thought-capture-${info.version}.apk"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)

            downloadId = manager.enqueue(request)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 注册下载完成广播，下载完成后自动调起安装器。
     */
    fun registerDownloadReceiver(onComplete: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk()
                    onComplete()
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
        return receiver
    }

    /**
     * 通过 FileProvider 调起系统安装器。
     */
    fun installApk() {
        try {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = manager.query(query)
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    cursor.close()
                    val apkUri = Uri.parse(uri)
                    installFromUri(apkUri)
                    return
                }
                cursor.close()
            }
        } catch (_: Exception) { }

        // 降级：从已知路径安装
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "thought-capture-latest.apk"
        )
        if (file.exists()) {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            installFromUri(apkUri)
        }
    }

    private fun installFromUri(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /**
     * 标记当前 version_code 为已安装（安装成功后调用）。
     */
    fun markInstalled(versionCode: Int) {
        prefs.remoteVersionCode = versionCode
    }
}
