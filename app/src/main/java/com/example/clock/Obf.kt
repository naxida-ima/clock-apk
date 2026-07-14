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
    val PREF_NAME          get() = d("OTY1OTE=")
    val KEY_SEL_SERVER     get() = d("KT82PzkuPz4FKT8oLD8o")
    val KEY_CUSTOM_SVRS    get() = d("OS8pLjU3BSk/KCw/KCk=")
    val KEY_SEL_HISTORY    get() = d("KT82PzkuMzU0BTIzKS41KCM=")
    val KEY_ONLINE_ON      get() = d("NTQ2MzQ/BTU0")
    val KEY_REMIND_ON      get() = d("KD83MzQ+BTU0")
    val KEY_NEXT_ENTRY_ON  get() = d("ND8iLgU/NC4oIwU1NA==")
    val KEY_DEVICE_UUID    get() = d("Pj8sMzk/BS8vMz4=")
    val EMPTY_ARR          get() = d("AQc=")

    // ===== URL 路径片段 =====
    val URL_HEARTBEAT      get() = d("dTI/OyguOD87LmUzPmc=")
    val URL_NEXT_ENTRY     get() = d("dTQ/Ii4/NC4oIw==")

    // ===== Toast / UI 文字 =====
    val TOAST_ONLINE_ON    get() = d("xKxy5eAqQFo=")
    val TOAST_ONLINE_OFF   get() = d("xKxy5eAqQCk=")
    val TOAST_REMIND_ON    get() = d("ishAWg==")
    val TOAST_REMIND_OFF   get() = d("ishAKQ==")
    val TOAST_SELECTED     get() = d("qFOzQA==")
    val TOAST_NEXT_ON      get() = d("UXuBP6yuQFo=")
    val TOAST_NEXT_OFF     get() = d("UXuBP6yuQCk=")
    val SYNC_ING           get() = d("Vj93fA==")
    val SYNC_OK            get() = d("qHuc")
    val SYNC_FAIL          get() = d("cHuceu16dmCsrg==")
    val ONLINE_DASH        get() = d("cuV6d3c=")
    val ONLINE_ERR         get() = d("cuV67Yxrfw==")
    val ONLINE_FMT         get() = d("cuV6fz564A==")
    val NEXT_ENTRY_FMT     get() = d("UXuBP3p/KQ==")
    val DLG_ADD_TITLE      get() = d("ofqwwBNX+zI=")
    val DLG_ADD_HINT       get() = d("0dh6NC4qdD8iOzcqNj90OTU3")
    val DLG_ADD_MSG        get() = d("McN6FA4Kelf7MmoaUg8eCnq1uXpraGlT")
    val DLG_ADD_OK         get() = d("ofo=")
    val DLG_ADD_CANCEL     get() = d("jNI=")
    val TOAST_EXISTS       get() = d("qAJy")
    val TOAST_NO_DEL       get() = d("gv5X+zJXtXo+")
    val DLG_DEL_TITLE      get() = d("ej5X+zI=")
    val DLG_DEL_MSG        get() = d("NMB6Pnp/KXpF")
    val DLG_DEL_OK         get() = d("ej4=")
    val LATENCY_ING        get() = d("EUV3fA==")
    val LATENCY_FMT        get() = d("rIV6fz56Nyk=")
    val LATENCY_TIMEOUT    get() = d("rIV636w=")
    val LABEL_SETTINGS     get() = d("V/sy5DQ=")
    val LABEL_CUSTOM       get() = d("UrDAE1M=")
    val LABEL_SELECTED     get() = d("SXp/KQ==")
}
