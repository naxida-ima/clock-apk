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
        val p = ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE)
        return p.getString(Obf.KEY_SEL_SERVER, defaultServers[0].second)
            ?: defaultServers[0].second
    }

    fun getCustomHosts(p: SharedPreferences): List<String> {
        val s = p.getString(Obf.KEY_CUSTOM_SVRS, null) ?: return emptyList()
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
        p.edit().putString(Obf.KEY_CUSTOM_SVRS, arr.toString()).apply()
    }

    // 从已记录历史构建最近若干次选择的 host 列表
    private fun recentHistory(ctx: Context): Array<String> {
        val p = ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE)
        val arr = try { JSONArray(p.getString(Obf.KEY_SEL_HISTORY, Obf.EMPTY_ARR)) } catch (e: Exception) { JSONArray() }
        return Array(arr.length()) { i -> arr.optString(i) }
    }

    // 记录一次服务器选择，返回是否恰好匹配「在线人数触发序列」。
    // 匹配逻辑（逆序 + 序列常量）下沉到 native 层，dex 中不可见。
    // 序列：阿里云备用 → 微软 → 阿里云 → (阿里云备用 | 微软)
    fun recordSelection(ctx: Context, host: String): Boolean {
        val p = ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE)
        val arr = try { JSONArray(p.getString(Obf.KEY_SEL_HISTORY, Obf.EMPTY_ARR)) } catch (e: Exception) { JSONArray() }
        arr.put(host)
        while (arr.length() > 8) arr.remove(0)
        p.edit().putString(Obf.KEY_SEL_HISTORY, arr.toString()).apply()
        return Obf.matchOnline(recentHistory(ctx))
    }

    fun isOnlineOn(ctx: Context): Boolean =
        ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE).getBoolean(Obf.KEY_ONLINE_ON, false)

    fun setOnlineOn(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(Obf.KEY_ONLINE_ON, on).apply()
    }

    // ===== 提醒功能 =====
    // 触发序列（匹配逻辑在 native 层）：苹果 → 微软 → 阿里云 → 腾讯 → (阿里云备用 | 微软)
    fun matchRemind(ctx: Context): Boolean {
        return Obf.matchRemind(recentHistory(ctx))
    }

    fun isRemindOn(ctx: Context): Boolean =
        ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE).getBoolean(Obf.KEY_REMIND_ON, false)

    fun setRemindOn(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences(Obf.PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(Obf.KEY_REMIND_ON, on).apply()
    }

}
