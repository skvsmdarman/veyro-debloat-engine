package com.veyro.debloatengine

import android.Manifest
import android.app.Application
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.veyro.debloatengine.R
import com.veyro.debloatengine.update.UpdateDownloadReceiver
import com.veyro.debloatengine.update.UpdateNotifier

/** Veyro Debloat Engine — application entry: channels + update receiver. */
class VeyroApp : Application() {

    private val downloadReceiver = UpdateDownloadReceiver()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onTerminate() {
        unregisterReceiver(downloadReceiver)
        super.onTerminate()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val updates = NotificationChannel(
            UpdateNotifier.CHANNEL_UPDATES,
            getString(R.string.update_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.update_notification_desc)
        }

        val status = NotificationChannel(
            "veyro_engine_status",
            getString(R.string.shizuku_notification_channel),
            NotificationManager.IMPORTANCE_MIN
        )

        manager.createNotificationChannel(updates)
        manager.createNotificationChannel(status)
    }
}
