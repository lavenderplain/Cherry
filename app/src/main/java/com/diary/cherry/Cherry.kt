package com.diary.cherry

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.diary.cherry.model.Reminder
import com.diary.utils.ReminderSave
import com.diary.utils.ReleaseTree
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class Cherry : Application() {
    val cherryScope = ProcessLifecycleOwner.get().lifecycleScope
    lateinit var releaseTree: ReleaseTree
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val REMINDER_NOTIFICATION_ID = 1001
        const val REMINDER_CHANNEL_ID = "diary_reminder"
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化 Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        releaseTree = ReleaseTree(this, BuildConfig.LOG_MIN_LEVEL, cherryScope)
        Timber.plant(releaseTree)

        // 初始化通知管理器
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        createNotificationChannel()

        // 启动提醒检查服务
        startReminderService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "备忘录提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "备忘录提醒通知"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startReminderService() {
        cherryScope.launch {
            while (true) {
                checkReminders()
                delay(30000) // 每30秒检查一次
            }
        }
    }

    private suspend fun checkReminders() {
        val reminderSave = ReminderSave(this@Cherry)
        val upcomingReminders = reminderSave.getUpcomingReminders(5) // 5分钟内的提醒
        
        upcomingReminders.forEach { reminder ->
            showReminderNotification(reminder)
        }
    }

    private fun showReminderNotification(reminder: Reminder) {
        try {
            // 创建通知
            val intent = Intent(this, CherryActivity::class.java).apply {
                putExtra("OPEN_REMINDER_ID", reminder.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    this, reminder.id.hashCode(), intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    this, reminder.id.hashCode(), intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val notification = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⏰ ${reminder.title}")
                .setContentText(reminder.content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .build()

            notificationManager.notify(reminder.id.hashCode(), notification)
            Timber.i("Showing reminder: ${reminder.title}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show reminder notification")
        }
    }
}