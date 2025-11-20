// [file name]: DiaryListFragment.kt
// [修改位置：使用Android内置布局，避免item_diary文件问题]
package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diary.cherry.R
import com.diary.utils.DiarySave
import com.google.android.material.floatingactionbutton.FloatingActionButton
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class DiaryListFragment : Fragment() {

    private lateinit var diarySave: DiarySave
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DiaryListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diarySave = DiarySave(requireContext())

        recyclerView = view.findViewById(R.id.recyclerEntries)
        adapter = DiaryListAdapter(emptyList()) { diaryId ->
            // 使用Bundle传递参数到详情页面
            val bundle = Bundle().apply {
                putString("diaryId", diaryId)
            }
            findNavController().navigate(R.id.diaryDetailFragment, bundle)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            findNavController().navigate(R.id.action_diaryListFragment_to_diaryEditorFragment)
        }

        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            findNavController().navigate(R.id.action_diaryListFragment_to_diarySettingsFragment)
        }

        loadDiaries()

        // 在 onViewCreated 方法中添加备忘录按钮
        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            findNavController().navigate(R.id.action_diaryListFragment_to_diarySettingsFragment)
        }

        // 添加备忘录按钮
        view.findViewById<ImageButton>(R.id.btnReminders)?.setOnClickListener {
            findNavController().navigate(R.id.action_diaryListFragment_to_reminderListFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDiaries()
    }

    private fun loadDiaries() {
        val files = diarySave.listAllFiles()
        val diaryEntries = files.mapNotNull { file ->
            try {
                val jsonString = file.readText(Charsets.UTF_8)
                val json = org.json.JSONObject(jsonString)

                val id = json.optString("id", file.nameWithoutExtension)
                val title = json.optString("title", "无标题")
                val content = json.optString("content", "")
                val createdAt = json.optLong("createdAt", file.lastModified())

                val dateFormatter = SimpleDateFormat("MM月dd日", Locale.getDefault())
                val dateLabel = dateFormatter.format(Date(createdAt))

                val preview = if (content.length > 50) content.substring(0, 50) + "..." else content

                DiaryEntry(id, title, preview, dateLabel, createdAt)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse diary file: ${file.name}")
                null
            }
        }.sortedByDescending { it.createdAt }

        adapter.updateData(diaryEntries)
    }
}

data class DiaryEntry(
    val id: String,
    val title: String,
    val preview: String,
    val dateLabel: String,
    val createdAt: Long
)

class DiaryListAdapter(
    private var diaries: List<DiaryEntry>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DiaryListAdapter.DiaryViewHolder>() {

    class DiaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 使用Android内置布局的ID
        val title: TextView = view.findViewById(android.R.id.text1)
        val subtitle: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        // 使用Android内置的simple_list_item_2布局
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diary = diaries[position]
        holder.title.text = diary.title
        holder.subtitle.text = "${diary.preview} • ${diary.dateLabel}"

        holder.itemView.setOnClickListener {
            onItemClick(diary.id)
        }
    }

    override fun getItemCount() = diaries.size

    fun updateData(newDiaries: List<DiaryEntry>) {
        diaries = newDiaries
        notifyDataSetChanged()
    }
}