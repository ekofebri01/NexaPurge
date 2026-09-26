package com.example.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NexaNotificationHelper {
    private const val CHANNEL_CLEANER = "nexapurge_cleaner_channel"
    private const val CHANNEL_STORAGE = "nexapurge_storage_channel"
    
    private const val NOTIFICATION_CLEAN_ID = 1001
    private const val NOTIFICATION_STORAGE_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nameCleaner = "Pembersihan Otomatis"
            val descCleaner = "Notifikasi pembersihan berkala otomatis"
            val impCleaner = NotificationManager.IMPORTANCE_DEFAULT
            val channelCleaner = NotificationChannel(CHANNEL_CLEANER, nameCleaner, impCleaner).apply {
                description = descCleaner
            }

            val nameStorage = "Peringatan Penyimpanan"
            val descStorage = "Peringatan saat ruang penyimpanan hampir habis"
            val impStorage = NotificationManager.IMPORTANCE_HIGH
            val channelStorage = NotificationChannel(CHANNEL_STORAGE, nameStorage, impStorage).apply {
                description = descStorage
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channelCleaner)
            manager.createNotificationChannel(channelStorage)
        }
    }

    @SuppressLint("MissingPermission")
    fun showAutoCleanNotification(context: Context, cleanedBytes: Long, filesCount: Int) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanSizeStr = formatSize(cleanedBytes)
        val contentText = "NexaPurge berhasil membersihkan $filesCount file cache & log ($cleanSizeStr) latar belakang."

        val builder = NotificationCompat.Builder(context, CHANNEL_CLEANER)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Standard lightweight fallback icon
            .setContentTitle("NexaPurge: Pembersihan Otomatis")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_CLEAN_ID, builder.build())
        }
    }

    @SuppressLint("MissingPermission")
    fun showStorageWarningNotification(context: Context, freePercentage: Int, freeBytes: Long) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val freeSizeStr = formatSize(freeBytes)
        val contentText = "Ruang tersisa kurang dari $freePercentage% ($freeSizeStr dibebaskan). Bersihkan sampah sekarang!"

        val builder = NotificationCompat.Builder(context, CHANNEL_STORAGE)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Standard caution system icon
            .setContentTitle("Penyimpanan Hampir Penuh!")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_STORAGE_ID, builder.build())
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
