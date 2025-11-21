package com.diary.cherry

import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.diary.utils.bgm.BackgroundMusicService.PlayMode
import com.diary.utils.bgm.MusicPlayerInterface
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private lateinit var musicPlayerInterface: MusicPlayerInterface
    private lateinit var sharedPreferences: SharedPreferences

    // 背景音乐相关视图
    private lateinit var musicSwitch: SwitchCompat
    private lateinit var currentMusicStatus: TextView
    private lateinit var currentMusicInfo: TextView
    private lateinit var btnPrevious: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnNext: Button
    private lateinit var btnSelectMusic: Button
    private lateinit var radioGroupPlayMode: RadioGroup
    private lateinit var seekbarVolume: SeekBar
    private lateinit var tvVolumeValue: TextView

    // 其他设置视图
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var seekbarFontSize: SeekBar
    private lateinit var btnDataManagement: Button
    private lateinit var btnAbout: Button

    private val handler = Handler(Looper.getMainLooper())
    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateMusicStatus()
            handler.postDelayed(this, 1000) // 每秒更新一次状态
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化音乐播放器接口
        musicPlayerInterface = MusicPlayerInterface(requireContext().applicationContext)

        // 初始化SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

        initMusicViews(view)
        loadSettings()
        setupMusicEventListeners()
        setupOtherEventListeners()

        // 绑定音乐服务
        bindMusicService()
    }

    private fun initMusicViews(view: View) {
        musicSwitch = view.findViewById(R.id.music_switch)
        currentMusicStatus = view.findViewById(R.id.current_music_status)
        currentMusicInfo = view.findViewById(R.id.current_music_info)
        btnPrevious = view.findViewById(R.id.btn_previous)
        btnPlayPause = view.findViewById(R.id.btn_play_pause)
        btnStop = view.findViewById(R.id.btn_stop)
        btnNext = view.findViewById(R.id.btn_next)
        btnSelectMusic = view.findViewById(R.id.btn_select_music)
        radioGroupPlayMode = view.findViewById(R.id.radio_group_play_mode)
        seekbarVolume = view.findViewById(R.id.seekbar_volume)
        tvVolumeValue = view.findViewById(R.id.tv_volume_value)

        // 其他设置视图
        themeSwitch = view.findViewById(R.id.theme_switch)
        seekbarFontSize = view.findViewById(R.id.seekbar_font_size)
        btnDataManagement = view.findViewById(R.id.btn_data_management)
        btnAbout = view.findViewById(R.id.btn_about)
    }

    private fun bindMusicService() {
        musicPlayerInterface.bindService { service ->
            if (service != null) {
                // 服务绑定成功
                updateControlButtonsState(true)
                updateMusicStatus()

                // 开始状态更新循环
                handler.post(statusUpdateRunnable)

                // 恢复之前的播放状态
                val wasPlaying = sharedPreferences.getBoolean("music_playing", false)
                if (wasPlaying && musicPlayerInterface.isPlaying()) {
                    updatePlayPauseButton(true)
                }
            } else {
                // 服务绑定失败
                updateControlButtonsState(false)
                currentMusicStatus.text = "状态：服务未就绪"
            }
        }
    }

    private fun loadSettings() {
        // 加载音乐设置
        val musicEnabled = sharedPreferences.getBoolean("music_enabled", true)
        musicSwitch.isChecked = musicEnabled
        updateMusicControlsVisibility(musicEnabled)

        val volume = sharedPreferences.getInt("music_volume", 50)
        seekbarVolume.progress = volume
        tvVolumeValue.text = "${volume}%"

        val playMode = sharedPreferences.getInt("play_mode", 0)
        when (playMode) {
            PlayMode.LIST_LOOP.ordinal -> radioGroupPlayMode.check(R.id.radio_list_loop) // 顺序播放
            PlayMode.SINGLE_LOOP.ordinal -> radioGroupPlayMode.check(R.id.radio_single_loop) // 单曲循环
            PlayMode.SHUFFLE.ordinal -> radioGroupPlayMode.check(R.id.radio_shuffle) // 随机播放
        }

        // 加载其他设置
        val darkTheme = sharedPreferences.getBoolean("dark_theme", false)
        themeSwitch.isChecked = darkTheme

        val fontSize = sharedPreferences.getInt("font_size", 14)
        seekbarFontSize.progress = fontSize
    }

    private fun setupMusicEventListeners() {
        // 音乐开关
        musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("music_enabled", isChecked) }
            updateMusicControlsVisibility(isChecked)

            if (isChecked) {
                // 如果之前有选择音乐，尝试播放
                if (musicPlayerInterface.getCurrentUri() != null) {
                    musicPlayerInterface.play()
                } else {
                    // 如果没有选择音乐，提示用户选择
                    Toast.makeText(requireContext(), "请先选择音乐文件", Toast.LENGTH_SHORT).show()
                }
            } else {
                musicPlayerInterface.pause()
                sharedPreferences.edit { putBoolean("music_playing", false) }
            }
        }

        // 控制按钮
        btnPrevious.setOnClickListener {
            musicPlayerInterface.previous()
            updateMusicStatus()
        }

        btnPlayPause.setOnClickListener {
            if (musicPlayerInterface.isPlaying()) {
                musicPlayerInterface.pause()
                sharedPreferences.edit { putBoolean("music_playing", false) }
            } else {
                musicPlayerInterface.play()
                sharedPreferences.edit { putBoolean("music_playing", true) }
            }
            updatePlayPauseButton(musicPlayerInterface.isPlaying())
        }

        btnStop.setOnClickListener {
            musicPlayerInterface.stop()
            sharedPreferences.edit { putBoolean("music_playing", false) }
            updatePlayPauseButton(false)
            updateMusicStatus()
        }

        btnNext.setOnClickListener {
            musicPlayerInterface.next()
            updateMusicStatus()
        }

        // 选择音乐
        btnSelectMusic.setOnClickListener {
            selectAndPlayMusic()
        }

        // 播放模式
        radioGroupPlayMode.setOnCheckedChangeListener { _, checkedId ->
            val playMode = when (checkedId) {
                R.id.radio_list_loop -> PlayMode.LIST_LOOP
                R.id.radio_single_loop -> PlayMode.SINGLE_LOOP
                R.id.radio_shuffle -> PlayMode.SHUFFLE
                else -> PlayMode.LIST_LOOP
            }
            musicPlayerInterface.setLooping(playMode)
            sharedPreferences.edit {
                putInt(
                    "play_mode",
                    when (playMode) {
                        PlayMode.SINGLE_LOOP -> 0
                        PlayMode.LIST_LOOP -> 1
                        PlayMode.SHUFFLE -> 2
                    }
                )
            }
        }

        // 音量控制
        seekbarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvVolumeValue.text = "${progress}%"
                    setMusicVolume(progress)
                    sharedPreferences.edit { putInt("music_volume", progress) }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupOtherEventListeners() {
        // 主题切换
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("dark_theme", isChecked) }
            applyTheme(isChecked)
        }

        // 字体大小
        seekbarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    sharedPreferences.edit { putInt("font_size", progress) }
                    applyFontSize(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 数据管理
        btnDataManagement.setOnClickListener {
            // TODO: 实现数据备份与恢复功能
            Toast.makeText(requireContext(), "数据管理功能开发中", Toast.LENGTH_SHORT).show()
        }

        // 关于应用
        btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun selectAndPlayMusic() {
        lifecycleScope.launch {
            try {
                musicPlayerInterface.selectAndPlayMusic()
                sharedPreferences.edit { putBoolean("music_playing", true) }

                // 延迟一下再更新状态，给服务一点时间
                handler.postDelayed({
                    updateMusicStatus()
                    updatePlayPauseButton(true)
                }, 500)

                Toast.makeText(requireContext(), "音乐选择成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "选择音乐失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMusicStatus() {
        if (!::musicPlayerInterface.isInitialized) return

        val isPlaying = musicPlayerInterface.isPlaying()
        val currentUri = musicPlayerInterface.getCurrentUri()

        currentMusicStatus.text = "状态：${if (isPlaying) "播放中" else "已暂停"}"

        currentUri?.let { uri ->
            // 从URI中提取文件名
            val pathSegments = uri.pathSegments
            val fileName = pathSegments.lastOrNull() ?: "未知文件"
            currentMusicInfo.text = "当前歌曲：$fileName"
        } ?: run {
            currentMusicInfo.text = "当前歌曲：无"
        }

        updatePlayPauseButton(isPlaying)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        btnPlayPause.text = if (isPlaying) "暂停" else "播放"
    }

    private fun updateControlButtonsState(enabled: Boolean) {
        btnPrevious.isEnabled = enabled
        btnPlayPause.isEnabled = enabled
        btnStop.isEnabled = enabled
        btnNext.isEnabled = enabled
        btnSelectMusic.isEnabled = enabled
        radioGroupPlayMode.isEnabled = enabled
        seekbarVolume.isEnabled = enabled
    }

    private fun updateMusicControlsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        currentMusicStatus.visibility = visibility
        currentMusicInfo.visibility = visibility
        btnPrevious.visibility = visibility
        btnPlayPause.visibility = visibility
        btnStop.visibility = visibility
        btnNext.visibility = visibility
        btnSelectMusic.visibility = visibility
        radioGroupPlayMode.visibility = visibility
        seekbarVolume.visibility = visibility
        tvVolumeValue.visibility = visibility

        // 查找音量控制标题的父布局并设置可见性
        val volumeLayout = seekbarVolume.parent as? ViewGroup
        volumeLayout?.visibility = visibility
    }

    private fun setMusicVolume(volume: Int) {
        // 设置系统媒体音量
        val audioManager = requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (volume / 100.0 * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    private fun applyTheme(isDark: Boolean) {
        // TODO: 实现主题切换逻辑
        Toast.makeText(requireContext(),
            "已切换到${if (isDark) "深色" else "浅色"}主题", Toast.LENGTH_SHORT).show()
    }

    private fun applyFontSize(size: Int) {
        // TODO: 实现字体大小调整逻辑
        Toast.makeText(requireContext(), "字体大小已设置为: $size", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("关于 Cherry Diary")
            .setMessage("Cherry Diary v1.0.0\n\n一个简洁优雅的日记和备忘录应用\n\n开发团队：Cherry Team")
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 停止状态更新
        handler.removeCallbacks(statusUpdateRunnable)

        // 保存当前播放状态
        sharedPreferences.edit { putBoolean("music_playing", musicPlayerInterface.isPlaying()) }

        // 不解绑服务，让音乐在后台继续播放
        // musicPlayerInterface.unbindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 应用退出时释放资源
        musicPlayerInterface.release()
    }
}