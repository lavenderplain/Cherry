package com.diary.cherry.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.diary.cherry.R
import com.diary.utils.DiarySave
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

import com.diary.utils.bgm.MusicSelector
class DiaryEditorFragment : Fragment() {

    private lateinit var diarySave: DiarySave
    private var diaryId: String = ""
    private var selectedDate = Date()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 新功能相关变量
    private val emojis = listOf("😊", "😂", "🥰", "😎", "🤔", "😢", "😡", "🎉", "❤️", "⭐")
    private var selectedEmojis = mutableListOf<String>()
    private var handwritingPaths = mutableListOf<String>()
    private var musicPath: String? = null
    private var mediaPlayer: MediaPlayer? = null

    // 手写相关变量
    private lateinit var handwritingView: HandwritingView
    private lateinit var previewImage: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diarySave = DiarySave(requireContext())
        diaryId = arguments?.getString("diaryId", "") ?: ""

        val titleEt = view.findViewById<TextInputEditText>(R.id.etTitle)
        val contentEt = view.findViewById<TextInputEditText>(R.id.etContent)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)

        // 初始化新功能视图
        initNewFeatures(view)

        if (diaryId.isNotEmpty()) {
            loadDiaryData(diaryId, titleEt, contentEt, btnPickDate)
        } else {
            updateDateButtonText(btnPickDate)
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val title = titleEt.text?.toString() ?: ""
            val content = contentEt.text?.toString() ?: ""

            if (title.isBlank() && content.isBlank()) {
                Snackbar.make(view, "标题和内容不能同时为空", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveDiary(diaryId, title, content, view)
        }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            mediaPlayer?.release()
            findNavController().navigateUp()
        }

        btnPickDate.setOnClickListener {
            showDatePickerDialog(btnPickDate)
        }
    }

    private fun initNewFeatures(view: View) {
        // 表情选择器
        val emojiContainer = view.findViewById<LinearLayout>(R.id.emojiContainer)
        emojis.forEach { emoji ->
            val textView = TextView(requireContext()).apply {
                text = emoji
                textSize = 20f
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    if (selectedEmojis.contains(emoji)) {
                        selectedEmojis.remove(emoji)
                        setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        selectedEmojis.add(emoji)
                        setBackgroundColor(Color.LTGRAY)
                    }
                }
            }
            emojiContainer.addView(textView)
        }

        // 手写功能
        handwritingView = view.findViewById(R.id.handwritingView)
        handwritingView.setupDrawing()

        previewImage = view.findViewById(R.id.ivHandwritingPreview)

        view.findViewById<Button>(R.id.btnClearHandwriting).setOnClickListener {
            handwritingView.clear()
        }

        view.findViewById<Button>(R.id.btnSaveHandwriting).setOnClickListener {
            saveHandwriting()
        }

        // 音乐功能
        view.findViewById<Button>(R.id.btnSelectMusic).setOnClickListener {
            selectMusicFile()
        }

        view.findViewById<Button>(R.id.btnPlayMusic).setOnClickListener {
            playSelectedMusic()
            // MusicSelector.queryMusicFiles(context = , )
        }

        view.findViewById<Button>(R.id.btnStopMusic).setOnClickListener {
            stopMusic()
        }
    }

    private fun saveHandwriting() {
        val bitmap = handwritingView.getBitmap()
        if (bitmap != null) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    // 保存原始尺寸的图片
                    val originalStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, originalStream)
                    val originalImageData = originalStream.toByteArray()

                    // 清空之前的手写图片，只保留一张
                    handwritingPaths.clear()

                    val path = diarySave.saveHandwritingImage(originalImageData, diaryId.ifEmpty { "new" })
                    handwritingPaths.add(path)

                    // 创建预览图片
                    val previewStream = ByteArrayOutputStream()
                    val previewBitmap = createPreviewBitmap(bitmap)
                    previewBitmap.compress(Bitmap.CompressFormat.PNG, 100, previewStream)

                    requireActivity().runOnUiThread {
                        Snackbar.make(requireView(), "手写内容已保存", Snackbar.LENGTH_SHORT).show()
                        // 更新预览
                        previewImage.setImageBitmap(previewBitmap)
                        previewImage.visibility = View.VISIBLE
                        handwritingView.clear()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save handwriting")
                    requireActivity().runOnUiThread {
                        Snackbar.make(requireView(), "保存手写失败", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Snackbar.make(requireView(), "请先进行手写", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun createPreviewBitmap(originalBitmap: Bitmap): Bitmap {
        val previewWidth = resources.displayMetrics.widthPixels - 100 // 留出边距
        val previewHeight = 300 // 固定预览高度

        // 计算缩放比例，保持宽高比
        val scale = Math.min(
            previewWidth.toFloat() / originalBitmap.width,
            previewHeight.toFloat() / originalBitmap.height
        ).coerceAtMost(1.0f)

        val scaledWidth = (originalBitmap.width * scale).toInt()
        val scaledHeight = (originalBitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    }

    private val musicPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val musicData = inputStream?.readBytes()
                    inputStream?.close()

                    musicData?.let { data ->
                        val fileName = uri.lastPathSegment ?: "music_${System.currentTimeMillis()}"
                        musicPath = diarySave.saveMusicFile(data, fileName)
                        requireActivity().runOnUiThread {
                            view?.findViewById<TextView>(R.id.tvSelectedMusic)?.text =
                                "已选择: ${fileName.takeLast(20)}"
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load music file")
                    requireActivity().runOnUiThread {
                        Snackbar.make(requireView(), "选择音乐失败", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun selectMusicFile() {
        musicPicker.launch("audio/*")
    }

    private fun playSelectedMusic() {
        musicPath?.let { path ->
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to play music")
                Snackbar.make(requireView(), "播放音乐失败", Snackbar.LENGTH_SHORT).show()
            }
        } ?: run {
            Snackbar.make(requireView(), "请先选择音乐文件", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // 添加这个扩展函数
    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun loadDiaryData(diaryId: String, titleEt: TextInputEditText, contentEt: TextInputEditText, btnPickDate: Button) {
        val jsonString = diarySave.loadAsJsonString(diaryId)
        if (jsonString != null) {
            try {
                val json = org.json.JSONObject(jsonString)
                titleEt.setText(json.optString("title", ""))
                contentEt.setText(json.optString("content", ""))

                val createdAt = json.optLong("createdAt", System.currentTimeMillis())
                selectedDate = Date(createdAt)
                updateDateButtonText(btnPickDate)

                // 加载扩展数据
                selectedEmojis = diarySave.getEmojis(diaryId).toMutableList()
                handwritingPaths = diarySave.getHandwritingPaths(diaryId).toMutableList()
                musicPath = diarySave.getMusicPath(diaryId)

                // 更新UI显示已选择的表情
                updateSelectedEmojisUI()

                // 加载已有的手写图片预览
                if (handwritingPaths.isNotEmpty()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(handwritingPaths[0])
                        val previewBitmap = createPreviewBitmap(bitmap)
                        previewImage.setImageBitmap(previewBitmap)
                        previewImage.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load handwriting preview")
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to parse diary data")
            }
        }
    }

    private fun updateSelectedEmojisUI() {
        view?.findViewById<LinearLayout>(R.id.emojiContainer)?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i) as? TextView
                child?.let { tv ->
                    val emoji = tv.text.toString()
                    if (selectedEmojis.contains(emoji)) {
                        tv.setBackgroundColor(Color.LTGRAY)
                    } else {
                        tv.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
        }
    }

    private fun saveDiary(diaryId: String, title: String, content: String, view: View) {
        val finalId = if (diaryId.isEmpty()) UUID.randomUUID().toString() else diaryId

        GlobalScope.launch(Dispatchers.IO) {
            try {
                diarySave.saveOrUpdate(
                    finalId,
                    content,
                    if (title.isBlank()) null else title,
                    selectedEmojis,
                    handwritingPaths,
                    musicPath
                )

                requireActivity().runOnUiThread {
                    Snackbar.make(view, "日记保存成功", Snackbar.LENGTH_SHORT).show()
                    mediaPlayer?.release()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save diary")
                requireActivity().runOnUiThread {
                    Snackbar.make(view, "保存失败: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
    }

    private fun showDatePickerDialog(btnPickDate: Button) {
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = calendar.time
                updateDateButtonText(btnPickDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun updateDateButtonText(btnPickDate: Button) {
        btnPickDate.text = "日期: ${dateFormatter.format(selectedDate)}"
    }
}

// 改进的 HandwritingView 类
class HandwritingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var path = Path()
    private var paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var lastX = 0f
    private var lastY = 0f
    private var isDrawing = false

    init {
        // 启用可滚动容器内的触摸事件拦截
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setupDrawing() {
        post {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!)
            canvas!!.drawColor(Color.WHITE)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 请求父视图不拦截触摸事件
                parent.requestDisallowInterceptTouchEvent(true)
                isDrawing = true
                lastX = x
                lastY = y
                path.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    // 继续阻止父视图拦截
                    parent.requestDisallowInterceptTouchEvent(true)
                    path.lineTo(x, y)

                    // 绘制到bitmap上
                    canvas?.drawPath(path, paint)
                    path.reset()
                    path.moveTo(lastX, lastY)
                    path.lineTo(x, y)

                    lastX = x
                    lastY = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isDrawing = false
                // 绘制最后一段到bitmap
                canvas?.drawPath(path, paint)
                path.reset()
                // 释放触摸事件拦截
                parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                path.reset()
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    fun clear() {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
        canvas!!.drawColor(Color.WHITE)
        path.reset()
        invalidate()
    }

    fun getBitmap(): Bitmap? {
        return bitmap
    }
}