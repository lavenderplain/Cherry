package com.diary.cherry.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.diary.cherry.R

class DiarySettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val darkSwitch = view.findViewById<Switch>(R.id.switchDarkMode)
        view.findViewById<Button>(R.id.btnExport).setOnClickListener {
            // TODO: DiarySettingsActions.onExport()
        }
        view.findViewById<Button>(R.id.btnImport).setOnClickListener {
            // TODO: DiarySettingsActions.onImport()
        }
        darkSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: DiarySettingsActions.onToggleDarkMode(isChecked)
        }
    }
}