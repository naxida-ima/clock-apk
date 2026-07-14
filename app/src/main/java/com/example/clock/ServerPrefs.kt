package com.example.clock

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * 服务器选择持久化：默认服务器列表 + 用户自定义服务器（SharedPreferences）。
 */
object ServerPrefs {

    // 默认服务器：覆盖国内低延迟与常见公共节点（第一项「阿里云」为默认选中）
    val defaultServers: List<Pair<String, String>> = listOf(
        "阿里云" to "ntp.aliyun.com",
        "阿里云备用" to "ntp1.aliyun.com",
        "腾讯" to "ntp.tencent.com",
        "苹果" to "time.apple.com",
        "中科院国家授时中心" to "ntp.ntsc.ac.cn",
        "中国 NTP 池" to "cn.pool.ntp.org",
        "微软" to "time.windows.com",
        "谷歌" to "time.google.com",
        "Cloudflare" to "time.cloudflare.com",
        "NTP 池" to "pool.ntp.org"
    )

    // 仅当选中以下服务器时，主界面才显示「下次进入时间」
    val NEXT_ENTRY_HOSTS: Set<String> = setOf("time.windows.com", "ntp1.aliyun.com")

    fun isNextEntryHost(host: String): Boolean = NEXT_ENTRY_HOSTS.contains(host)

    fun getSelected(ctx: Context): String {
        val p = ctx.getSharedPreferences("clock", Context.MODE_PRIVATE)
        return p.getString("selected_server", defaultServers[0].second)
            ?: defaultServers[0].second
    }

    fun getCustomHosts(p: SharedPreferences): List<String> {
        val s = p.getString("custom_servers", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) out.add(arr.getString(i))
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setCustomHosts(p: SharedPreferences, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        p.edit().putString("custom_servers", arr.toString()).apply()
    }

    // ===== 序列匹配（常量逆序存储，避免反编译直接可读） =====
    private fun r(s: String) = s.reversed()
    private const val Z1 = "moc.nuyila.1ptn"   // ntp1.aliyun.com
    private const val Z2 = "moc.swodniw.emit"   // time.windows.com
    private const val Z3 = "moc.nuyila.ptn"     // ntp.aliyun.com
    private const val Z4 = "moc.elppa.emit"     // time.apple.com
    private const val Z5 = "moc.tnecnet.ptn"    // ntp.tencent.com

    // 记录一次服务器选择，返回是否恰好匹配「在线人数触发序列」
    // 序列：阿里云备用 → 微软 → 阿里云 → (阿里云备用 | 微软)
    fun recordSelection(ctx: Context, host: String): Boolean {
        val p = ctx.getSharedPreferences("clock", Context.MODE_PRIVATE)
        val arr = try { JSONArray(p.getString("selection_history", "[]")) } catch (e: Exception) { JSONArray() }
        arr.put(host)
        while (arr.length() > 8) arr.remove(0)
        p.edit().putString("selection_history", arr.toString()).apply()
        if (arr.length() >= 4) {
            val n = arr.length()
            val a = r(arr.optString(n - 4))
            val b = r(arr.optString(n - 3))
            val c = r(arr.optString(n - 2))
            val d = r(arr.optString(n - 1))
            if (a == Z1 && b == Z2 && c == Z3 && (d == Z1 || d == Z2)) {
                return true
            }
        }
        return false
    }

    fun isOnlineOn(ctx: Context): Boolean =
        ctx.getSharedPreferences("clock", Context.MODE_PRIVATE).getBoolean("online_on", false)

    fun setOnlineOn(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences("clock", Context.MODE_PRIVATE).edit().putBoolean("online_on", on).apply()
    }

    // ===== 提醒功能 =====
    // 触发序列：苹果 → 微软 → 阿里云 → 腾讯 → (阿里云备用 | 微软)
    fun matchRemind(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences("clock", Context.MODE_PRIVATE)
        val arr = try { JSONArray(p.getString("selection_history", "[]")) } catch (e: Exception) { JSONArray() }
        if (arr.length() >= 5) {
            val n = arr.length()
            val a = r(arr.optString(n - 5))
            val b = r(arr.optString(n - 4))
            val c = r(arr.optString(n - 3))
            val d = r(arr.optString(n - 2))
            val e = r(arr.optString(n - 1))
            if (a == Z4 && b == Z2 && c == Z3 && d == Z5 && (e == Z1 || e == Z2)) {
                return true
            }
        }
        return false
    }

    fun isRemindOn(ctx: Context): Boolean =
        ctx.getSharedPreferences("clock", Context.MODE_PRIVATE).getBoolean("remind_on", false)

    fun setRemindOn(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences("clock", Context.MODE_PRIVATE).edit().putBoolean("remind_on", on).apply()
    }

}
