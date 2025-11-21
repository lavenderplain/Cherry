package com.diary.utils

import android.content.Context
import com.diary.cherry.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.util.*

class ReminderSave(private val context: Context) {
    private val reminderDir: File by lazy {
        val externalDir = context.getExternalFilesDir("reminders")
        if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
            externalDir
        } else {
            File(context.filesDir, "reminders").apply { mkdirs() }
        }
    }

    private val charset: Charset = Charsets.UTF_8

    /**
     * 保存或更新备忘录
     */
    suspend fun saveOrUpdate(reminder: Reminder): File {
        val file = fileOf(reminder.id)
        val json = JSONObject().apply {
            put("id", reminder.id)
            put("title", reminder.title)
            put("content", reminder.content)
            put("reminderTime", reminder.reminderTime)
            put("isCompleted", reminder.isCompleted)
            put("createdAt", reminder.createdAt)
            put("updatedAt", System.currentTimeMillis())
        }

        writeFileAtomicAsync(file, json.toString())
        return file
    }

    /**
     * 删除备忘录
     */
    fun delete(id: String): Boolean {
        return fileOf(id).delete()
    }

    /**
     * 标记为完成
     */
    suspend fun markAsCompleted(id: String) {
        val reminder = load(id) ?: return
        val updatedReminder = reminder.copy(isCompleted = true, updatedAt = System.currentTimeMillis())
        saveOrUpdate(updatedReminder)
    }

    /**
     * 标记为未完成
     */
    suspend fun markAsIncomplete(id: String) {
        val reminder = load(id) ?: return
        val updatedReminder = reminder.copy(isCompleted = false, updatedAt = System.currentTimeMillis())
        saveOrUpdate(updatedReminder)
    }

    /**
     * 切换完成状态
     */
    suspend fun toggleCompletionStatus(id: String): Boolean {
        val reminder = load(id) ?: return false
        val updatedReminder = reminder.copy(
            isCompleted = !reminder.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        saveOrUpdate(updatedReminder)
        return updatedReminder.isCompleted
    }

    /**
     * 加载单个备忘录
     */
    fun load(id: String): Reminder? {
        val jsonString = loadAsJsonString(id) ?: return null
        return try {
            val json = JSONObject(jsonString)
            Reminder(
                id = json.optString("id", id),
                title = json.optString("title", ""),
                content = json.optString("content", ""),
                reminderTime = json.optLong("reminderTime"),
                isCompleted = json.optBoolean("isCompleted", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse reminder: $id")
            null
        }
    }

    /**
     * 获取所有备忘录
     */
    fun getAllReminders(): List<Reminder> {
        val files = listAllFiles()
        return files.mapNotNull { file ->
            load(file.nameWithoutExtension)
        }.sortedByDescending { it.reminderTime }
    }

    /**
     * 获取待处理的备忘录（未完成且未过期）
     */
    fun getPendingReminders(): List<Reminder> {
        return getAllReminders().filter { !it.isCompleted && it.reminderTime > System.currentTimeMillis() }
    }

    /**
     * 获取过期的备忘录
     */
    fun getOverdueReminders(): List<Reminder> {
        return getAllReminders().filter { it.isOverdue() }
    }

    /**
     * 获取即将到期的备忘录
     */
    fun getUpcomingReminders(withinMinutes: Long = 60): List<Reminder> {
        return getAllReminders().filter { it.isUpcoming(withinMinutes) }
    }

    /**
     * 获取已完成的备忘录
     */
    fun getCompletedReminders(): List<Reminder> {
        return getAllReminders().filter { it.isCompleted }
    }

    private fun loadAsJsonString(id: String): String? {
        val file = fileOf(id)
        if (!file.exists()) return null
        return runCatching { file.readText(charset) }.getOrNull()
    }

    private fun listAllFiles(): List<File> {
        val files = reminderDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }
    }

    private fun fileOf(id: String): File = File(reminderDir, "$id.json")

    private suspend fun writeFileAtomicAsync(target: File, content: String) = withContext(Dispatchers.IO) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeText(content, charset)
            if (!tmp.renameTo(target)) {
                throw Exception("Failed to rename tmp to target")
            }
        } catch (e: Exception) {
            Timber.e(e, "Atomic write failed: ${target.absolutePath}")
            runCatching { if (tmp.exists()) tmp.delete() }
            throw e
        }
    }
}