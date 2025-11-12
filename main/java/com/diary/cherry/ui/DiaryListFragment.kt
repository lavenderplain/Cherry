package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R

class DiaryListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            findNavController().navigate(R.id.action_diaryListFragment_to_diaryEditorFragment)
        }
        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            findNavController().navigate(R.id.action_diaryListFragment_to_diarySettingsFragment)
        }
        // TODO: 绑定 RecyclerView 与数据源；点击项 -> navigate 到详情
    }
}