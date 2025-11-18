package com.diary.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

object EmojiUtils {
    // 扩展表情库（增加到40个常用表情，分类展示）
    fun getCommonEmojis(): List<String> {
        return listOf(
            // 面部表情
            "😊", "😂", "😍", "😢", "😠", "👍", "👎", "❤️", "🎉", "✨",
            "🌟", "🔥", "🥰", "😘", "🤔", "🙏", "👏", "👋", "🤗", "😉",
            // 新增表情
            "🥳", "🤩", "🤪", "😎", "🤓", "🥺", "😱", "😡", "🤯", "😴",
            "🤤", "😷", "🤒", "🤕", "💪", "👊", "🤝", "✌️", "🤞", "🙌"
        )
    }

    /**
     * 插入表情到EditText的光标位置
     */
    fun insertEmoji(editText: EditText, emoji: String) {
        val cursorPosition = editText.selectionStart
        val editable = editText.editableText
        editable.insert(cursorPosition, emoji)
    }

    /**
     * 显示优化后的表情选择对话框
     * @param context 上下文
     * @param onEmojiSelected 表情选中回调
     */
    fun showEmojiDialog(context: Context, onEmojiSelected: (String) -> Unit) {
        // 创建对话框
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_emoji_picker, null)
        val dialog = AlertDialog.Builder(context)
            .setTitle("选择表情")
            .setView(dialogView)
            .create()

        // 初始化表情列表
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvEmojiList)
        recyclerView.layoutManager = GridLayoutManager(context, 6) // 6列网格布局
        recyclerView.adapter = EmojiAdapter(getCommonEmojis()) { emoji ->
            onEmojiSelected(emoji)
            dialog.dismiss() // 选中后关闭对话框
        }

        dialog.show()
    }

    /**
     * 表情适配器（RecyclerView）
     */
    private class EmojiAdapter(
        private val emojis: List<String>,
        private val onEmojiClick: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

        class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val btnEmoji: Button = itemView.findViewById(R.id.btnEmoji)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emoji, parent, false)
            return EmojiViewHolder(view)
        }

        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            val emoji = emojis[position]
            holder.btnEmoji.text = emoji
            holder.btnEmoji.setOnClickListener { onEmojiClick(emoji) }
        }

        override fun getItemCount() = emojis.size
    }
}
