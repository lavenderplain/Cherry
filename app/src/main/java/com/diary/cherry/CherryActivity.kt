package com.diary.cherry

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import timber.log.Timber
import android.view.View

class CherryActivity : AppCompatActivity(R.layout.activity_diary) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_diary) as NavHostFragment
        val navController = navHostFragment.navController

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        //显示页面标题，例如"日记列表"
        setupActionBarWithNavController(navController)

        // 处理通知点击
        handleNotificationIntent()

        // 示例按钮，用于测试日志异常输出
        findViewById<Button>(R.id.btnTestError).setOnClickListener {
            try {
                throw RuntimeException("Test exception from button click")
            } catch (e: Exception) {
                Timber.e(e, "Test error occurred by custom")
            }
        }
    }

    private fun handleNotificationIntent() {
        val diaryId = intent.getStringExtra("OPEN_DIARY_ID")
        diaryId?.let { id ->
            // 导航到日记详情页面
            val bundle = Bundle().apply {
                putString("diaryId", id)
            }
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_diary) as NavHostFragment
            navHostFragment.navController.navigate(R.id.diaryDetailFragment, bundle)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_diary) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}