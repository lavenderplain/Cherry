// [file name]: DiaryDetailFragment.kt
// [修改位置：移除navArgs，改用arguments获取参数]
package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R
import com.diary.utils.DiarySave
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class DiaryDetailFragment : Fragment() {

    private lateinit var diarySave: DiarySave
    private lateinit var diaryId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diarySave = DiarySave(requireContext())

        // 从arguments获取diaryId参数
        diaryId = arguments?.getString("diaryId") ?: run {
            Snackbar.make(view, "日记ID不存在", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        loadDiaryData(view)

        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            // 创建Bundle传递参数
            val bundle = Bundle().apply {
                putString("diaryId", diaryId)
            }
            findNavController().navigate(R.id.diaryEditorFragment, bundle)
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            deleteDiary(view)
        }

        view.findViewById<Button>(R.id.btnShare).setOnClickListener {
            shareDiary(view)
        }
    }

    private fun loadDiaryData(view: View) {
        val jsonString = diarySave.loadAsJsonString(diaryId)
        if (jsonString != null) {
            try {
                val json = org.json.JSONObject(jsonString)

                val title = json.optString("title", "无标题")
                val content = json.optString("content", "")
                val createdAt = json.optLong("createdAt", System.currentTimeMillis())

                val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                val dateString = dateFormatter.format(Date(createdAt))

                view.findViewById<TextView>(R.id.tvTitle).text = title
                view.findViewById<TextView>(R.id.tvDate).text = dateString
                view.findViewById<TextView>(R.id.tvContent).text = content

            } catch (e: Exception) {
                Timber.e(e, "Failed to parse diary data")
                Snackbar.make(view, "加载日记失败", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(view, "日记不存在", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun deleteDiary(view: View) {
        try {
            diarySave.delete(diaryId)
            Snackbar.make(view, "日记已删除", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete diary")
            Snackbar.make(view, "删除失败", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun shareDiary(view: View) {
        val jsonString = diarySave.loadAsJsonString(diaryId)
        if (jsonString != null) {
            try {
                val json = org.json.JSONObject(jsonString)
                val title = json.optString("title", "无标题")
                val content = json.optString("content", "")

                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$content")
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "分享日记"))

            } catch (e: Exception) {
                Timber.e(e, "Failed to share diary")
                Snackbar.make(view, "分享失败", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}