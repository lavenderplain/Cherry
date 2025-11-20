package com.diary.cherry.model

import java.util.*

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val reminderTime: Long, // 提醒时间戳
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isOverdue(): Boolean {
        return reminderTime < System.currentTimeMillis() && !isCompleted
    }
    
    fun isUpcoming(withinMinutes: Long = 60): Boolean {
        val currentTime = System.currentTimeMillis()
        val threshold = currentTime + withinMinutes * 60 * 1000
        return reminderTime in currentTime..threshold && !isCompleted
    }
}