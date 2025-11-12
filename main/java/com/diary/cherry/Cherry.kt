package com.diary.cherry

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.diary.utils.ReleaseTree
import timber.log.Timber

class Cherry : Application() {
    val cherryScope = ProcessLifecycleOwner.get().lifecycleScope
    lateinit var releaseTree: ReleaseTree

    override fun onCreate() {
        super.onCreate()

        // 初始化 Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        releaseTree = ReleaseTree(this, BuildConfig.LOG_MIN_LEVEL, cherryScope)
        Timber.plant(releaseTree)
    }
}