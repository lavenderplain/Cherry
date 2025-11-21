package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diary.cherry.R
import com.diary.cherry.model.Reminder
import com.diary.utils.ReminderSave
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class ReminderListFragment : Fragment() {

    private lateinit var reminderSave: ReminderSave
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReminderListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_reminder_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reminderSave = ReminderSave(requireContext())

        recyclerView = view.findViewById(R.id.recyclerReminders)
        adapter = ReminderListAdapter(emptyList(),
            onItemClick = { reminderId ->
                // 导航到备忘录编辑页面
                val bundle = Bundle().apply {
                    putString("reminderId", reminderId)
                }
                findNavController().navigate(R.id.action_reminderListFragment_to_reminderEditorFragment, bundle)
            },
            onToggleComplete = { reminderId ->
                // 切换完成状态
                toggleReminderCompletion(reminderId)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAddReminder).setOnClickListener {
            // 导航到新建备忘录页面
            findNavController().navigate(R.id.action_reminderListFragment_to_reminderEditorFragment)
        }

        loadReminders()
    }

    override fun onResume() {
        super.onResume()
        loadReminders()
    }

    private fun loadReminders() {
        val reminders = reminderSave.getAllReminders()
        adapter.updateData(reminders)

        // 更新统计信息
        updateStats(reminders)
    }

    private fun updateStats(reminders: List<Reminder>) {
        val pendingCount = reminders.count { !it.isCompleted && it.reminderTime > System.currentTimeMillis() }
        val overdueCount = reminders.count { it.isOverdue() }
        val completedCount = reminders.count { it.isCompleted }

        view?.findViewById<TextView>(R.id.tvStats)?.text =
            "待处理: $pendingCount | 已过期: $overdueCount | 已完成: $completedCount"
    }

    private fun toggleReminderCompletion(reminderId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val isNowCompleted = reminderSave.toggleCompletionStatus(reminderId)
                requireActivity().runOnUiThread {
                    val message = if (isNowCompleted) {
                        "备忘录已标记为完成"
                    } else {
                        "备忘录已标记为未完成"
                    }
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
                    loadReminders() // 重新加载数据更新UI
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle reminder completion status")
                requireActivity().runOnUiThread {
                    Snackbar.make(requireView(), "操作失败", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 内部类 - ReminderListAdapter
    class ReminderListAdapter(
        private var reminders: List<Reminder>,
        private val onItemClick: (String) -> Unit,
        private val onToggleComplete: (String) -> Unit
    ) : RecyclerView.Adapter<ReminderListAdapter.ReminderViewHolder>() {

        class ReminderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvReminderTitle)
            val content: TextView = view.findViewById(R.id.tvReminderContent)
            val time: TextView = view.findViewById(R.id.tvReminderTime)
            val status: TextView = view.findViewById(R.id.tvReminderStatus)
            val toggleComplete: TextView = view.findViewById(R.id.ivToggleComplete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reminder, parent, false)
            return ReminderViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
            val reminder = reminders[position]
            val dateFormatter = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            val timeLabel = dateFormatter.format(Date(reminder.reminderTime))

            holder.title.text = reminder.title
            holder.content.text = reminder.content
            holder.time.text = timeLabel

            // 根据状态设置不同的样式
            if (reminder.isCompleted) {
                holder.status.text = "已完成"
                holder.status.setBackgroundColor(0xFF4CAF50.toInt()) // 绿色
                // 显示勾号
                holder.toggleComplete.text = "✓"
            } else {
                // 未完成状态
                if (reminder.isOverdue()) {
                    holder.status.text = "已过期"
                    holder.status.setBackgroundColor(0xFFF44336.toInt()) // 红色
                } else {
                    holder.status.text = "待处理"
                    holder.status.setBackgroundColor(0xFF2196F3.toInt()) // 蓝色
                }
                // 显示方框
                holder.toggleComplete.text = "□"
            }

            // 设置黑色文本
            holder.toggleComplete.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))

            // 点击条目进入编辑
            holder.itemView.setOnClickListener {
                onItemClick(reminder.id)
            }

            // 点击勾选框切换完成状态
            holder.toggleComplete.setOnClickListener {
                onToggleComplete(reminder.id)
            }
        }

        override fun getItemCount() = reminders.size

        fun updateData(newReminders: List<Reminder>) {
            reminders = newReminders
            notifyDataSetChanged()
        }
    }
}