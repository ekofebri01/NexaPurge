package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.NexaPurgeDatabase
import com.example.data.repository.NexaPurgeRepository
import com.example.data.repository.JunkCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoCleanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("nexapurge_prefs", Context.MODE_PRIVATE)
        val autoCleanEnabled = sharedPrefs.getBoolean("auto_clean_enabled", false)
        val alertThreshold = sharedPrefs.getInt("alert_threshold_percent", 15)

        // Run background coroutine scope
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = NexaPurgeDatabase.getDatabase(context)
                val repository = NexaPurgeRepository(
                    context = context,
                    quarantinedFileDao = db.quarantinedFileDao(),
                    cleanHistoryDao = db.cleanHistoryDao()
                )

                // 1. Perform Storage capacity threshold check
                val stats = repository.getStorageStats(alertThresholdPercent = alertThreshold)
                if (stats.thresholdAlert) {
                    val occupiedRatio = (stats.occupiedBytes.toDouble() / stats.totalBytes.toDouble()) * 100
                    val freePercentage = (100 - occupiedRatio).toInt()
                    NexaNotificationHelper.showStorageWarningNotification(
                        context = context,
                        freePercentage = freePercentage,
                        freeBytes = stats.freeBytes
                    )
                }

                // 2. Perform Automatic Cleanup if configured
                if (autoCleanEnabled) {
                    // Gather scan list
                    val scannedList = repository.scanJunkFiles(includeRoot = false)
                    // Auto close temp logs and general caches
                    val autoPurgerTargets = scannedList.filter {
                        it.category == JunkCategory.TEMP_FILES || it.category == JunkCategory.APP_CACHE
                    }

                    if (autoPurgerTargets.isNotEmpty()) {
                        val result = repository.executeClean(
                            selectedFiles = autoPurgerTargets,
                            isRootClean = false,
                            isAutoClean = true
                        )
                        // Trigger successful clean feedback notifier
                        NexaNotificationHelper.showAutoCleanNotification(
                            context = context,
                            cleanedBytes = result.first,
                            filesCount = result.second
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun schedulePeriodicClean(context: Context, intervalHours: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, AutoCleanReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                1882,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val intervalMs = intervalHours * 60 * 60 * 1000L
            val triggerTime = System.currentTimeMillis() + intervalMs

            try {
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    intervalMs,
                    pendingIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun cancelPeriodicClean(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, AutoCleanReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                1882,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
