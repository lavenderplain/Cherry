package com.diary.utils.memo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 为Memo实体提供的数据库操作接口
 */
@Dao
interface MemoDao {
    @Insert
    suspend fun insert(memo: Memo): Long

    @Update
    suspend fun update(memo: Memo)

    @Delete
    suspend fun delete(memo: Memo)

    /**
     * 获取所有备忘录，按创建时间降序排列
     * @return 包含所有备忘录的Flow列表
     */
    @Query("SELECT * FROM memos ORDER BY createTime DESC")
    fun getAllMemos(): Flow<List<Memo>>

    /**
     * 根据ID获取特定备忘录
     * @param id 备忘录ID
     * @return 对应的备忘录对象，若不存在则返回null
     */
    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getMemoById(id: Int): Memo?

    /**
     * 获取所有待处理的提醒备忘录
     * @param currentTime 当前时间戳（毫秒）
     * @return 包含待处理提醒备忘录的列表
     */
    @Query("SELECT * FROM memos WHERE reminderTime > 0 AND reminderTime <= :currentTime AND isCompleted = 0")
    suspend fun getPendingReminders(currentTime: Long): List<Memo>

    /**
     * 将指定ID的备忘录标记为已完成
     * @param id 备忘录ID
     */
    @Query("UPDATE memos SET isCompleted = 1 WHERE id = :id")
    suspend fun markAsCompleted(id: Int)
}