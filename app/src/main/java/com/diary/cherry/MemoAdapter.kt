package com.diary.cherry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.diary.utils.memo.Memo
import java.util.*

// TODO: 优化预览内容的截断逻辑，避免截断单词
// TODO: 统一备忘录与日记的背景色选择逻辑，确保一致性
// TODO: 添加备忘录搜索功能，方便用户查找特定内容
// TODO: 检查logcat输出、单行代码过长、注释、命名规范、未使用资源等问题
// TODO: 按日期定时，如果超过不会“变灰”来提醒用户

class MemoAdapter(
    private val onItemClick: (MemoItem) -> Unit
) : ListAdapter<MemoAdapter.MemoItem, MemoAdapter.ViewHolder>(MemoDiffCallback) {
    data class MemoItem(
        val id: Int,
        val title: String,
        val content: String,
        val reminderType: Memo.ReminderType,
        val reminderTime: Long,
        val isCompleted: Boolean,
        val createdAt: Long,
        // val updatedAt: Long
    )

    companion object {
        private const val MAX_PREVIEW_LENGTH = 50
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.memo_title)
        val contentText: TextView = itemView.findViewById(R.id.memo_content)
        val dateText: TextView = itemView.findViewById(R.id.memo_date)
        val reminderText: TextView = itemView.findViewById(R.id.memo_reminder)
        val completedIndicator: View = itemView.findViewById(R.id.completed_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memo = getItem(position)

        holder.titleText.text = memo.title
        val previewContent = memo.content.take(MAX_PREVIEW_LENGTH)
        val ellipsis = if (memo.content.length > MAX_PREVIEW_LENGTH) "..." else ""
        holder.contentText.text = holder.itemView.context.getString(R.string.memo_content_preview, previewContent, ellipsis)
        // 日期显示：创建时间
        holder.dateText.text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            .format(Date(memo.createdAt)) // TODO: 本地化处理，已修正显示创建时间

        // 提醒信息
        val reminderText = when (memo.reminderType) {
            Memo.ReminderType.NONE -> "无提醒"
            Memo.ReminderType.TIMED -> "定时: ${formatReminderTime(memo.reminderTime)}"
            Memo.ReminderType.COUNTDOWN -> "倒计时: ${formatCountdown(memo.reminderTime)}"
        }
        holder.reminderText.text = reminderText
        holder.reminderText.visibility = if (memo.reminderType == Memo.ReminderType.NONE) View.GONE else View.VISIBLE

        // 完成状态
        holder.completedIndicator.visibility = if (memo.isCompleted) View.VISIBLE else View.GONE
        if (memo.isCompleted) {
            holder.itemView.alpha = 0.6f
        } else {
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            onItemClick(memo)
        }

        // 交替背景色
        val backgroundColor = when (position % 3) {
            0 -> 0xFFE8F5E8.toInt() // 浅绿
            1 -> 0xFFFFF3E0.toInt() // 浅橙
            else -> 0xFFFCE4EC.toInt() // 浅粉
        }
        holder.itemView.setBackgroundColor(backgroundColor)
    }

    private fun formatReminderTime(timestamp: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }
    private fun formatCountdown(reminderTime: Long): String {
        val currentTime = System.currentTimeMillis()
        val diffMillis = reminderTime - currentTime
        if (diffMillis <= 0) return "已到时间"

        val seconds = diffMillis / 1000 % 60
        val minutes = diffMillis / (1000 * 60) % 60
        val hours = diffMillis / (1000 * 60 * 60) % 24
        val days = diffMillis / (1000 * 60 * 60 * 24)

        return buildString {
            if (days > 0) append("${days}天 ")
            if (hours > 0) append("${hours}小时 ")
            if (minutes > 0) append("${minutes}分钟 ")
            if (seconds > 0) append("${seconds}秒")
        }.trim()
    }
}

// DiffUtil回调
object MemoDiffCallback : DiffUtil.ItemCallback<MemoAdapter.MemoItem>() {
    override fun areItemsTheSame(oldItem: MemoAdapter.MemoItem, newItem: MemoAdapter.MemoItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MemoAdapter.MemoItem, newItem: MemoAdapter.MemoItem): Boolean {
        return oldItem.title == newItem.title &&
               oldItem.content == newItem.content &&
               oldItem.reminderType == newItem.reminderType &&
               oldItem.reminderTime == newItem.reminderTime &&
               // oldItem.isCompleted == newItem.isCompleted &&
               // oldItem.updatedAt == newItem.updatedAt
               oldItem.isCompleted == newItem.isCompleted
    }
}