package com.diary.utils.bgm

import android.content.Context
import android.net.Uri
import timber.log.Timber
/**
 * 音乐播放器接口类，封装了与音乐播放服务的交互
 * @constructor 创建音乐播放器接口
 */
class MusicPlayerInterface(private val context: Context) {
    // 音乐播放器管理器实例, 负责绑定和管理后台音乐服务
    private val musicManager: MusicPlayerManager by lazy { MusicPlayerManager.getInstance(context) }
    /**
     * 绑定后台音乐服务
     * @param onBound 绑定完成后的回调，传递音乐服务实例
     */
    fun bindService(onBound: (BackgroundMusicService?) -> Unit = {}) { musicManager.bindService(onBound) }
    /**
     * 解绑后台音乐服务
     */
    fun unbindService() { musicManager.unbindService() }

    /**
     * 选择并播放音乐文件
     * 查询设备上的音乐文件，设置播放列表并开始播放
     */
    fun selectAndPlayMusic() {
        // 查询音乐文件
        val musicUris = MusicSelector.queryMusicFiles(context)

        if (musicUris.isEmpty()) {
            Timber.tag("CherryPlayer").w("No music files found")
            return
        }

        // 设置播放列表并播放
        musicManager.getService()?.setPlaylist(musicUris)
        musicManager.getService()?.play()
    }

    /**
     * 开始播放音乐
     */
    fun play() {
        musicManager.getService()?.play()
    }

    /**
     * 暂停播放音乐
     */
    fun pause() {
        musicManager.getService()?.pause()
    }

    /**
     * 停止播放音乐
     */
    fun stop() {
        musicManager.getService()?.stop()
    }

    /**
     * 播放下一首音乐
     */
    fun next() {
        musicManager.getService()?.next()
    }

    /**
     * 播放上一首音乐
     */
    fun previous() {
        musicManager.getService()?.previous()
    }

    /**
     * 设置播放模式
     * @param playMode 播放模式枚举值
     */
    fun setLooping(playMode: BackgroundMusicService.PlayMode) {
        musicManager.getService()?.setPlayMode(playMode)
    }

    /**
     * 检查音乐是否正在播放
     * @return 如果正在播放则返回 true，否则返回 false
     */
    fun isPlaying(): Boolean = musicManager.getService()?.isPlaying() ?: false

    /**
     * 获取当前播放的音乐文件 URI
     * @return 当前播放的音乐文件 URI，若无则返回 null
     */
    fun getCurrentUri(): Uri? = musicManager.getService()?.getCurrentUri()

    /**
     * 释放音乐播放器接口资源，解绑后台音乐服务
     */
    fun release() {
        musicManager.unbindService()
    }
}