package com.diary.cherry

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.diary.utils.DiarySave
import kotlinx.coroutines.launch
import org.json.JSONObject

// TODO: 添加删除日记功能

class DiaryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyStateText: TextView
    private lateinit var diaryAdapter: DiaryAdapter
    private lateinit var diarySave: DiarySave

    // 本地列表不再需要手动维护，改为使用适配器的数据源
    // private val diaryList = mutableListOf<DiaryItem>()

    /**
     * 创建视图
     * @param inflater 布局填充器
     * @param container 容器
     * @param savedInstanceState 保存的实例状态
     * @return 视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    /**
     * 视图创建完成后的初始化
     * @param view 视图
     * @param savedInstanceState 保存的实例状态
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化DiarySave
        diarySave = DiarySave(requireContext().applicationContext)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadDiaries()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.diary_recycler_view)
        fabAdd = view.findViewById(R.id.fab_add_diary)
        emptyStateText = view.findViewById(R.id.empty_state_text)
    }

    private fun setupRecyclerView() {
        diaryAdapter = DiaryAdapter { diary ->
            // 点击日记项的回调
            openDiaryDetail(diary)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter
        }

        updateEmptyState(emptyList())
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            openDiaryDetail(null) // null表示新建日记
        }
    }

    private fun openDiaryDetail(diary: DiaryItem?) {
        val intent = Intent(requireContext(), DiaryDetailActivity::class.java).apply {
            diary?.let {
                putExtra("diary_id", it.id)
                putExtra("diary_title", it.title)
                putExtra("diary_content", it.content)
            }
        }
        startActivity(intent)
    }

    private fun updateEmptyState(diaryItems: List<DiaryItem>) {
        emptyStateText.visibility = if (diaryItems.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // 当从详情页面返回时重新加载数据
        loadDiaries()
    }

    private fun loadDiaries() {
        lifecycleScope.launch {
            try {
                val files = diarySave.listAllFiles()
                val diaryItems = mutableListOf<DiaryItem>()

                files.forEach { file ->
                    try {
                        val jsonString = file.readText(Charsets.UTF_8)
                        val jsonObject = JSONObject(jsonString)

                        val id = jsonObject.getString("id")
                        val title = jsonObject.getString("title")
                        val content = jsonObject.getString("content")
                        val createdAt = jsonObject.getLong("createdAt")
                        val updatedAt = jsonObject.getLong("updatedAt")

                        diaryItems.add(DiaryItem(id, title, content, createdAt, updatedAt))
                    } catch (e: Exception) {
                        // 跳过无法解析的文件
                        e.printStackTrace()
                    }
                }

                // 按更新时间倒序排序
                diaryItems.sortByDescending { it.updatedAt }

                requireActivity().runOnUiThread {
                    diaryAdapter.submitList(diaryItems)
                    updateEmptyState(diaryItems)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 显示错误状态
                requireActivity().runOnUiThread {
                    emptyStateText.text = "加载日记失败"
                    emptyStateText.visibility = TextView.VISIBLE
                }
            }
        }
    }

    data class DiaryItem(
        val id: String,
        val title: String,
        val content: String,
        val createdAt: Long,
        val updatedAt: Long
    )
}