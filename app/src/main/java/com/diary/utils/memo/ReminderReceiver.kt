package com.diary.utils.memo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * ReminderReceiver类用于接收备忘录提醒的广播，并显示通知。
 * 通过创建通知渠道（适用于Android O及以上版本）并构建通知，
 * 当用户点击通知时，会打开应用的主Activity。
 * 通知包含备忘录的标题和内容，提醒用户查看备忘录。
 */
class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "memo_reminder_channel"
        const val CHANNEL_NAME = "备忘录提醒"
    }

    /**
     * 接收广播并显示通知
     * @param context 上下文
     * @param intent 包含备忘录ID、标题和内容的Intent
     */
    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getIntExtra("memo_id", -1)
        val title = intent.getStringExtra("title") ?: "备忘录提醒"
        val content = intent.getStringExtra("content") ?: "您有一个备忘录提醒"

        if (memoId == -1) return

        createNotificationChannel(context)
        showNotification(context, memoId, title, content)
    }

    /**
     * 创建通知渠道（仅适用于Android O及以上版本）
     * @param context 上下文
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "备忘录提醒通知"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示通知
     * @param context 上下文
     * @param memoId 备忘录ID
     * @param title 通知标题
     * @param content 通知内容
     */
    private fun showNotification(context: Context, memoId: Int, title: String, content: String) {
        // 创建点击通知后的Intent（替换为您的应用主Activity）
        val intent = Intent(context, Class.forName("com.diary.MainActivity"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            memoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(memoId, notification)
    }
}