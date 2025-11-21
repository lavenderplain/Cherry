package com.diary.cherry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.diary.cherry.DiaryFragment.DiaryItem
import androidx.recyclerview.widget.ListAdapter
import java.text.SimpleDateFormat
import java.util.*

// TODO: 添加删除日记功能
// TODO: 优化预览内容的截断逻辑，避免截断单词
// TODO: 支持多种日期格式显示，用户可选择
// TODO: 添加日记分类标签，并在列表中显示
// TODO: 实现日记搜索功能，方便用户查找特定内容
// TODO: 优化背景色选择逻辑失效，始终为蓝色，这个问题仅存在与多次创建日记时，退出重进不会出现这个问题

class DiaryAdapter(
    private val onItemClick: (DiaryItem) -> Unit
) : ListAdapter<DiaryItem, DiaryAdapter.ViewHolder>(DiaryDiffCallback) {
    companion object {
        private const val MAX_PREVIEW_LENGTH = 50
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.diary_title)
        val contentText: TextView = itemView.findViewById(R.id.diary_content)
        val dateText: TextView = itemView.findViewById(R.id.diary_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val diary = getItem(position)

        holder.titleText.text = diary.title
        val previewContent = diary.content.take(MAX_PREVIEW_LENGTH)
        val ellipsis = if (diary.content.length > MAX_PREVIEW_LENGTH) "..." else ""
        holder.contentText.text = holder.itemView.context.getString(R.string.diary_content_preview, previewContent, ellipsis)
        holder.dateText.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(diary.createdAt)) // TODO: 本地化处理，已修正显示创建时间

        holder.itemView.setOnClickListener {
            onItemClick(diary)
        }

        // 交替背景色
        val backgroundColor = when (position % 3) {
            0 -> 0xFFE3F2FD.toInt() // 浅蓝
            1 -> 0xFFF3E5F5.toInt() // 浅紫
            else -> 0xFFE8F5E8.toInt() // 浅绿
        }
        holder.itemView.setBackgroundColor(backgroundColor)
    }
}

object DiaryDiffCallback : DiffUtil.ItemCallback<DiaryItem>() {
    override fun areItemsTheSame(oldItem: DiaryItem, newItem: DiaryItem): Boolean {
        // ID相同表示是同一个项目
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DiaryItem, newItem: DiaryItem): Boolean {
        // 内容相同（标题、内容、更新时间都相同）
        return oldItem.title == newItem.title &&
               oldItem.content == newItem.content &&
               oldItem.updatedAt == newItem.updatedAt
    }
}