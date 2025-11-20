package com.diary.utils.bgm

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import android.os.Build
import timber.log.Timber
import androidx.core.net.toUri
import androidx.core.content.edit

/**
 * 音乐文件选择器
 * 提供查询设备上音乐文件的功能
 * 包括MediaStore和自定义目录
 * 并实现缓存机制以提高查询效率
 */
object MusicSelector {
    /**
     * 查询MediaStore中的音乐文件
     * @param context 上下文
     * @return 音乐文件URI列表
     */
    private fun queryMediaMusicFiles(context: Context): List<Uri> {
        // 存储音乐文件的URI列表
        val musicUris = mutableListOf<Uri>()

        // 定义查询参数：projection为只获取ID列，selection为只选择音乐文件
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        // 执行查询
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // 数据源，系统的外部音频存储
            projection,
            selection,
            null,
            null
        )

        // 遍历查询结果，构建音乐文件的URI并添加到列表中
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (it.moveToNext()) {
                // 获取音乐文件的ID
                val id = it.getLong(idColumn)
                // 构建音乐文件的URI
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                musicUris.add(uri)
            }
        }

        // 关闭游标
        cursor?.close()
        return musicUris
    }

    /**
     * 查询自定义目录中的音乐文件
     * @param context 上下文
     * @return 音乐文件URI列表
     */
    private fun queryCustomDirs(context: Context): List<Uri> {
        return CustomMusicSelector.queryCustomMusicFiles(context)
    }

    /**
     * 记录所有音乐文件的路径（调试用）
     * @param context 上下文
     */
    fun logAllMusicFilePaths(context: Context) {
    // 定义查询参数：获取ID和文件路径
    val projection = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            // Android 10 (Q) 及以上版本，推荐使用 RELATIVE_PATH 和 DISPLAY_NAME
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.DISPLAY_NAME
            )
        }
        else -> {
            // Android 10 以下版本，使用 _DATA 获取完整路径
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA
            )
        }
    }

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    try {
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        cursor?.use { // use 块会自动关闭 cursor
            Timber.d("开始查询音乐文件，共找到 ${it.count} 个。")

            // 获取列索引
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 对于新版本，我们稍后拼接路径
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            }
            val nameColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            } else {
                -1 // 不需要
            }

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)

                val fullPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+：拼接相对路径和文件名
                    val relativePath = it.getString(pathColumn) ?: ""
                    val displayName = it.getString(nameColumn) ?: ""
                    // RELATIVE_PATH 通常以 '/' 结尾，所以直接拼接
                    "/storage/emulated/0/$relativePath$displayName"
                } else {
                    // Android 10-：直接从 _DATA 获取
                    it.getString(pathColumn)
                }

                Timber.d("找到音乐文件 (ID: $id):$fullPath")
            }
        }
    } catch (e: SecurityException) {
        Timber.e(e, "没有读取外部存储的权限。")
    } catch (e: Exception) {
        Timber.e(e, "查询音乐文件时发生未知错误。")
    }
}

    /**
     * 对外接口-查询设备上的音乐文件
     * 包括MediaStore和自定义目录
     * 使用缓存机制提高查询效率
     * @param context 上下文
     * @param forceRefresh 是否强制刷新缓存
     * @return 音乐文件URI列表
     */
    fun queryMusicFiles(context: Context, forceRefresh: Boolean = false): List<Uri> {
        if (!forceRefresh || MusicCacheManager.isCacheValid(context)) {
            val uris = MusicCacheManager.getCachedUris(context)
            return uris ?: emptyList()
        }
        val musicUris = mutableListOf<Uri>()

        // 1. 查询系统媒体库
        musicUris.addAll(queryMediaMusicFiles(context))
        // 2. 查询自定义目录
        musicUris.addAll(queryCustomDirs(context))

        // 3. 保存到缓存
        MusicCacheManager.saveCache(context, musicUris)
        return musicUris
    }

    /**
     * 添加单个音乐文件到缓存
     * @param context 上下文
     * @param fileUri 音乐文件的URI
     */
    fun addSingleFile(context: Context, fileUri: Uri) {
        val uris = MusicCacheManager.getCachedUris(context)?.toMutableList()
        if (uris == null) {
            MusicCacheManager.saveCache(context, listOf(fileUri))
            return
        }
        // 不重复添加
        if (uris.contains(fileUri)) return
        uris.add(fileUri)
        MusicCacheManager.saveCache(context, uris)
    }

    /**
     * 移除单个音乐文件从缓存
     * @param context 上下文
     * @param fileUri 音乐文件的URI
     */
    fun removeSingleFile(context: Context, fileUri: Uri) {
        val uris = MusicCacheManager.getCachedUris(context)?.toMutableList()
        if (uris != null) {
            uris.remove(fileUri)
            MusicCacheManager.saveCache(context, uris)
        }
    }
}

/**
 * 音乐文件缓存管理器
 * 实现两级缓存机制：内存缓存 + 持久化缓存
 * 提供缓存有效性检查、读取、保存和清理功能
 */
private object MusicCacheManager {
    // 使用SharedPreferences作为持久化缓存
    private const val PREFS_NAME = "music_cache" // SharedPreferences名称
    private const val KEY_TIMESTAMP = "timestamp" // 缓存时间戳键
    private const val KEY_URIS = "uris" // 音乐文件URI列表键
    private const val KEY_CUSTOM_DIR = "custom_dir" // 非MediaStore合法目录列表键
    // 两级缓存：内存缓存 + 持久化缓存
    private var memoryCacheUri: List<Uri>? = null      // 一级缓存：内存
    private var memoryCacheDir: Set<String>? = null    // 自定义目录内存缓存
    private var memoryCacheTime: Long = 0           // 内存缓存时间戳
    private const val CACHE_VALID_DURATION = 24 * 60 * 60 * 1000 // 24小时

    /**
     * 检查缓存是否有效
     * 优先检查内存缓存，其次检查持久化缓存
     * @param context 上下文
     * @return 缓存是否有效，true表示有效，false表示无效
     */
    fun isCacheValid(context: Context): Boolean {
        return isMemoryCacheValid() || isPersistentCacheValid(context)
    }

    /**
     * 检查内存缓存是否有效
     * @return 内存缓存是否有效，true表示有效，false表示无效
     */
    private fun isMemoryCacheValid(): Boolean {
        return memoryCacheUri != null && memoryCacheDir != null &&
               (System.currentTimeMillis() - memoryCacheTime) < CACHE_VALID_DURATION
    }

    /**
     * 检查持久化缓存是否有效
     * 慢速路径，应用重启后使用
     * @param context 上下文
     * @return 持久化缓存是否有效，true表示有效，false表示无效
     */
    private fun isPersistentCacheValid(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        val hasUris = prefs.contains(KEY_URIS)  // 检查键是否存在
        val hasCustomDir = prefs.contains(KEY_CUSTOM_DIR)

        val isValid = timestamp > 0 &&
                     hasUris && hasCustomDir &&
                     (System.currentTimeMillis() - timestamp) < CACHE_VALID_DURATION

        if (isValid) {
            // 缓存预热：将持久化缓存加载到内存
            loadCacheToMemory(context)
        }
        return isValid
    }

    /**
     * 获取缓存的音乐文件URI列表
     * 优先从内存缓存获取，其次从持久化缓存获取
     * @param context 上下文
     * @return 音乐文件URI列表，如果缓存无效则返回null
     */
    fun getCachedUris(context: Context): List<Uri>? {
        return when {
            isMemoryCacheValid() -> memoryCacheUri
            isPersistentCacheValid(context) -> memoryCacheUri
            else -> null
        }
    }

    /**
     * 获取缓存的自定义目录列表
     * 优先从内存缓存获取，其次从持久化缓存获取
     * @param context 上下文
     * @return 自定义目录列表，如果缓存无效则返回null
     */
    fun getCachedCustomDirs(context: Context): Set<String>? {
        return when {
            isMemoryCacheValid() -> memoryCacheDir
            isPersistentCacheValid(context) -> memoryCacheDir
            else -> null
        }
    }

    /**
     * 保存音乐文件URI列表到缓存
     * 同时更新内存缓存和持久化缓存
     * @param context 上下文
     * @param uris 音乐文件URI列表
     */
    fun saveCache(context: Context, uris: List<Uri>) {
        saveToMemory(uris)          // 更新内存缓存
        saveToPreferences(context, uris)  // 更新持久化缓存
    }

    /**
     * 保存自定义目录列表到缓存
     * 同时更新内存缓存和持久化缓存
     * @param context 上下文
     * @param customDirs 自定义目录列表
     */
    fun saveCache(context: Context, customDirs: Set<String>){
        // 因为自定义目录变化不频繁，直接只更新持久化缓存和内存缓存
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putStringSet(KEY_CUSTOM_DIR, customDirs)
            apply()
        }
        memoryCacheDir = customDirs
    }

    /**
     * 加载持久化缓存到内存
     * @param context 上下文
     * @return 音乐文件URI列表，如果持久化缓存无效则返回null
     */
    private fun loadCacheToMemory(context: Context): List<Uri>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriSet = prefs.getStringSet(KEY_URIS, null) ?: return null
        val customDirSet = prefs.getStringSet(KEY_CUSTOM_DIR, null) ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)

        val uris = uriSet.map { it.toUri() }
        memoryCacheUri = uris
        memoryCacheDir = customDirSet
        memoryCacheTime = timestamp  // 使用原始保存时间，不是当前时间
        return uris
    }

    /**
     * 保存音乐文件URI列表到内存缓存
     * @param uris 音乐文件URI列表
     */
    private fun saveToMemory(uris: List<Uri>) {
        memoryCacheUri = uris
        memoryCacheTime = System.currentTimeMillis()
    }

    /**
     * 保存音乐文件URI列表到持久化缓存
     * @param context 上下文
     * @param uris 音乐文件URI列表
     */
    private fun saveToPreferences(context: Context, uris: List<Uri>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriSet = uris.map { it.toString() }.toSet()

        prefs.edit().apply {
            putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            putStringSet(KEY_URIS, uriSet)
            apply()  // 异步提交，不阻塞UI线程
        }
    }

    /**
     * 清理内存缓存和持久化缓存
     * @param context 上下文
     */
    fun clearCache(context: Context) {
        // 清理内存缓存
        memoryCacheUri = null
        memoryCacheDir = null
        memoryCacheTime = 0

        // 清理持久化缓存
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }

    /**
     * 获取缓存信息
     * 包括内存缓存和持久化缓存的大小、最后更新时间和有效性
     * @param context 上下文
     * @return 缓存信息数据类
     */
    fun getCacheInfo(context: Context): CacheInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        val uriSet = prefs.getStringSet(KEY_URIS, emptySet())
        val customDirSet = prefs.getStringSet(KEY_CUSTOM_DIR, emptySet())

        return CacheInfo(
            memoryCachedUriSize = memoryCacheUri?.size ?: 0,
            persistentCachedUriSize = uriSet?.size ?: 0,
            memoryCachedCustomDirSize = memoryCacheDir?.size ?: 0,
            persistentCachedCustomDirSize = customDirSet?.size ?: 0,
            lastUpdated = if (timestamp > 0) timestamp else memoryCacheTime,
            isValid = isCacheValid(context)
        )
    }

    /**
     * 缓存信息数据类
     * 包括内存缓存和持久化缓存的大小、最后更新时间和有效性
     */
    data class CacheInfo(
        val memoryCachedUriSize: Int,
        val persistentCachedUriSize: Int,
        val memoryCachedCustomDirSize: Int,
        val persistentCachedCustomDirSize: Int,
        val lastUpdated: Long,
        val isValid: Boolean
    )
}

/**
 * 自定义音乐目录选择器
 * 提供添加、查询和移除自定义音乐目录的功能
 */
object CustomMusicSelector {
    /**
     * 添加自定义目录
     * @param context 上下文
     * @param dirUri 自定义目录的URI
     */
    fun addCustomDir(context: Context, dirUri: Uri) {
        val customDirs  = MusicCacheManager.getCachedCustomDirs(context)?.toMutableSet()
        if (customDirs == null) {
            MusicCacheManager.saveCache(context, setOf(dirUri.toString()))
            return
        }
        customDirs.add(dirUri.toString())
        MusicCacheManager.saveCache(context, customDirs.toSet())
    }

    /**
     * 查询自定义目录中的音乐文件
     * @param context 上下文
     * @return 音乐文件URI列表
     */
    fun queryCustomMusicFiles(context: Context): List<Uri> {
        val musicUris = mutableListOf<Uri>()
        MusicCacheManager.getCachedCustomDirs(context)?.forEach { dirUriString ->
            val dirUri = dirUriString.toUri()
            musicUris.addAll(queryMusicInDir(context, dirUri))
        }
        MusicCacheManager.saveCache(context, musicUris)
        return musicUris
    }

    /**
     * 递归查询目录中的音乐文件
     * @param context 上下文
     * @param dirUri 目录的URI
     * @return 音乐文件URI列表
     */
    private fun queryMusicInDir(context: Context, dirUri: Uri): List<Uri> {
        val musicUris = mutableListOf<Uri>()

        try {
            val documentFile = DocumentFile.fromTreeUri(context, dirUri)
            documentFile?.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    // 递归搜索子目录
                    musicUris.addAll(queryMusicInDir(context, file.uri))
                } else if (isMusicFile(file)) {
                    musicUris.add(file.uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return musicUris
    }

    /**
     * 检查文件是否为音乐文件
     * 通过MIME类型和文件扩展名进行判断
     * @param documentFile 文件对象
     * @return 是否为音乐文件，true表示是，false表示否
     */
    private fun isMusicFile(documentFile: DocumentFile): Boolean {
        val mimeType = documentFile.type ?: return false
        return mimeType.startsWith("audio/") ||
               documentFile.name?.let { name ->
                   name.endsWith(".mp3", ignoreCase = true) ||
                   name.endsWith(".wav", ignoreCase = true) ||
                   name.endsWith(".aac", ignoreCase = true) ||
                   name.endsWith(".flac", ignoreCase = true) ||
                   name.endsWith(".m4a", ignoreCase = true) ||
                   name.endsWith(".ogg", ignoreCase = true)
               } ?: false
    }

    /**
     * 移除自定义目录
     * @param context 上下文
     * @param dirUri 自定义目录的URI
     */
    fun removeCustomDir(context: Context, dirUri: Uri) {
        val dirs = MusicCacheManager.getCachedCustomDirs(context)?.toMutableSet()
        if (dirs != null) {
            dirs.remove(dirUri.toString())
            MusicCacheManager.saveCache(context, dirs.toSet())
        }
    }
}