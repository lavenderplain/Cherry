package com.diary.utils.memo

import androidx.room.TypeConverter
import com.diary.utils.memo.Memo.ReminderType

/**
 * ReminderTypeConverter类用于在Room数据库中存储和检索ReminderType枚举类型。
 * 提供了将ReminderType转换为整数和将整数转换为ReminderType的方法。
 */
class ReminderTypeConverter {
    @TypeConverter
    fun toReminderType(value: Int): ReminderType {
        return when (value) {
            ReminderType.TIMED.ordinal -> ReminderType.TIMED
            ReminderType.COUNTDOWN.ordinal -> ReminderType.COUNTDOWN
            else -> throw IllegalArgumentException("Invalid ReminderType value: $value")
        }
    }

    @TypeConverter
    fun fromReminderType(type: ReminderType): Int {
        return type.ordinal
    }
}