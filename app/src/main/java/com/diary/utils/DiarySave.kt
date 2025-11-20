package com.diary.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * 日记存储类：将日记以 JSON 文件存储至应用专属存储中。
 * 目录优先：context.getExternalFilesDir("diary")，否则：filesDir/diary。
 *
 * JSON 字段：
 * - id: String
 * - title: String?
 * - content: String
 * - createdAt: Long (epoch millis)
 * - updatedAt: Long (epoch millis)
 */
class DiarySave(private val context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val diaryDir: File by lazy {
        val externalDir = context.getExternalFilesDir("diary")
        if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
            externalDir
        } else {
            File(context.filesDir, "diary").apply { mkdirs() }
        }
    }

    private val charset: Charset = Charsets.UTF_8

    /**
     * 新增一篇日记，返回创建的文件。
     */
    fun addNew(content: String, title: String? = null): File {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("content", content)
            put("createdAt", now)
            put("updatedAt", now)
        }
        val file = fileOf(id)
        // 日记创建时不需要异步
        writeFileAtomic(file, json.toString())
        return file
    }

    /**
     * 按 id 保存（存在则更新，不存在则创建），返回文件。
     */
    suspend fun saveOrUpdate(id: String, content: String, title: String? = null): File {
        val file = fileOf(id)
        val now = System.currentTimeMillis()
        val json = if (file.exists()) {
            // 更新：保留 createdAt
            runCatching { JSONObject(file.readText(charset)) }.getOrNull() ?: JSONObject().put("id", id)
        } else {
            JSONObject().put("id", id).put("createdAt", now)
        }
        json.put("title", title)
        json.put("content", content)
        json.put("updatedAt", now)
        writeFileAtomicAsync(file, json.toString())
        return file
    }

    /**
     * 读取指定 id 的日记 JSON 字符串（不存在返回 null）。
     */
    fun loadAsJsonString(id: String): String? {
        val file = fileOf(id)
        if (!file.exists()) return null
        return runCatching { file.readText(charset) }.onFailure {
            Timber.e(it, "Failed to read diary: %s", id)
        }.getOrNull()
    }

    /**
     * 读取指定 id 的内容文本（若 JSON 不含 content 或文件不存在返回 null）。
     */
    fun loadContent(id: String): String? {
        val json = loadAsJsonString(id) ?: return null
        return runCatching { JSONObject(json).optString("content", null) }.getOrNull()
    }

    /**
     * 列出所有日记文件，按最后修改时间倒序。
     */
    fun listAllFiles(): List<File> {
        val files = diaryDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * 删除指定 id 的日记。
     */
    fun delete(id: String): Boolean {
        return fileOf(id).delete()
    }

    /**
     * 清空所有日记，返回删除数量。
     */
    fun clearAll(): Int {
        val files = diaryDir.listFiles() ?: return 0
        var count = 0
        files.forEach { f ->
            if (f.isFile && f.delete()) count++
        }
        return count
    }

    /**
     * 返回存储目录。
     */
    fun storageDir(): File = diaryDir

    /**
     * 根据 id 获取对应的文件。
     * @param id 日记 ID
     * @return 对应的文件
     */
    private fun fileOf(id: String): File = File(diaryDir, "$id.json")

    /**
     * 原子写入：先写入临时文件，再重命名覆盖目标文件。
     * @param target 目标文件
     * @param content 写入内容
     */
    private fun writeFileAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeText(content, charset)
            if (!tmp.renameTo(target)) {
                throw IOException("Failed to rename tmp to target: ${tmp.absolutePath} -> ${target.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Atomic write failed: %s", target.absolutePath)
            // 清理临时文件
            runCatching { if (tmp.exists()) tmp.delete() }
            throw e
        }
    }

    /**
     * 异步原子写入：使用协程实现异步非阻塞。
     * @param target 目标文件
     * @param content 写入内容
     */
    private suspend fun writeFileAtomicAsync(target: File, content: String) = withContext(Dispatchers.IO) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeText(content, charset)
            if (!tmp.renameTo(target)) {
                throw IOException("Failed to rename tmp to target: ${tmp.absolutePath} -> ${target.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Atomic write failed: %s", target.absolutePath)
            // 清理临时文件
            runCatching { if (tmp.exists()) tmp.delete() }
            throw e
        }
    }
}