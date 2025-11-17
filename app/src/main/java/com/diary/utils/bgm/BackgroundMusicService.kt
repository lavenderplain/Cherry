package com.diary.utils.bgm


import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import timber.log.Timber

/**
 * 后台音乐播放服务
 * @constructor 管理后台音乐播放服务
 */
class BackgroundMusicService : Service() {
    private val binder = MusicBinder() // 服务绑定器，其他组件通过它与服务交互
    private var mediaPlayer: MediaPlayer? = null // 媒体播放器实例
    private var currentPlaylist: List<Uri> = emptyList() // 当前播放列表
    private var currentIndex = 0 // 当前播放索引
    private var isPrepared = false // 播放器是否已准备好
    private var playMode: PlayMode = PlayMode.LIST_LOOP // 播放模式

    enum class PlayMode {
        SINGLE_LOOP, // 单曲循环
        LIST_LOOP, // 列表循环
        SHUFFLE, // 随机播放
    }

    inner class MusicBinder : Binder() {
        // 通过bindService绑定服务时返回的Binder，从而获取服务实例
        fun getService(): BackgroundMusicService = this@BackgroundMusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder // 返回绑定器

    /** 设置播放列表
     * @param musicUris 音乐文件URI列表
     */
    fun setPlaylist(musicUris: List<Uri>) {
        currentPlaylist = musicUris
        currentIndex = 0
        prepareCurrentTrack()
    }

    /**
     * 播放指定位置的音乐
     * @param position 播放列表中的索引位置
     */
    fun playTrack(position: Int) {
        if (position in currentPlaylist.indices) {
            currentIndex = position
            prepareCurrentTrack()
        }
    }

    /**
     * 开始播放音乐
     */
    fun play() {
        mediaPlayer?.start()
    }

    /**
     * 暂停播放音乐
     */
    fun pause() {
        mediaPlayer?.pause()
    }

    /**
     * 停止播放音乐并释放资源
     */
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }

    /**
     * 播放下一首音乐
     */
    fun next() {
        if (currentPlaylist.isEmpty()) return

        currentIndex = (currentIndex + 1) % currentPlaylist.size
        prepareCurrentTrack()
    }

    /**
     * 播放上一首音乐
     */
    fun previous() {
        if (currentPlaylist.isEmpty()) return

        currentIndex = if (currentIndex == 0) {
            currentPlaylist.size - 1
        } else {
            currentIndex - 1
        }
        prepareCurrentTrack()
    }

    /**
     * 设置播放模式
     * @param playMode 播放模式
     */
    fun setPlayMode(playMode: PlayMode) {
        this.playMode = playMode
    }

    /**
     * 获取当前播放位置
     * @return 当前播放位置，单位为毫秒
     */
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    /**
     * 获取音乐总时长
     * @return 音乐总时长，单位为毫秒
     */
    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    /**
     * 跳转到指定位置
     * @param position 跳转位置，单位为毫秒
     */
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    /**
     * 检查音乐是否正在播放
     * @return 如果正在播放则返回 true，否则返回 false
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    /**
     * 获取当前播放的音乐URI
     * @return 当前播放的音乐URI，如果无效则返回 null
     */
    fun getCurrentUri(): Uri? = if (currentIndex in currentPlaylist.indices) {
        currentPlaylist[currentIndex]
    } else {
        null
    }

    /**
     * 准备当前曲目进行播放
     */
    private fun prepareCurrentTrack() {
        if (currentPlaylist.isEmpty()) return

        val uri = currentPlaylist[currentIndex]
        mediaPlayer?.release() // 释放之前的资源

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri) // 设置数据源
            setOnPreparedListener { // 设置准备完成监听器
                isPrepared = true
                start()
            }
            setOnCompletionListener { // 设置播放完成监听器
                val shouldLoop = when (playMode) {
                    PlayMode.SINGLE_LOOP -> true
                    PlayMode.LIST_LOOP -> false
                    PlayMode.SHUFFLE -> {
                        // 随机选择下一首，通过修改currentIndex实现
                        currentIndex = (currentPlaylist.indices).random()
                        prepareCurrentTrack()
                        return@setOnCompletionListener
                    }
                }
                if (shouldLoop) { // 循环播放当前曲目
                    seekTo(0)
                    start()
                } else { // 播放下一首
                    next()
                }
            }
            setOnErrorListener { _, what, extra -> // 设置错误监听器
                Timber.tag("MusicService").e("Playback error: what=$what, extra=$extra")
                true
            }
            prepareAsync()
        }
    }

    /**
     * 服务销毁时释放资源
     */
    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}