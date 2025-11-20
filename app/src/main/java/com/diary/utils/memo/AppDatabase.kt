package com.diary.utils.memo

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * AppDatabase类是Room数据库的主要访问点。
 * 定义了数据库的实体和版本，并提供了获取DAO的方法。
 * 使用单例模式确保数据库实例在应用程序中唯一。
 */
@Database(
    entities = [Memo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ReminderTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memo_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}