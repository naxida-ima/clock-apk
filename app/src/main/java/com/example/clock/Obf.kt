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
    val PREF_NAME          get() = d("OTY1OTE=")  // clock
    val KEY_SEL_SERVER          get() = d("KT82PzkuPz4FKT8oLD8o")  // selected_server
    val KEY_CUSTOM_SVRS          get() = d("OS8pLjU3BSk/KCw/KCk=")  // custom_servers
    val KEY_SEL_HISTORY          get() = d("KT82PzkuMzU0BTIzKS41KCM=")  // selection_history
    val KEY_ONLINE_ON          get() = d("NTQ2MzQ/BTU0")  // online_on
    val KEY_REMIND_ON          get() = d("KD83MzQ+BTU0")  // remind_on
    val KEY_NEXT_ENTRY_ON          get() = d("ND8iLgU/NC4oIwU1NA==")  // next_entry_on
    val KEY_DEVICE_UUID          get() = d("Pj8sMzk/BS8vMz4=")  // device_uuid
    val EMPTY_ARR          get() = d("AQc=")  // []
    // ===== URL 路径片段 =====
    val URL_HEARTBEAT          get() = d("dTI/OyguOD87LmUzPmc=")  // /heartbeat?id=
    val URL_NEXT_ENTRY          get() = d("dTQ/Ii4/NC4oIw==")  // /nextentry
    // ===== Toast / UI 文字 =====
    val TOAST_ONLINE_ON          get() = d("v/TEvM3sv8byveDlvuDgvM/qtebAv+ba")  // 实时在线人数：开
    val TOAST_ONLINE_OFF          get() = d("v/TEvM3sv8byveDlvuDgvM/qtebAv9/p")  // 实时在线人数：关
    val TOAST_REMIND_ON          get() = d("vNXKs9zItebAv+ba")  // 提醒：开
    val TOAST_REMIND_OFF          get() = d("vNXKs9zItebAv9/p")  // 提醒：关
    val TOAST_SELECTED          get() = d("v+3os9rTvNHztebA")  // 已选择：
    val TOAST_NEXT_ON          get() = d("vuLRvPb7suXBv9//vM3ss83utebAv+ba")  // 下次进入时间：开
    val TOAST_NEXT_OFF          get() = d("vuLRvPb7suXBv9//vM3ss83utebAv9/p")  // 下次进入时间：关
    val SYNC_ING          get() = d("v8rWvPf/vuL3uNr8")  // 同步中…
    val SYNC_OK          get() = d("v+3ovPr7v93c")  // 已校准
    val SYNC_FAIL          get() = d("vMbwvPr7v93cepjterzG9rzG4LzN7LPN7g==")  // 未校准 · 本机时间
    val ONLINE_DASH          get() = d("v8byveDlend3")  // 在线 --
    val ONLINE_ERR          get() = d("v8byveDlerLU7b/VzL/+67Lu/w==")  // 在线 获取失败
    val ONLINE_FMT          get() = d("v8byveDlen8+er7g4A==")  // 在线 %d 人
    val NEXT_ENTRY_FMT          get() = d("vuLRvPb7suXBv9//en8p")  // 下次进入 %s
    val DLG_ADD_TITLE          get() = d("vO3hv9D6st3wv/TAvuPTvMbXv9D7v8Py")  // 添加自定义服务器
    val DLG_ADD_HINT          get() = d("vuTRv/zYejQuKnQ/Ijs3KjY/dDk1Nw==")  // 例如 ntp.example.com
    val DLG_ADD_MSG          get() = d("v/vxv9zDehQOCnq8xte/0Pu/w/K/xuq/x9q15tIPHgp6vfH1v9X5emtoabXm0w==")  // 填写 NTP 服务器地址（UDP 端口 123）
    val DLG_ADD_OK          get() = d("vO3hv9D6")  // 添加
    val DLG_ADD_CANCEL          get() = d("v9XMvOzS")  // 取消
    val TOAST_EXISTS          get() = d("v+3ov/fCv8by")  // 已存在
    val TOAST_NO_DEL          get() = d("s+HCsvT+vMbXv9D7v8PyvuLXv9X1v9L6s8P+")  // 默认服务器不可删除
    val DLG_DEL_TITLE          get() = d("v9L6s8P+vMbXv9D7v8Py")  // 删除服务器
    val DLG_DEL_MSG          get() = d("vfv0v/TAv9L6s8P+en8perXmxQ==")  // 确定删除 %s ？
    val DLG_DEL_OK          get() = d("v9L6s8P+")  // 删除
    val LATENCY_ING          get() = d("vO/Rs9rFvuL3uNr8")  // 测速中…
    val LATENCY_FMT          get() = d("v+HssuXFen8+ejcp")  // 延迟 %d ms
    val LATENCY_TIMEOUT          get() = d("v+HssuXFerLs37zN7A==")  // 延迟 超时
    val LABEL_SETTINGS          get() = d("vMbXv9D7v8PysvTkvef0")  // 服务器设置
    val LABEL_CUSTOM          get() = d("tebSst3wv/TAvuPTtebT")  // （自定义）
    val LABEL_SELECTED          get() = d("uMbJen8p")  // ✓ %s
}
