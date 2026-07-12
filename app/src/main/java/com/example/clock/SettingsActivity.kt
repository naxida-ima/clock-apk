package com.example.clock

import android.app.AlertDialog
import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

private data class ServerEntry(val name: String, val host: String, val custom: Boolean)

class SettingsActivity : Activity() {

    private lateinit var listView: ListView
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: ServerAdapter
    private val handler = Handler(Looper.getMainLooper())

    // 服务器地址 -> 响应延迟文案（毫秒），测速后填充
    private val latencyMap: MutableMap<String, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        prefs = getSharedPreferences("clock", MODE_PRIVATE)
        listView = findViewById(R.id.serverList)
        adapter = ServerAdapter()
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, pos, _ -> adapter.selectAt(pos) }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            adapter.requestDeleteAt(pos)
            true
        }
        findViewById<Button>(R.id.addBtn).setOnClickListener { showAddDialog() }
        findViewById<Button>(R.id.doneBtn).setOnClickListener { finish() }

        measureLatency()
    }

    // 对每个服务器做一次 SNTP 探测，取网络往返毫秒数显示在列表项
    private fun measureLatency() {
        val hosts = (ServerPrefs.defaultServers.map { it.second } +
                ServerPrefs.getCustomHosts(prefs)).toSet()
        thread {
            for (h in hosts) {
                val text = try {
                    val r = SntpClient.requestTimeWithDelay(h)
                    "延迟 ${r.delay} ms"
                } catch (e: Exception) {
                    "延迟 超时"
                }
                latencyMap[h] = text
                handler.post { adapter.notifyDataSetChanged() }
            }
        }
    }

    private fun showAddDialog() {
        val et = EditText(this)
        et.hint = "例如 ntp.example.com"
        AlertDialog.Builder(this)
            .setTitle("添加自定义服务器")
            .setMessage("填写 NTP 服务器地址（UDP 端口 123）")
            .setView(et)
            .setPositiveButton("添加") { _, _ ->
                val h = et.text.toString().trim()
                if (h.isNotEmpty()) adapter.addCustom(h)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private inner class ServerAdapter : BaseAdapter() {

        private val inflater = LayoutInflater.from(this@SettingsActivity)
        private var entries: MutableList<ServerEntry> = buildEntries()

        private fun buildEntries(): MutableList<ServerEntry> {
            val list = mutableListOf<ServerEntry>()
            for ((n, h) in ServerPrefs.defaultServers) list.add(ServerEntry(n, h, false))
            for (h in ServerPrefs.getCustomHosts(prefs)) list.add(ServerEntry(h, h, true))
            return list
        }

        fun selectAt(pos: Int) {
            val e = entries[pos]
            prefs.edit().putString("selected_server", e.host).apply()
            val matched = ServerPrefs.recordSelection(this@SettingsActivity, e.host)
            notifyDataSetChanged()
            if (matched) {
                val on = !ServerPrefs.isOnlineOn(this@SettingsActivity)
                ServerPrefs.setOnlineOn(this@SettingsActivity, on)
                Toast.makeText(
                    this@SettingsActivity,
                    if (on) "实时在线人数：开" else "实时在线人数：关",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this@SettingsActivity, "已选择：${e.name}", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        fun addCustom(host: String) {
            val cur = ServerPrefs.getCustomHosts(prefs).toMutableList()
            if (cur.any { it.equals(host, true) }) {
                Toast.makeText(this@SettingsActivity, "已存在", Toast.LENGTH_SHORT).show()
                return
            }
            cur.add(host)
            ServerPrefs.setCustomHosts(prefs, cur)
            entries = buildEntries()
            notifyDataSetChanged()
            measureLatency()
        }

        fun requestDeleteAt(pos: Int) {
            val e = entries[pos]
            if (!e.custom) {
                Toast.makeText(this@SettingsActivity, "默认服务器不可删除", Toast.LENGTH_SHORT).show()
                return
            }
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("删除服务器")
                .setMessage("确定删除 ${e.host} ？")
                .setPositiveButton("删除") { _, _ ->
                    val cur = ServerPrefs.getCustomHosts(prefs).toMutableList()
                    cur.removeAll { it.equals(e.host, true) }
                    ServerPrefs.setCustomHosts(prefs, cur)
                    entries = buildEntries()
                    notifyDataSetChanged()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        override fun getCount() = entries.size
        override fun getItem(p: Int) = entries[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(p: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: inflater.inflate(R.layout.server_item, parent, false)
            val name = v.findViewById<TextView>(R.id.itemName)
            val host = v.findViewById<TextView>(R.id.itemHost)
            val latency = v.findViewById<TextView>(R.id.itemLatency)
            val e = entries[p]
            val sel = prefs.getString("selected_server", ServerPrefs.defaultServers[0].second)
            val base = e.name + if (e.custom) "（自定义）" else ""
            name.text = if (e.host == sel) "✓ $base" else base
            host.text = e.host
            latency.text = latencyMap[e.host] ?: "测速中…"
            return v
        }
    }
}
