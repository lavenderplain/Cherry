package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
            onMarkComplete = { reminderId ->
                // 标记为已完成
                markReminderAsComplete(reminderId)
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

    private fun markReminderAsComplete(reminderId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                reminderSave.markAsCompleted(reminderId)
                requireActivity().runOnUiThread {
                    Snackbar.make(requireView(), "备忘录已标记为完成", Snackbar.LENGTH_SHORT).show()
                    loadReminders() // 重新加载数据更新UI
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark reminder as complete")
                requireActivity().runOnUiThread {
                    Snackbar.make(requireView(), "标记失败", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 内部类 - ReminderListAdapter
    class ReminderListAdapter(
        private var reminders: List<Reminder>,
        private val onItemClick: (String) -> Unit,
        private val onMarkComplete: (String) -> Unit
    ) : RecyclerView.Adapter<ReminderListAdapter.ReminderViewHolder>() {

        class ReminderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvReminderTitle)
            val content: TextView = view.findViewById(R.id.tvReminderContent)
            val time: TextView = view.findViewById(R.id.tvReminderTime)
            val status: TextView = view.findViewById(R.id.tvReminderStatus)
            val btnMarkComplete: Button = view.findViewById(R.id.btnMarkComplete)
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
                holder.btnMarkComplete.visibility = View.GONE // 已完成的不显示按钮
            } else if (reminder.isOverdue()) {
                holder.status.text = "已过期"
                holder.status.setBackgroundColor(0xFFF44336.toInt()) // 红色
                holder.btnMarkComplete.visibility = View.VISIBLE
            } else {
                holder.status.text = "待处理"
                holder.status.setBackgroundColor(0xFF2196F3.toInt()) // 蓝色
                holder.btnMarkComplete.visibility = View.VISIBLE
            }

            // 点击条目进入编辑
            holder.itemView.setOnClickListener {
                onItemClick(reminder.id)
            }

            // 点击标记完成按钮
            holder.btnMarkComplete.setOnClickListener {
                onMarkComplete(reminder.id)
            }
        }

        override fun getItemCount() = reminders.size

        fun updateData(newReminders: List<Reminder>) {
            reminders = newReminders
            notifyDataSetChanged()
        }
    }
}