package com.diary.cherry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.diary.utils.memo.MemoInterface
import com.diary.utils.memo.Memo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.diary.cherry.MemoAdapter.MemoItem

class MemoFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyStateText: TextView
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var memoInterface: MemoInterface

    private val memoList = mutableListOf<MemoItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初始化MemoInterface
        memoInterface = MemoInterface(requireContext().applicationContext)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeMemos()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.memo_recycler_view)
        fabAdd = view.findViewById(R.id.fab_add_memo)
        emptyStateText = view.findViewById(R.id.empty_state_text)
    }

    private fun setupRecyclerView() {
//        memoAdapter = MemoAdapter(memoList) { memo ->
//            // 点击备忘录项的回调 - 使用Navigation跳转
//            val action = MemoFragmentDirections.actionMemoFragmentToMemoDetailActivity(
//                memoId = memo.id,
//                memoTitle = memo.title,
//                memoContent = memo.content
//            )
//            findNavController().navigate(action)
//        }
        memoAdapter = MemoAdapter { memo ->
            openMemoDetail(memo)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memoAdapter
        }

        // updateEmptyState()
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
//            // 新建备忘录 - 使用Navigation跳转
//            val action = MemoFragmentDirections.actionMemoFragmentToMemoDetailActivity()
//            findNavController().navigate(action)
            openMemoDetail(null) // null表示新建备忘录
        }
    }

    private fun observeMemos() {
        lifecycleScope.launch {
            memoInterface.getAllMemos().collectLatest { memos ->
                val memoItems = memos.map { memo ->
                    MemoAdapter.MemoItem(
                        id = memo.id,
                        title = memo.title,
                        content = memo.content,
                        reminderType = memo.reminderType,
                        reminderTime = memo.reminderTime,
                        isCompleted = memo.isCompleted,
                        createdAt = memo.createdAt,
                        // updatedAt = memo.updatedAt
                    )
                }

                // 按更新时间倒序排序
                // TODO: 考虑排序方式和memo数据结构调整
                // val sortedItems = memoItems.sortedByDescending { it.updatedAt }
                val sortedItems = memoItems.sortedByDescending { it.createdAt }

                memoAdapter.submitList(sortedItems)
                updateEmptyState(sortedItems)
            }
        }
    }

//    private fun loadMemos() {
//        // TODO: 实现从数据库或文件加载备忘录的逻辑
//        memoList.clear()
//        // 示例数据
//        memoList.addAll(listOf(
//            MemoItem("1", "购物清单", "牛奶、鸡蛋、面包、水果、蔬菜", System.currentTimeMillis()),
//            MemoItem("2", "会议提醒", "下午3点团队会议，记得准备项目进度报告", System.currentTimeMillis() - 3600000),
//            MemoItem("3", "学习计划", "完成Android作业，复习Kotlin语法，准备期末考试", System.currentTimeMillis() - 86400000)
//        ))
//        memoAdapter.updateData(memoList)
//        updateEmptyState()
//    }

    private fun openMemoDetail(memo: MemoAdapter.MemoItem?) {
        val intent = Intent(requireContext(), MemoDetailActivity::class.java).apply {
            memo?.let {
                putExtra("memo_id", it.id)
                putExtra("memo_title", it.title)
                putExtra("memo_content", it.content)
                putExtra("memo_reminder_type", it.reminderType.ordinal)
                putExtra("memo_reminder_time", it.reminderTime)
                putExtra("memo_completed", it.isCompleted)
            }
        }
        startActivity(intent)
    }

    private fun updateEmptyState(memoItems: List<MemoAdapter.MemoItem>) {
        emptyStateText.visibility = if (memoList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // 当从详情页面返回时重新加载数据
        observeMemos()
    }

    data class MemoItem_(
        val id: String,
        val title: String,
        val content: String,
        val createTime: Long
    )
}