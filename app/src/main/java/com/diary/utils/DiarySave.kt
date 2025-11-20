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
import java.util.*

/**
 * 日记存储类：扩展支持表情、手写、音乐功能
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

    // 手写图片存储目录
    private val handwritingDir: File by lazy {
        File(diaryDir, "handwriting").apply { mkdirs() }
    }

    // 背景音乐存储目录
    private val musicDir: File by lazy {
        File(diaryDir, "music").apply { mkdirs() }
    }

    private val charset: Charset = Charsets.UTF_8

    /**
     * 新增一篇日记，支持扩展字段
     */
    fun addNew(content: String, title: String? = null, emojis: List<String>? = null, 
               handwritingPaths: List<String>? = null, musicPath: String? = null): File {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("content", content)
            put("createdAt", now)
            put("updatedAt", now)
            
            // 新增字段
            if (emojis != null) put("emojis", JSONObject().apply {
                emojis.forEachIndexed { index, emoji ->
                    put("emoji_$index", emoji)
                }
            })
            
            if (handwritingPaths != null) put("handwritingPaths", JSONObject().apply {
                handwritingPaths.forEachIndexed { index, path ->
                    put("path_$index", path)
                }
            })
            
            put("musicPath", musicPath)
        }
        val file = fileOf(id)
        writeFileAtomic(file, json.toString())
        return file
    }

    /**
     * 保存或更新日记，支持扩展字段
     */
    suspend fun saveOrUpdate(id: String, content: String, title: String? = null, 
                           emojis: List<String>? = null, handwritingPaths: List<String>? = null,
                           musicPath: String? = null): File {
        val file = fileOf(id)
        val now = System.currentTimeMillis()
        val json = if (file.exists()) {
            runCatching { JSONObject(file.readText(charset)) }.getOrNull() ?: JSONObject().put("id", id)
        } else {
            JSONObject().put("id", id).put("createdAt", now)
        }
        
        json.put("title", title)
        json.put("content", content)
        json.put("updatedAt", now)
        
        // 更新扩展字段
        if (emojis != null) {
            val emojisJson = JSONObject()
            emojis.forEachIndexed { index, emoji ->
                emojisJson.put("emoji_$index", emoji)
            }
            json.put("emojis", emojisJson)
        }
        
        if (handwritingPaths != null) {
            val pathsJson = JSONObject()
            handwritingPaths.forEachIndexed { index, path ->
                pathsJson.put("path_$index", path)
            }
            json.put("handwritingPaths", pathsJson)
        }
        
        json.put("musicPath", musicPath)
        
        writeFileAtomicAsync(file, json.toString())
        return file
    }

    /**
     * 获取表情列表
     */
    fun getEmojis(id: String): List<String> {
        val jsonString = loadAsJsonString(id) ?: return emptyList()
        return runCatching {
            val json = JSONObject(jsonString)
            val emojisJson = json.optJSONObject("emojis") ?: return emptyList()
            val emojis = mutableListOf<String>()
            val keys = emojisJson.keys()
            while (keys.hasNext()) {
                emojis.add(emojisJson.getString(keys.next()))
            }
            emojis
        }.getOrElse { emptyList() }
    }

    /**
     * 获取手写图片路径列表
     */
    fun getHandwritingPaths(id: String): List<String> {
        val jsonString = loadAsJsonString(id) ?: return emptyList()
        return runCatching {
            val json = JSONObject(jsonString)
            val pathsJson = json.optJSONObject("handwritingPaths") ?: return emptyList()
            val paths = mutableListOf<String>()
            val keys = pathsJson.keys()
            while (keys.hasNext()) {
                paths.add(pathsJson.getString(keys.next()))
            }
            paths
        }.getOrElse { emptyList() }
    }

    /**
     * 获取背景音乐路径
     */
    fun getMusicPath(id: String): String? {
        val jsonString = loadAsJsonString(id) ?: return null
        return runCatching { 
            JSONObject(jsonString).optString("musicPath", null) 
        }.getOrNull()
    }

    /**
     * 保存手写图片
     */
    fun saveHandwritingImage(bitmapData: ByteArray, diaryId: String): String {
        val fileName = "handwriting_${diaryId}_${System.currentTimeMillis()}.png"
        val file = File(handwritingDir, fileName)
        file.writeBytes(bitmapData)
        return file.absolutePath
    }

    /**
     * 保存背景音乐
     */
    fun saveMusicFile(musicData: ByteArray, originalName: String): String {
        val fileName = "music_${System.currentTimeMillis()}_$originalName"
        val file = File(musicDir, fileName)
        file.writeBytes(musicData)
        return file.absolutePath
    }

    fun loadAsJsonString(id: String): String? {
        val file = fileOf(id)
        if (!file.exists()) return null
        return runCatching { file.readText(charset) }.onFailure {
            Timber.e(it, "Failed to read diary: %s", id)
        }.getOrNull()
    }

    fun loadContent(id: String): String? {
        val json = loadAsJsonString(id) ?: return null
        return runCatching { JSONObject(json).optString("content", null) }.getOrNull()
    }

    fun listAllFiles(): List<File> {
        val files = diaryDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }
    }

    fun delete(id: String): Boolean {
        // 删除关联的手写图片和音乐文件
        val handwritingPaths = getHandwritingPaths(id)
        handwritingPaths.forEach { path ->
            runCatching { File(path).delete() }
        }
        
        val musicPath = getMusicPath(id)
        musicPath?.let { path ->
            runCatching { File(path).delete() }
        }
        
        return fileOf(id).delete()
    }

    fun clearAll(): Int {
        val files = diaryDir.listFiles() ?: return 0
        var count = 0
        files.forEach { f ->
            if (f.isFile && f.delete()) count++
        }
        // 同时清空手写和音乐目录
        handwritingDir.listFiles()?.forEach { it.delete() }
        musicDir.listFiles()?.forEach { it.delete() }
        return count
    }

    fun storageDir(): File = diaryDir

    private fun fileOf(id: String): File = File(diaryDir, "$id.json")

    private fun writeFileAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeText(content, charset)
            if (!tmp.renameTo(target)) {
                throw IOException("Failed to rename tmp to target: ${tmp.absolutePath} -> ${target.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Atomic write failed: %s", target.absolutePath)
            runCatching { if (tmp.exists()) tmp.delete() }
            throw e
        }
    }

    private suspend fun writeFileAtomicAsync(target: File, content: String) = withContext(Dispatchers.IO) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeText(content, charset)
            if (!tmp.renameTo(target)) {
                throw IOException("Failed to rename tmp to target: ${tmp.absolutePath} -> ${target.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Atomic write failed: %s", target.absolutePath)
            runCatching { if (tmp.exists()) tmp.delete() }
            throw e
        }
    }
}