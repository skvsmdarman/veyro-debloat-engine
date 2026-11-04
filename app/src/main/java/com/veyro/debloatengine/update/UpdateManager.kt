package com.veyro.debloatengine.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.veyro.debloatengine.BuildConfig
import com.veyro.debloatengine.data.model.UpdateInfo
import com.veyro.debloatengine.shizuku.ShizukuEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Auto-Update System.
 *
 * Fetches `update.json` from the public repository, compares the remote
 * [UpdateInfo.versionCode] against [BuildConfig.VERSION_CODE], downloads the
 * APK through the system DownloadManager, and triggers the installer either
 * through FileProvider (ACTION_VIEW) or silently via Shizuku (`pm install -r`).
 */
object UpdateManager {

    const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/skvsmdarman/veyro-debloat-engine/main/update.json"

    const val APK_FILE_NAME = "veyro-debloat-engine-update.apk"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Fetches and parses update.json. Returns the [UpdateInfo] when a newer
     * version exists, or null when the app is already up to date / offline /
     * the manifest is unreachable.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(UPDATE_JSON_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Cache-Control", "no-cache")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val info = json.decodeFromString<UpdateInfo>(body)
            if (info.versionCode > BuildConfig.VERSION_CODE) info else null
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    // Download (system DownloadManager, real-time progress)
    // ------------------------------------------------------------------
    fun startDownload(context: Context, update: UpdateInfo): Long {
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl)).apply {
            setTitle("Veyro Debloat Engine v${update.versionName}")
            setDescription("Downloading update…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    /**
     * Queries the live progress of a download.
     * Returns (downloadedBytes, totalBytes) or (-1, -1) when unknown/finished.
     */
    fun queryProgress(context: Context, downloadId: Long): Pair<Long, Long> {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        return dm.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return@use Pair(-1L, -1L)
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIdx >= 0 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                return@use Pair(1L, 1L) // sentinel: complete
            }
            val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val bytes = if (bytesIdx >= 0) cursor.getLong(bytesIdx) else -1L
            val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L
            Pair(bytes, total)
        }
    }

    fun downloadStatus(context: Context, downloadId: Long): Int {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return DownloadManager.STATUS_FAILED
            val idx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            return if (idx >= 0) cursor.getInt(idx) else DownloadManager.STATUS_FAILED
        }
    }

    fun downloadedApkFile(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return File(dir, APK_FILE_NAME)
    }

    // ------------------------------------------------------------------
    // Installer triggers
    // ------------------------------------------------------------------

    /** Opens the system package installer via FileProvider. */
    fun openInstaller(context: Context): Boolean {
        val apk = downloadedApkFile(context)
        if (!apk.exists()) return false
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 1-tap silent in-place install via Shizuku: `pm install -r <apk>`.
     * Only call when Shizuku is READY.
     */
    suspend fun silentInstallViaShizuku(context: Context) = withContext(Dispatchers.IO) {
        val apk = downloadedApkFile(context)
        if (!apk.exists()) {
            com.veyro.debloatengine.data.model.CommandResult(-1, "", "APK not found on disk.")
        } else {
            ShizukuEngine.silentInstall(apk.absolutePath)
        }
    }
}

/**
 * Fired by the DownloadManager when the update APK finishes downloading.
 * Posts the heads-up "tap to install" path — the UI also observes progress
 * directly, so this receiver only handles the background case.
 */
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            UpdateNotifier.notifyDownloadComplete(context)
        }
    }
}
