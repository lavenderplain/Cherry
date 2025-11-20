package com.diary.utils.memo

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.diary.utils.memo.Memo.ReminderType

/**
 * MemoManager类用于管理备忘录的创建、提醒设置、取消提醒和数据访问。
 * 提供了添加备忘录、设置定时提醒和倒计时提醒的方法，
 * 并使用协程处理异步操作，确保不会阻塞主线程。
 */
class MemoInterface(private val context: Context) {
    private val memoDao = AppDatabase.getInstance(context).memoDao()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 添加备忘录并设置提醒
     * @param title 备忘录标题
     * @param content 备忘录内容
     * @param reminderType 提醒类型
     * @param reminderTime 提醒时间戳（毫秒）
     * @return Result封装的备忘录对象或异常
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    suspend fun addMemo(
        title: String,
        content: String,
        reminderType: ReminderType,
        reminderTime: Long
    ): Result<Memo> = withContext(Dispatchers.IO) {
        try {
            val memo = Memo(
                title = title,
                content = content,
                reminderType = reminderType,
                reminderTime = reminderTime
            )

            val id = memoDao.insert(memo)
            val savedMemo = memo.copy(id = id.toInt())

            // 设置提醒
            if (reminderTime > 0) {
                setReminder(savedMemo)
            }

            Result.success(savedMemo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设置定时提醒
     * @param title 备忘录标题
     * @param content 备忘录内容
     * @param reminderTime 提醒时间戳（毫秒）
     * @return Result封装的备忘录对象或异常
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    suspend fun setTimedReminder(
        title: String,
        content: String,
        reminderTime: Long
    ): Result<Memo> {
        return addMemo(title, content, ReminderType.TIMED, reminderTime)
    }

    /**
     * 设置倒计时提醒
     * @param title 备忘录标题
     * @param content 备忘录内容
     * @param countdownMillis 倒计时毫秒数
     * @return Result封装的备忘录对象或异常
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    suspend fun setCountdownReminder(
        title: String,
        content: String,
        countdownMillis: Long
    ): Result<Memo> {
        val reminderTime = System.currentTimeMillis() + countdownMillis
        return addMemo(title, content, ReminderType.COUNTDOWN, reminderTime)
    }

    /**
     * 设置提醒
     * @param memo 备忘录对象
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun setReminder(memo: Memo) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("memo_id", memo.id)
            putExtra("title", memo.title)
            putExtra("content", memo.content)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            memo.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 使用setExactAndAllowWhileIdle确保提醒准时触发
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            memo.reminderTime,
            pendingIntent
        )
    }

    /**
     * 取消提醒
     * @param memoId 备忘录ID
     */
    fun cancelReminder(memoId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            memoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * 获取所有备忘录
     * @return 包含所有备忘录的Flow列表
     */
    fun getAllMemos() = memoDao.getAllMemos()

    /**
     * 根据ID获取备忘录
     * @param id 备忘录ID
     * @return Result封装的备忘录对象或异常
     */
    suspend fun getMemoById(id: Int): Result<Memo?> = withContext(Dispatchers.IO) {
        try {
            val memo = memoDao.getMemoById(id)
            Result.success(memo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将备忘录标记为已完成
     * @param memoId 备忘录ID
     * @return Result封装的操作结果或异常
     */
    suspend fun markAsCompleted(memoId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            memoDao.markAsCompleted(memoId)
            cancelReminder(memoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除备忘录
     * @param memoId 备忘录ID
     * @return Result封装的操作结果或异常
     */
    suspend fun deleteMemo(memoId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val memo = memoDao.getMemoById(memoId)
            memo?.let {
                memoDao.delete(it)
                cancelReminder(memoId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 异步添加备忘录并设置提醒
     * @param title 备忘录标题
     * @param content 备忘录内容
     * @param reminderType 提醒类型
     * @param reminderTime 提醒时间戳（毫秒）
     * @param callback 操作结果回调
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun addMemoAsync(
        title: String,
        content: String,
        reminderType: ReminderType,
        reminderTime: Long,
        callback: (Result<Memo>) -> Unit
    ) {
        coroutineScope.launch {
            val result = addMemo(title, content, reminderType, reminderTime)
            callback(result)
        }
    }

    /**
     * 异步设置定时提醒
     * @param title 备忘录标题
     * @param content 备忘录内容
     * @param reminderTime 提醒时间戳（毫秒）
     * @param callback 操作结果回调
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun setTimedReminderAsync(
        title: String,
        content: String,
        reminderTime: Long,
        callback: (Result<Memo>) -> Unit
    ) {
        coroutineScope.launch {
            val result = setTimedReminder(title, content, reminderTime)
            callback(result)
        }
    }

    /**
     * 异步设置倒计时提醒
     * @param title 备忘录标题
     * @param content 备忘录内容
     * @param countdownMillis 倒计时毫秒数
     * @param callback 操作结果回调
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun setCountdownReminderAsync(
        title: String,
        content: String,
        countdownMillis: Long,
        callback: (Result<Memo>) -> Unit
    ) {
        coroutineScope.launch {
            val result = setCountdownReminder(title, content, countdownMillis)
            callback(result)
        }
    }
}