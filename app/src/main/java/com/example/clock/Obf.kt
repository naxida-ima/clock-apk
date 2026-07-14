package com.example.clock

/**
 * 字符串常量混淆：所有敏感的 SharedPreferences key、URL 路径、UI 文案
 * 均以 XOR+Base64 编码存储（base64(XOR(UTF8(明文), 0x5A))）。
 * 解码逻辑（XOR key）与暗号序列常量下沉到 native 层 (libobf.so)，
 * dex 中既无解码逻辑也无序列明文，反编译难度大幅提升。
 * 注意：所有常量均由脚本按上述算法生成，确保 decode() 可还原为正确明文。
 */
object Obf {
    init {
        System.loadLibrary("obf")
    }

    /** 解码 XOR+Base64 编码的字符串（实现见 native 层） */
    external fun d(e: String): String

    /** 暗号序列匹配：native 实现，which 0=在线人数序列 1=提醒序列 */
    private external fun matchSeq(history: Array<String>, which: Int): Boolean

    fun matchOnline(history: Array<String>): Boolean = matchSeq(history, 0)
    fun matchRemind(history: Array<String>): Boolean = matchSeq(history, 1)

    // ===== SharedPreferences 文件名 & keys =====
    // ===== SharedPreferences 文件名 & keys =====
    val PREF_NAME          get() = d("FRoZFR0=")  // clock
    val KEY_SEL_SERVER          get() = d("BRMaExUCExIpBRMEABME")  // selected_server
    val KEY_CUSTOM_SVRS          get() = d("FQMFAhkbKQUTBAATBAU=")  // custom_servers
    val KEY_SEL_HISTORY          get() = d("BRMaExUCHxkYKR4fBQIZBA8=")  // selection_history
    val KEY_ONLINE_ON          get() = d("GRgaHxgTKRkY")  // online_on
    val KEY_REMIND_ON          get() = d("BBMbHxgSKRkY")  // remind_on
    val KEY_NEXT_ENTRY_ON          get() = d("GBMOAikTGAIEDykZGA==")  // next_entry_on
    val KEY_DEVICE_UUID          get() = d("EhMAHxUTKQMDHxI=")  // device_uuid
    val EMPTY_ARR          get() = d("LSs=")  // []
    // ===== URL 路径片段 =====
    val URL_HEARTBEAT          get() = d("WR4TFwQCFBMXAkkfEks=")  // /heartbeat?id=
    val URL_NEXT_ENTRY          get() = d("WRgTDgITGAIEDw==")  // /nextentry
    // ===== Toast / UI 文字 =====
    val TOAST_ONLINE_ON          get() = d("k9jokOHAk+rekczJkszMkOPGmcrsk8r2")  // 实时在线人数：开
    val TOAST_ONLINE_OFF          get() = d("k9jokOHAk+rekczJkszMkOPGmcrsk/PF")  // 实时在线人数：关
    val TOAST_REMIND_ON          get() = d("kPnmn/Dkmcrsk8r2")  // 提醒：开
    val TOAST_REMIND_OFF          get() = d("kPnmn/Dkmcrsk/PF")  // 提醒：关
    val TOAST_SELECTED          get() = d("k8HEn/b/kP3fmcrs")  // 已选择：
    val TOAST_NEXT_ON          get() = d("ks79kNrXnsntk/PTkOHAn+HCmcrsk8r2")  // 下次进入时间：开
    val TOAST_NEXT_OFF          get() = d("ks79kNrXnsntk/PTkOHAn+HCmcrsk/PF")  // 下次进入时间：关
    val SYNC_ING          get() = d("k+b6kNvTks7blPbQ")  // 同步中…
    val SYNC_OK          get() = d("k8HEkNbXk/Hw")  // 已校准
    val SYNC_FAIL          get() = d("kOrckNbXk/HwVrTBVpDq2pDqzJDhwJ/hwg==")  // 未校准 · 本机时间
    val ONLINE_DASH          get() = d("k+rekczJVltb")  // 在线 --
    val ONLINE_ERR          get() = d("k+rekczJVp74wZP54JPSx57C0w==")  // 在线 获取失败
    val ONLINE_FMT          get() = d("k+rekczJVlMSVpLMzA==")  // 在线 %d 人
    val NEXT_ENTRY_FMT          get() = d("ks79kNrXnsntk/PTVlMF")  // 下次进入 %s
    val DLG_ADD_TITLE          get() = d("kMHNk/zWnvHck9jsks//kOr7k/zXk+/e")  // 添加自定义服务器
    val DLG_ADD_HINT          get() = d("ksj9k9D0VhgCBlgTDhcbBhoTWBUZGw==")  // 例如 ntp.example.com
    val DLG_ADD_MSG          get() = d("k9fdk/DvVjgiJlaQ6vuT/NeT796T6saT6/aZyv4jMiZWkd3Zk/nVVkdERZnK/w==")  // 填写 NTP 服务器地址（UDP 端口 123）
    val DLG_ADD_OK          get() = d("kMHNk/zW")  // 添加
    val DLG_ADD_CANCEL          get() = d("k/ngkMD+")  // 取消
    val TOAST_EXISTS          get() = d("k8HEk9vuk+re")  // 已存在
    val TOAST_NO_DEL          get() = d("n83untjSkOr7k/zXk+/eks77k/nZk/7Wn+/S")  // 默认服务器不可删除
    val DLG_DEL_TITLE          get() = d("k/7Wn+/SkOr7k/zXk+/e")  // 删除服务器
    val DLG_DEL_MSG          get() = d("kdfYk9jsk/7Wn+/SVlMFVpnK6Q==")  // 确定删除 %s ？
    val DLG_DEL_OK          get() = d("k/7Wn+/S")  // 删除
    val LATENCY_ING          get() = d("kMP9n/bpks7blPbQ")  // 测速中…
    val LATENCY_FMT          get() = d("k83AnsnpVlMSVhsF")  // 延迟 %d ms
    val LATENCY_TIMEOUT          get() = d("k83AnsnpVp7A85DhwA==")  // 延迟 超时
    val LABEL_SETTINGS          get() = d("kOr7k/zXk+/entjIkcvY")  // 服务器设置
    val LABEL_CUSTOM          get() = d("mcr+nvHck9jsks//mcr/")  // （自定义）
    val LABEL_SELECTED          get() = d("lOrlVlMF")  // ✓ %s
}


