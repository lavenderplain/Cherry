package com.diary.cherry.ui

data class DiaryEntryUi(
    val id: Long,
    val title: String,
    val preview: String,
    val dateLabel: String
)

interface DiaryListActions {
    fun onAddEntry()
    fun onEntrySelected(id: Long)
    fun onOpenSettings()
}

interface DiaryEditorActions {
    fun onSave(title: String, content: String, dateMillis: Long?)
    fun onCancelEdit()
}

interface DiaryDetailActions {
    fun onEdit(id: Long)
    fun onDelete(id: Long)
    fun onShare(id: Long)
}

interface DiarySettingsActions {
    fun onToggleDarkMode(enabled: Boolean)
    fun onExport()
    fun onImport()
}