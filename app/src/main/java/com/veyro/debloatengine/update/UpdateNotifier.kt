package com.veyro.debloatengine.update

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.veyro.debloatengine.MainActivity
import com.veyro.debloatengine.R

/**
 * Heads-up notifications for the GitHub auto-update system.
 * Uses only androidx.core (NotificationCompat) — no appcompat needed.
 */
object UpdateNotifier {

    const val CHANNEL_UPDATES = "veyro_updates"
    const val NOTIFICATION_UPDATE_ID = 4201

    fun notifyUpdateAvailable(context: Context, versionName: String) {
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("show_update_dialog", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_veyro)
            .setContentTitle("Veyro Engine Update Available (v$versionName)")
            .setContentText("Tap to review and install the new release.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        post(context, NOTIFICATION_UPDATE_ID, notification)
    }

    fun notifyDownloadComplete(context: Context) {
        val tapIntent = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).apply {
                putExtra("install_update", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_veyro)
            .setContentTitle("Veyro update downloaded")
            .setContentText("Tap to start the installer.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        post(context, NOTIFICATION_UPDATE_ID + 1, notification)
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return // Notifications not granted; the in-app banner still shows.
        }
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) { }
    }
}
