package com.diary.utils.bgm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
/**
 * 音乐播放器管理器，负责绑定和管理后台音乐服务
 * @constructor 创建音乐播放器管理器
 */
class MusicPlayerManager private constructor(context: Context) {
    private val appContext = context.applicationContext // 使用应用程序上下文，避免内存泄漏
    private var serviceConnection: ServiceConnection? = null // 服务连接对象
    private var musicService: BackgroundMusicService? = null // 音乐服务实例
    private var isBound = false // 服务是否已绑定

    /**
     * 获取音乐播放器管理器实例
     * 确保整个安卓应用中，有且只有一个 MusicPlayerManager 的实例
     * @param context 上下文
     * @return 音乐播放器管理器实例
     */
    companion object {
        @Volatile // 确保多线程环境下的可见性
        private var instance: MusicPlayerManager? = null // 单例实例
        /**
         * 获取音乐播放器管理器实例
         * 通过双重检查锁定实现线程安全的单例模式
         * 懒加载实例
         * @param context 上下文
         * @return 音乐播放器管理器实例
         */
        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context).also { instance = it }
            }
        }
    }

    /**
     * 绑定后台音乐服务
     * @param onBound 绑定完成后的回调，传递音乐服务实例
     */
    fun bindService(onBound: (BackgroundMusicService?) -> Unit) {
        if (isBound) {
            onBound(musicService)
            return
        }

        // 创建服务连接对象
        serviceConnection = object : ServiceConnection {
            /**
             * 服务连接成功回调
             * @param name 组件名称
             * @param service 服务绑定器
             */
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                // 获取服务实例
                val binder = service as BackgroundMusicService.MusicBinder
                musicService = binder.getService()
                isBound = true
                onBound(musicService)
            }

            /**
             * 服务断开连接回调
             * @param name 组件名称
             */
            override fun onServiceDisconnected(name: ComponentName?) {
                musicService = null
                isBound = false
            }
        }

        val intent = Intent(appContext, BackgroundMusicService::class.java)
        appContext.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    /**
     * 解绑后台音乐服务
     */
    fun unbindService() {
        if (isBound && serviceConnection != null) {
            appContext.unbindService(serviceConnection!!)
            isBound = false
            musicService = null
            serviceConnection = null
        }
    }

    /**
     * 获取后台音乐服务实例
     * @return 音乐服务实例，若未绑定则返回 null
     */
    fun getService(): BackgroundMusicService? = musicService
}