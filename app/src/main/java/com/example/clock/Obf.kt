package com.example.clock

/**
 * 字符串常量混淆：所有敏感的 SharedPreferences key、URL 路径、UI 文案
 * 均以 XOR+Base64 编码存储。解码逻辑（XOR key）与暗号序列常量下沉到
 * native 层 (libobf.so)，dex 中既无解码逻辑也无序列明文，反编译难度大幅提升。
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
    val PREF_NAME          get() = d("OTY1OTE=")             // clock
    val KEY_SEL_SERVER     get() = d("KT82PzkuPz4FKT8oLD8o") // selected_server
    val KEY_CUSTOM_SVRS    get() = d("OS8pLjU3BSk/KCw/KCk=") // custom_servers
    val KEY_SEL_HISTORY    get() = d("KT82PzkuMzU0BTIzKS41KCM=") // selection_history
    val KEY_ONLINE_ON      get() = d("NTQ2MzQ/BTU0")         // online_on
    val KEY_REMIND_ON      get() = d("KD83MzQ+BTU0")         // remind_on
    val KEY_NEXT_ENTRY_ON  get() = d("ND8iLgU/NC4oIwU1NA==") // next_entry_on
    val KEY_DEVICE_UUID    get() = d("Pj8sMzk/BS8vMz4=")     // device_uuid
    val EMPTY_ARR          get() = d("AQc=")                 // []

    // ===== URL 路径片段 =====
    val URL_HEARTBEAT      get() = d("dTI/OyguOD87LmUzPmc=") // /heartbeat?id=
    val URL_NEXT_ENTRY     get() = d("dTQ/Ii4/NC4oIw==")     // /nextentry

    // ===== Toast / UI 文字 =====
    val TOAST_ONLINE_ON    get() = d("xKxy5eAqQFo=")         // 实时在线人数：开
    val TOAST_ONLINE_OFF   get() = d("xKxy5eAqQCk=")         // 实时在线人数：关
    val TOAST_REMIND_ON    get() = d("ishAWg==")             // 提醒：开
    val TOAST_REMIND_OFF   get() = d("ishAKQ==")             // 提醒：关
    val TOAST_SELECTED     get() = d("qFOzQA==")             // 已选择：
    val TOAST_NEXT_ON      get() = d("UXuBP6yuQFo=")         // 下次进入时间：开
    val TOAST_NEXT_OFF     get() = d("UXuBP6yuQCk=")         // 下次进入时间：关
    val SYNC_ING           get() = d("Vj93fA==")             // 同步中…
    val SYNC_OK            get() = d("qHuc")                 // 已校准
    val SYNC_FAIL          get() = d("cHuceu16dmCsrg==")     // 未校准 · 本机时间
    val ONLINE_DASH        get() = d("cuV6d3c="")             // 在线 --
    val ONLINE_ERR         get() = d("cuV67Yxrfw==")         // 在线 获取失败
    val ONLINE_FMT         get() = d("cuV6fz564A==")         // 在线 %d 人
    val NEXT_ENTRY_FMT     get() = d("UXuBP3p/KQ==")         // 下次进入 %s
    val DLG_ADD_TITLE      get() = d("ofqwwBNX+zI=")         // 添加自定义服务器
    val DLG_ADD_HINT       get() = d("0dh6NC4qdD8iOzcqNj90OTU3") // 例如 ntp.example.com
    val DLG_ADD_MSG        get() = d("McN6FA4Kelf7MmoaUg8eCnq1uXpraGlT") // 填写 NTP 服务器地址（UDP 端口 123）
    val DLG_ADD_OK         get() = d("ofo=")                 // 添加
    val DLG_ADD_CANCEL     get() = d("jNI=")                 // 取消
    val TOAST_EXISTS       get() = d("qAJy")                 // 已存在
    val TOAST_NO_DEL       get() = d("gv5X+zJXtXo+")         // 默认服务器不可删除
    val DLG_DEL_TITLE      get() = d("ej5X+zI=")             // 删除服务器
    val DLG_DEL_MSG        get() = d("NMB6Pnp/KXpF")         // 确定删除 %s ？
    val DLG_DEL_OK         get() = d("ej4=")                 // 删除
    val LATENCY_ING        get() = d("EUV3fA==")             // 测速中…
    val LATENCY_FMT        get() = d("rIV6fz56Nyk=")         // 延迟 %d ms
    val LATENCY_TIMEOUT    get() = d("rIV636w=")             // 延迟 超时
    val LABEL_SETTINGS     get() = d("V/sy5DQ=")             // 服务器设置
    val LABEL_CUSTOM       get() = d("UrDAE1M=")             // （自定义）
    val LABEL_SELECTED     get() = d("SXp/KQ==")             // ✓ %s
}
