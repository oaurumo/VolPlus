package com.wells.volplus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * 音量调节主界面。
 *
 * 支持调节「媒体 / 铃声 / 通知 / 闹钟 / 通话」五类音量，
 * 提供滑块、加减、静音、恢复（含全局静音 / 恢复）等操作。
 *
 * 兼容范围：minSdk 23 (Android 6.0) ~ targetSdk 37 (Android 17)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var prefs: SharedPreferences

    /** 是否在调节音量时弹出系统音量面板（VolumeDialog） */
    private var showVolumeDialog: Boolean = false

    /** 需要管理的音频流定义 */
    private data class StreamDef(
        val type: Int,
        val name: String
    )

    private val streamDefs = listOf(
        StreamDef(AudioManager.STREAM_MUSIC, "媒体"),
        StreamDef(AudioManager.STREAM_RING, "铃声"),
        StreamDef(AudioManager.STREAM_NOTIFICATION, "通知"),
        StreamDef(AudioManager.STREAM_ALARM, "闹钟"),
        StreamDef(AudioManager.STREAM_VOICE_CALL, "通话")
    )

    /** 每个音频流的视图与状态 */
    private inner class StreamHolder(
        val def: StreamDef,
        val titleView: TextView,
        val valueView: TextView,
        val seekBar: SeekBar,
        val btnMinus: Button,
        val btnMute: Button,
        val btnPlus: Button,
        val btnRestore: Button
    ) {
        var savedVolume: Int = -1   // 静音前保存的音量，-1 表示未静音
        var maxVolume: Int = 0
        var isMuted: Boolean = false
    }

    private val holders = mutableListOf<StreamHolder>()

    /** 监听系统音量变化（例如用户按物理音量键），同步刷新 UI */
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                refreshAllHolders()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        showVolumeDialog = prefs.getBoolean(KEY_SHOW_DIALOG, false)
        setContentView(buildContentView())
    }

    override fun onResume() {
        super.onResume()
        // 进入前台时刷新一次，并注册音量变化监听
        refreshAllHolders()
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(volumeReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(volumeReceiver)
        } catch (_: IllegalArgumentException) {
            // 未注册过则忽略
        }
    }

    // ------------------------------------------------------------------
    //  视图构建
    // ------------------------------------------------------------------

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        // 标题
        val title = TextView(this).apply {
            text = "音量调节"
            textSize = 22f
            setTextColor(0xFF222222.toInt())
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(title)

        // VolumeDialog 开关
        root.addView(buildDialogSwitchCard())

        // 全局操作行：全部静音 / 全部恢复
        val globalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        val btnMuteAll = Button(this).apply {
            text = "全部静音"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { muteAll() }
        }
        val btnRestoreAll = Button(this).apply {
            text = "全部恢复"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { restoreAll() }
        }
        globalRow.addView(btnMuteAll)
        globalRow.addView(btnRestoreAll)
        root.addView(globalRow)

        // 各音频流卡片
        for (def in streamDefs) {
            root.addView(buildStreamCard(def))
        }

        // 卡片较多，需支持滚动
        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }

    /** 构建「调节时显示系统音量面板」开关卡片 */
    private fun buildDialogSwitchCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card)
        }

        val switch = SwitchMaterial(this).apply {
            text = "调节时显示 VolumeDialog"
            textSize = 15f
            isChecked = showVolumeDialog
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, checked ->
                showVolumeDialog = checked
                prefs.edit().putBoolean(KEY_SHOW_DIALOG, checked).apply()
            }
        }
        card.addView(switch)

        val hint = TextView(this).apply {
            text = "开启后，用本应用调整音量会弹出系统音量面板"
            textSize = 11f
            setTextColor(0xFF888888.toInt())
            setPadding(dp(2), 0, 0, dp(8))
        }
        card.addView(hint)
        return card
    }

    private fun buildStreamCard(def: StreamDef): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            // 圆角背景
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card)
        }

        // 标题行：名称 + 当前数值
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleView = TextView(this).apply {
            text = def.name
            textSize = 16f
            setTextColor(0xFF222222.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val valueView = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }
        titleRow.addView(titleView)
        titleRow.addView(valueView)
        card.addView(titleRow)

        // 滑块
        val seekBar = SeekBar(this).apply {
            max = audioManager.getStreamMaxVolume(def.type)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setStreamVolume(def, progress)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        card.addView(seekBar)

        // 按钮行：-  静音  +  恢复
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        val btnMinus = Button(this).apply {
            text = "−"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { adjustVolume(def, -1) }
        }
        val btnMute = Button(this).apply {
            text = "静音"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { toggleMute(def) }
        }
        val btnPlus = Button(this).apply {
            text = "+"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { adjustVolume(def, 1) }
        }
        val btnRestore = Button(this).apply {
            text = "恢复"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { restoreStream(def) }
        }
        btnRow.addView(btnMinus)
        btnRow.addView(btnMute)
        btnRow.addView(btnPlus)
        btnRow.addView(btnRestore)
        card.addView(btnRow)

        val holder = StreamHolder(
            def = def,
            titleView = titleView,
            valueView = valueView,
            seekBar = seekBar,
            btnMinus = btnMinus,
            btnMute = btnMute,
            btnPlus = btnPlus,
            btnRestore = btnRestore
        ).also {
            it.maxVolume = audioManager.getStreamMaxVolume(def.type)
        }
        holders.add(holder)
        return card
    }

    // ------------------------------------------------------------------
    //  音量操作
    // ------------------------------------------------------------------

    private fun findHolder(type: Int): StreamHolder? =
        holders.firstOrNull { it.def.type == type }

    /**
     * 写入音量。开关开启时附带 FLAG_SHOW_UI，
     * 由系统弹出 VolumeDialog 音量面板。
     */
    private fun applyVolume(streamType: Int, value: Int) {
        val flags = if (showVolumeDialog) AudioManager.FLAG_SHOW_UI else 0
        audioManager.setStreamVolume(streamType, value, flags)
    }

    private fun setStreamVolume(def: StreamDef, value: Int) {
        val clamped = value.coerceIn(0, audioManager.getStreamMaxVolume(def.type))
        applyVolume(def.type, clamped)
        val holder = findHolder(def.type) ?: return
        // 手动调节后若之前处于静音态，则取消静音态
        if (holder.isMuted) {
            holder.isMuted = false
            holder.savedVolume = -1
        }
        updateHolderDisplay(holder)
    }

    private fun adjustVolume(def: StreamDef, delta: Int) {
        val holder = findHolder(def.type) ?: return
        val cur = audioManager.getStreamVolume(def.type)
        setStreamVolume(def, cur + delta)
        updateHolderDisplay(holder)
    }

    /** 切换单个流的静音状态 */
    private fun toggleMute(def: StreamDef) {
        val holder = findHolder(def.type) ?: return
        if (holder.isMuted) {
            restoreStream(def)
        } else {
            val cur = audioManager.getStreamVolume(def.type)
            if (cur > 0) {
                holder.savedVolume = cur
            }
            applyVolume(def.type, 0)
            holder.isMuted = true
            updateHolderDisplay(holder)
        }
    }

    /** 恢复单个流到静音前的音量 */
    private fun restoreStream(def: StreamDef) {
        val holder = findHolder(def.type) ?: return
        if (holder.isMuted && holder.savedVolume >= 0) {
            applyVolume(def.type, holder.savedVolume)
        }
        holder.isMuted = false
        holder.savedVolume = -1
        updateHolderDisplay(holder)
    }

    /** 全部静音：保存各流当前音量并置零 */
    private fun muteAll() {
        for (holder in holders) {
            if (!holder.isMuted) {
                val cur = audioManager.getStreamVolume(holder.def.type)
                if (cur > 0) {
                    holder.savedVolume = cur
                }
                applyVolume(holder.def.type, 0)
                holder.isMuted = true
            }
        }
        refreshAllHolders()
        Toast.makeText(this, "已全部静音", Toast.LENGTH_SHORT).show()
    }

    /** 全部恢复：将各流恢复到静音前的音量 */
    private fun restoreAll() {
        for (holder in holders) {
            if (holder.isMuted && holder.savedVolume >= 0) {
                applyVolume(holder.def.type, holder.savedVolume)
            }
            holder.isMuted = false
            holder.savedVolume = -1
        }
        refreshAllHolders()
        Toast.makeText(this, "已全部恢复", Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------------
    //  UI 刷新
    // ------------------------------------------------------------------

    private fun refreshAllHolders() {
        for (holder in holders) {
            updateHolderDisplay(holder)
        }
    }

    private fun updateHolderDisplay(holder: StreamHolder) {
        val cur = audioManager.getStreamVolume(holder.def.type)
        val max = holder.maxVolume
        holder.seekBar.max = max
        holder.seekBar.progress = cur
        holder.valueView.text = "$cur / $max"
        holder.btnMute.text = if (holder.isMuted || cur == 0) "取消静音" else "静音"
        val isZero = cur == 0
        holder.valueView.setTextColor(
            if (isZero) 0xFFE53935.toInt() else 0xFF666666.toInt()
        )
    }

    // ------------------------------------------------------------------
    //  工具
    // ------------------------------------------------------------------

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val PREFS_NAME = "volplus_settings"
        const val KEY_SHOW_DIALOG = "show_volume_dialog"
    }
}
