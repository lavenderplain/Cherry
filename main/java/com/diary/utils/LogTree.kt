package com.diary.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReleaseTree(
    private val context: Context,
    private val minPriority: Int,
    private val coroutineScope: CoroutineScope,
) : Timber.Tree() {
    private val logDir: File by lazy {
        // 优先使用外存，否则使用内存
        // 外存存储位置：/storage/emulated/0/Android/data/com.example.myapplication/files/logs
        // 内存存储位置：/data/data/com.example.myapplication
        val externalDir = context.getExternalFilesDir("logs")
        // Timber.log(Log.INFO, "Log directory: ${externalDir?.absolutePath}")
        if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())){
            externalDir
        } else {
            File(context.filesDir, "logs").apply { mkdirs() }
        }
    }

    // 最大日志文件大小 (4MB)
    private val MAX_LOG_SIZE = 4 * 1024 * 1024
    // 最多保留的日志文件数
    // 保留扩展存储日志的能力
    private val MAX_LOG_FILES = 2

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 只记录 ERROR 及以上级别的日志
        if (priority < minPriority) return

        // 通过协程在 IO 线程中写入日志文件
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 追加记录模式
                val logFile = getCurrentLogFile()
                val logEntry = formatLogEntry(priority, tag, message, t)

                FileOutputStream(logFile, true).use { fos ->
                    fos.write(logEntry.toByteArray())
                }

                // 清理旧日志
                cleanOldLogs()
            } catch (e: Exception) {
                // 日志写入失败时，记录到系统日志
                Timber.tag("ReleaseTree").e(e, "Failed to write log to file")
            }
        }
    }

    /*
     * 获取当前日志文件
     */
    private fun getCurrentLogFile(): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val logFiles = logDir.listFiles { file ->
            file.name.startsWith("error_log_$today") && file.isFile
        }?.sortedBy { it.name }

        if (!logFiles.isNullOrEmpty()) {
            val lastFile = logFiles.last()
            if (lastFile.length() < MAX_LOG_SIZE) {
                return lastFile
            }
            // 文件过大，创建新文件
            return createNewLogFile(today, logFiles.size + 1)
        }
        return createNewLogFile(today, 1)
    }

    /*
     * 创建新的日志文件
     */
    private fun createNewLogFile(date: String, index: Int): File {
        return File(logDir, "error_log_${date}_$index.txt").apply {
            createNewFile()
        }
    }

    /*
     * 格式化日志条目
     */
    private fun formatLogEntry(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val level = when (priority) {
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }

        val sb = StringBuilder()
        sb.append("[$timestamp] [$level] ${tag ?: "NO_TAG"}: $message\n")

        t?.let {
            sb.append("Exception: ${it.javaClass.name}: ${it.message}\n")
            it.stackTrace.forEach { element ->
                sb.append("\tat $element\n")
            }

            // 记录可能的异常原因
            var cause = it.cause
            while (cause != null && cause != it) {
                sb.append("Caused by: ${cause.javaClass.name}: ${cause.message}\n")
                cause.stackTrace.forEach { element ->
                    sb.append("\tat $element\n")
                }
                cause = cause.cause
            }
        }
        return sb.toString()
    }

    /*
     * 清理旧日志
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun cleanOldLogs() {
        val logFiles = logDir.listFiles()?.filter { it.isFile } ?: return
        if (logFiles.size <= MAX_LOG_FILES) return

        // 按时间排序（最旧的在最前面）
        val sortedFiles = logFiles.sortedBy { it.lastModified() }.toMutableList()

        while (sortedFiles.size > MAX_LOG_FILES) {
            sortedFiles.firstOrNull()?.delete()
            sortedFiles.removeFirst()
        }
    }

    /*
     * 获取所有日志文件（便于提交到服务器）
     * 待完善
     */
    fun getLogFile(): List<File> {
        return logDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    /*
     * 清除所有日志文件
     * 用于清除应用缓存
     */
    fun clearLogs() {
        logDir.listFiles()?.forEach { it.delete() }
    }

    override fun toString(): String {
        return "ReleaseTree(logDir=$logDir)"
    }
}