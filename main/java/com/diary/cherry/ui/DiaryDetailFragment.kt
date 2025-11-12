package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R

class DiaryDetailFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            // TODO: 传入当前条目 id
            findNavController().navigate(R.id.diaryEditorFragment)
        }
        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            // TODO: 调用 DiaryDetailActions.onDelete(id)
            findNavController().navigateUp()
        }
        view.findViewById<Button>(R.id.btnShare).setOnClickListener {
            // TODO: 调用 DiaryDetailActions.onShare(id)
        }
    }
}