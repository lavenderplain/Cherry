// [file name]: DiaryEditorFragment.kt
// [修改位置：修复协程调用问题]
package com.diary.cherry.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R
import com.diary.utils.DiarySave
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class DiaryEditorFragment : Fragment() {

    private lateinit var diarySave: DiarySave
    private var diaryId: String = ""
    private var selectedDate = Date()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diarySave = DiarySave(requireContext())

        // 从arguments获取diaryId参数
        diaryId = arguments?.getString("diaryId", "") ?: ""

        val titleEt = view.findViewById<TextInputEditText>(R.id.etTitle)
        val contentEt = view.findViewById<TextInputEditText>(R.id.etContent)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)

        // 如果是编辑模式，加载现有数据
        if (diaryId.isNotEmpty()) {
            loadDiaryData(diaryId, titleEt, contentEt, btnPickDate)
        } else {
            updateDateButtonText(btnPickDate)
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val title = titleEt.text?.toString() ?: ""
            val content = contentEt.text?.toString() ?: ""

            if (title.isBlank() && content.isBlank()) {
                Snackbar.make(view, "标题和内容不能同时为空", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveDiary(diaryId, title, content, view)
        }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            findNavController().navigateUp()
        }

        btnPickDate.setOnClickListener {
            showDatePickerDialog(btnPickDate)
        }
    }

    private fun loadDiaryData(diaryId: String, titleEt: TextInputEditText, contentEt: TextInputEditText, btnPickDate: Button) {
        val jsonString = diarySave.loadAsJsonString(diaryId)
        if (jsonString != null) {
            try {
                val json = org.json.JSONObject(jsonString)
                titleEt.setText(json.optString("title", ""))
                contentEt.setText(json.optString("content", ""))

                val createdAt = json.optLong("createdAt", System.currentTimeMillis())
                selectedDate = Date(createdAt)
                updateDateButtonText(btnPickDate)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse diary data")
            }
        }
    }

    private fun saveDiary(diaryId: String, title: String, content: String, view: View) {
        val finalId = if (diaryId.isEmpty()) UUID.randomUUID().toString() else diaryId

        // 使用GlobalScope启动协程
        GlobalScope.launch(Dispatchers.IO) {
            try {
                diarySave.saveOrUpdate(finalId, content, if (title.isBlank()) null else title)

                requireActivity().runOnUiThread {
                    Snackbar.make(view, "日记保存成功", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save diary")
                requireActivity().runOnUiThread {
                    Snackbar.make(view, "保存失败: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDatePickerDialog(btnPickDate: Button) {
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = calendar.time
                updateDateButtonText(btnPickDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun updateDateButtonText(btnPickDate: Button) {
        btnPickDate.text = "日期: ${dateFormatter.format(selectedDate)}"
    }
}