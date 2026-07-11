package com.example.clock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var syncIndicator: TextView
    private lateinit var settingsBtn: TextView

    private val handler = Handler(Looper.getMainLooper())

    // NTP 校准得到的「本地时间 → 真实时间」偏移（毫秒）。null 表示尚未校准。
    private var offset: Long? = null

    private val shanghai = TimeZone.getTimeZone("Asia/Shanghai")
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply { timeZone = shanghai }
    private val dateFmt = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).apply { timeZone = shanghai }

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        syncIndicator = findViewById(R.id.syncIndicator)
        settingsBtn = findViewById(R.id.settingsBtn)

        fitTextSize()
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        handler.post(tick)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        syncTime() // 从设置返回后按新选择重新校准
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    // 根据屏宽自适应字号，保证 "HH:mm:ss" 一行铺满又不溢出
    private fun fitTextSize() {
        val dm = resources.displayMetrics
        val wDp = dm.widthPixels / dm.density
        val size = ((wDp * 0.85f) / (8f * 0.6f)).coerceIn(40f, 170f)
        timeText.textSize = size
        dateText.textSize = (size * 0.26f).coerceIn(18f, 40f)
    }

    private fun updateClock() {
        val now = System.currentTimeMillis() + (offset ?: 0)
        val cal = Calendar.getInstance(shanghai)
        cal.timeInMillis = now
        timeText.text = timeFmt.format(cal.time)
        dateText.text = dateFmt.format(cal.time)
    }

    // 优先用用户在设置里选的服务器；失败再静默回退其他服务器，绝不显示具体地址
    private fun syncTime() {
        val prefs = getSharedPreferences("clock", MODE_PRIVATE)
        val selected = ServerPrefs.getSelected(this)
        val custom = ServerPrefs.getCustomHosts(prefs)
        val defaults = ServerPrefs.defaultServers.map { it.second }
        val order = mutableListOf(selected)
        (defaults + custom).filter { it != selected }.toCollection(order)

        syncIndicator.text = "同步中…"
        thread {
            for (host in order) {
                try {
                    val off = SntpClient.requestTime(host)
                    offset = off
                    handler.post { syncIndicator.text = "已校准" }
                    return@thread
                } catch (e: Exception) {
                    // 尝试下一个
                }
            }
            handler.post { syncIndicator.text = "未校准 · 本机时间" }
        }
    }
}
