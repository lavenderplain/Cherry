package com.diary.cherry.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R
import com.diary.cherry.model.Reminder
import com.diary.utils.ReminderSave
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class ReminderEditorFragment : Fragment() {

    private lateinit var reminderSave: ReminderSave
    private var reminderId: String = ""
    private var reminderTime: Long = System.currentTimeMillis() + 3600000 // 默认1小时后
    private var isCompleted: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_reminder_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reminderSave = ReminderSave(requireContext())
        reminderId = arguments?.getString("reminderId", "") ?: ""

        val titleEt = view.findViewById<TextInputEditText>(R.id.etReminderTitle)
        val contentEt = view.findViewById<TextInputEditText>(R.id.etReminderContent)
        val btnPickTime = view.findViewById<Button>(R.id.btnPickReminderTime)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteReminder)
        val btnComplete = view.findViewById<Button>(R.id.btnCompleteReminder)

        if (reminderId.isNotEmpty()) {
            // 编辑模式：显示删除按钮
            btnDelete.visibility = View.VISIBLE
            loadReminderData(reminderId, titleEt, contentEt, btnPickTime, btnComplete)
        } else {
            // 新建模式：隐藏删除按钮和完成按钮
            btnDelete.visibility = View.GONE
            btnComplete.visibility = View.GONE
            updateTimeButtonText(btnPickTime)
        }

        // 设置时间选择
        btnPickTime.setOnClickListener {
            showDateTimePicker(btnPickTime)
        }

        // 保存按钮
        view.findViewById<Button>(R.id.btnSaveReminder).setOnClickListener {
            val title = titleEt.text?.toString() ?: ""
            val content = contentEt.text?.toString() ?: ""

            if (title.isBlank()) {
                Snackbar.make(view, "请输入备忘录标题", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveReminder(reminderId, title, content, view)
        }

        // 取消按钮
        view.findViewById<Button>(R.id.btnCancelReminder).setOnClickListener {
            findNavController().navigateUp()
        }

        // 删除按钮（仅编辑模式显示）
        btnDelete.setOnClickListener {
            deleteReminder(view)
        }

        // 完成按钮（仅编辑模式显示且未完成时显示）
        btnComplete.setOnClickListener {
            markAsComplete(view)
        }
    }

    private fun loadReminderData(
        reminderId: String,
        titleEt: TextInputEditText,
        contentEt: TextInputEditText,
        btnPickTime: Button,
        btnComplete: Button
    ) {
        val reminder = reminderSave.load(reminderId)
        if (reminder != null) {
            titleEt.setText(reminder.title)
            contentEt.setText(reminder.content)
            reminderTime = reminder.reminderTime
            isCompleted = reminder.isCompleted
            updateTimeButtonText(btnPickTime)

            // 修复逻辑：只有未完成的备忘录才显示完成按钮
            if (reminder.isCompleted) {
                btnComplete.visibility = View.GONE
                btnComplete.text = "已完成" // 可以改为显示状态，但隐藏按钮
            } else {
                btnComplete.visibility = View.VISIBLE
                btnComplete.text = "标记为已完成"
            }
        }
    }

    private fun showDateTimePicker(btnPickTime: Button) {
        val calendar = Calendar.getInstance().apply { timeInMillis = reminderTime }

        // 先选择日期
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                // 然后选择时间
                showTimePicker(calendar, btnPickTime)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker(calendar: Calendar, btnPickTime: Button) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                reminderTime = calendar.timeInMillis
                updateTimeButtonText(btnPickTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateTimeButtonText(btnPickTime: Button) {
        val timeFormatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
        btnPickTime.text = "提醒时间: ${timeFormatter.format(Date(reminderTime))}"
    }

    private fun saveReminder(reminderId: String, title: String, content: String, view: View) {
        val finalId = if (reminderId.isEmpty()) UUID.randomUUID().toString() else reminderId

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val reminder = Reminder(
                    id = finalId,
                    title = title,
                    content = content,
                    reminderTime = reminderTime,
                    isCompleted = isCompleted // 保持原有的完成状态
                )
                reminderSave.saveOrUpdate(reminder)

                requireActivity().runOnUiThread {
                    Snackbar.make(view, "备忘录保存成功", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save reminder")
                requireActivity().runOnUiThread {
                    Snackbar.make(view, "保存失败: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteReminder(view: View) {
        if (reminderId.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    reminderSave.delete(reminderId)
                    requireActivity().runOnUiThread {
                        Snackbar.make(view, "备忘录已删除", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to delete reminder")
                    requireActivity().runOnUiThread {
                        Snackbar.make(view, "删除失败", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun markAsComplete(view: View) {
        if (reminderId.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    reminderSave.markAsCompleted(reminderId)
                    requireActivity().runOnUiThread {
                        Snackbar.make(view, "备忘录已标记为完成", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to mark reminder as complete")
                    requireActivity().runOnUiThread {
                        Snackbar.make(view, "标记失败", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}