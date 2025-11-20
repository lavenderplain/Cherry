package com.diary.cherry.ui

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R
import com.diary.utils.DiarySave
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class DiaryDetailFragment : Fragment() {

    private lateinit var diarySave: DiarySave
    private lateinit var diaryId: String
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diarySave = DiarySave(requireContext())
        diaryId = arguments?.getString("diaryId") ?: run {
            Snackbar.make(view, "日记ID不存在", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        loadDiaryData(view)

        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            val bundle = Bundle().apply {
                putString("diaryId", diaryId)
            }
            findNavController().navigate(R.id.diaryEditorFragment, bundle)
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            deleteDiary(view)
        }

        view.findViewById<Button>(R.id.btnShare).setOnClickListener {
            shareDiary(view)
        }

        // 音乐播放控制
        view.findViewById<Button>(R.id.btnPlayMusicDetail).setOnClickListener {
            playMusic()
        }

        view.findViewById<Button>(R.id.btnStopMusicDetail).setOnClickListener {
            stopMusic()
        }
    }

    private fun loadDiaryData(view: View) {
        val jsonString = diarySave.loadAsJsonString(diaryId)
        if (jsonString != null) {
            try {
                val json = org.json.JSONObject(jsonString)

                val title = json.optString("title", "无标题")
                val content = json.optString("content", "")
                val createdAt = json.optLong("createdAt", System.currentTimeMillis())

                val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                val dateString = dateFormatter.format(Date(createdAt))

                view.findViewById<TextView>(R.id.tvTitle).text = title
                view.findViewById<TextView>(R.id.tvDate).text = dateString
                view.findViewById<TextView>(R.id.tvContent).text = content

                // 显示表情
                val emojis = diarySave.getEmojis(diaryId)
                val emojiContainer = view.findViewById<LinearLayout>(R.id.emojiContainerDetail)
                emojiContainer.removeAllViews()
                if (emojis.isNotEmpty()) {
                    emojis.forEach { emoji ->
                        val textView = TextView(requireContext()).apply {
                            text = emoji
                            textSize = 24f
                            setPadding(8, 4, 8, 4)
                        }
                        emojiContainer.addView(textView)
                    }
                } else {
                    val textView = TextView(requireContext()).apply {
                        text = "无表情"
                        textSize = 14f
                        setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    }
                    emojiContainer.addView(textView)
                }

                // 显示手写图片 - 直接显示第一张图片
                val handwritingPaths = diarySave.getHandwritingPaths(diaryId)
                val handwritingImage = view.findViewById<ImageView>(R.id.ivHandwriting)

                if (handwritingPaths.isNotEmpty()) {
                    // 显示第一张手写图片
                    try {
                        val bitmap = BitmapFactory.decodeFile(handwritingPaths[0])
                        handwritingImage.setImageBitmap(bitmap)
                        handwritingImage.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load handwriting image")
                        handwritingImage.visibility = View.GONE
                    }
                } else {
                    handwritingImage.visibility = View.GONE
                }

                // 显示音乐信息
                val musicPath = diarySave.getMusicPath(diaryId)
                view.findViewById<TextView>(R.id.tvMusicInfo).text =
                    if (musicPath != null) "已添加背景音乐"
                    else "无背景音乐"

            } catch (e: Exception) {
                Timber.e(e, "Failed to parse diary data")
                Snackbar.make(view, "加载日记失败", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(view, "日记不存在", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun playMusic() {
        val musicPath = diarySave.getMusicPath(diaryId)
        musicPath?.let { path ->
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
                Snackbar.make(requireView(), "开始播放背景音乐", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Timber.e(e, "Failed to play music")
                Snackbar.make(requireView(), "播放音乐失败", Snackbar.LENGTH_SHORT).show()
            }
        } ?: run {
            Snackbar.make(requireView(), "此日记没有背景音乐", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Snackbar.make(requireView(), "停止播放音乐", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
    }

    private fun deleteDiary(view: View) {
        try {
            diarySave.delete(diaryId)
            Snackbar.make(view, "日记已删除", Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete diary")
            Snackbar.make(view, "删除失败", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun shareDiary(view: View) {
        val jsonString = diarySave.loadAsJsonString(diaryId)
        if (jsonString != null) {
            try {
                val json = org.json.JSONObject(jsonString)
                val title = json.optString("title", "无标题")
                val content = json.optString("content", "")

                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$content")
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "分享日记"))

            } catch (e: Exception) {
                Timber.e(e, "Failed to share diary")
                Snackbar.make(view, "分享失败", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}