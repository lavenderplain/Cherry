package com.diary.cherry.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.diary.cherry.R
import com.diary.utils.DiarySave
import com.google.android.material.textfield.TextInputEditText
import timber.log.Timber
import java.util.UUID

class EnhancedDiaryEditorFragment : DiaryEditorFragment() {
    private lateinit var diarySave: DiarySave
    private var diaryId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_enhanced_diary_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        diarySave = DiarySave(requireContext())
        diaryId = arguments?.getString("diaryId")

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveDiary()
        }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<Button>(R.id.btnEmoji).setOnClickListener {
            openEmojiPicker()
        }

        diaryId?.let { loadDiaryContent(it) }
    }

    private fun saveDiary() {
        val title = view?.findViewById<TextInputEditText>(R.id.etTitle)?.text?.toString()
        val content = view?.findViewById<TextInputEditText>(R.id.etContent)?.text?.toString()

        if (content.isNullOrEmpty()) return

        try {
            val id = diaryId ?: UUID.randomUUID().toString()
            diarySave.saveOrUpdate(id, content, title)
            findNavController().navigateUp()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save diary")
        }
    }

    private fun loadDiaryContent(id: String) {
        val content = diarySave.loadContent(id)
        val jsonString = diarySave.loadAsJsonString(id)
        val title = jsonString?.let { 
            try {
                org.json.JSONObject(it).optString("title")
            } catch (e: Exception) {
                null
            }
        }

        view?.findViewById<TextInputEditText>(R.id.etTitle)?.setText(title)
        view?.findViewById<TextInputEditText>(R.id.etContent)?.setText(content)
    }

    private fun openEmojiPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
    }
}
