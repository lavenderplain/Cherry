package com.diary.utils.memo

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * 备忘录数据类，包含标题、内容、创建时间、提醒时间、提醒类型和完成状态等属性。
 * 提醒类型使用枚举类ReminderType表示，支持定时提醒和倒计时提醒两种类型。
 */
@Entity(tableName = "memos")
@TypeConverters(ReminderTypeConverter::class)
data class Memo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val createTime: Long = System.currentTimeMillis(),
    val reminderTime: Long = 0, // 提醒时间戳（毫秒）
    val reminderType: ReminderType = ReminderType.TIMED,
    val isCompleted: Boolean = false
) {
    enum class ReminderType {
        TIMED,  // 定时提醒（特定时间点）
        COUNTDOWN // 倒计时提醒
    }
}