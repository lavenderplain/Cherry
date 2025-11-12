package com.diary.cherry

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import timber.log.Timber

class CherryActivity : AppCompatActivity(R.layout.activity_diary) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_diary) as NavHostFragment
        val navController = navHostFragment.navController

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)

        // 示例按钮，用于测试日志异常输出
        findViewById<Button>(R.id.btnTestError).setOnClickListener {
            try {
                throw RuntimeException("Test exception from button click")
            } catch (e: Exception) {
                Timber.e(e, "Test error occurred by custom")
            }
        }

        // 功能按钮，打开日记编辑界面
        // 待完善
        findViewById<Button>(R.id.btnOpenDiary).setOnClickListener {
            try {
                val current = navController.currentDestination?.id
                if (current == R.id.diaryListFragment) {
                    navController.navigate(R.id.action_diaryListFragment_to_diaryEditorFragment)
                } else if (current != R.id.diaryEditorFragment) {
                    navController.navigate(R.id.diaryEditorFragment)
                }
                Timber.log(Log.INFO, "Navigated to DiaryEditorFragment")
            } catch (e: Exception) {
                Timber.e(e, "Navigation error occurred")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_diary) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}