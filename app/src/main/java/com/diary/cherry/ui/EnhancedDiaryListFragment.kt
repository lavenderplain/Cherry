package com.diary.cherry.ui

import android.view.View
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.diary.utils.DiarySave
import timber.log.Timber

class EnhancedDiaryListFragment : DiaryListFragment() {
    private lateinit var diarySave: DiarySave

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        diarySave = DiarySave(requireContext())
        
        setupDeleteButton()
    }

    private fun setupDeleteButton() {
        val deleteButton = view?.findViewById<Button>(R.id.btnDelete)
        deleteButton?.setOnClickListener {
            val diaryId = "current_selected_diary_id"
            deleteDiary(diaryId)
        }
    }

    private fun deleteDiary(id: String) {
        try {
            val deleted = diarySave.delete(id)
            if (deleted) {
                refreshDiaryList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete diary")
        }
    }

    private fun refreshDiaryList() {
    }
}
