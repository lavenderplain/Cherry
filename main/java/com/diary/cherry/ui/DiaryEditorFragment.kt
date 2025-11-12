package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R
import com.google.android.material.textfield.TextInputEditText

class DiaryEditorFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleEt = view.findViewById<TextInputEditText>(R.id.etTitle)
        val contentEt = view.findViewById<TextInputEditText>(R.id.etContent)

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            // TODO: 调用 DiaryEditorActions.onSave(titleEt.text, contentEt.text, date)
            findNavController().navigateUp()
        }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            // TODO: 调用 DiaryEditorActions.onCancelEdit()
            findNavController().navigateUp()
        }
        view.findViewById<Button>(R.id.btnPickDate).setOnClickListener {
            // TODO: 弹出日期选择器
        }
    }
}