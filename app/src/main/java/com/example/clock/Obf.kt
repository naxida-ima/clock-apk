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
    val PREF_NAME          get() = d("ZWppZW0=")  // clock
    val KEY_SEL_SERVER          get() = d("dWNqY2VyY2JZdWN0cGN0")  // selected_server
    val KEY_CUSTOM_SVRS          get() = d("ZXN1cmlrWXVjdHBjdHU=")  // custom_servers
    val KEY_SEL_HISTORY          get() = d("dWNqY2Vyb2loWW5vdXJpdH8=")  // selection_history
    val KEY_ONLINE_ON          get() = d("aWhqb2hjWWlo")  // online_on
    val KEY_REMIND_ON          get() = d("dGNrb2hiWWlo")  // remind_on
    val KEY_NEXT_ENTRY_ON          get() = d("aGN+clljaHJ0f1lpaA==")  // next_entry_on
    val KEY_DEVICE_UUID          get() = d("YmNwb2VjWXNzb2I=")  // device_uuid
    val EMPTY_ARR          get() = d("XVs=")  // []
    // ===== URL 路径片段 =====
    val URL_HEARTBEAT          get() = d("KW5jZ3RyZGNncjlvYjs=")  // /heartbeat?id=
    val URL_NEXT_ENTRY          get() = d("KWhjfnJjaHJ0fw==")  // /nextentry
    // ===== Toast / UI 文字 =====
    val TOAST_ONLINE_ON          get() = d("46iY4JGw45qu4by54ry84JO26bqc47qG")  // 实时在线人数：开
    val TOAST_ONLINE_OFF          get() = d("46iY4JGw45qu4by54ry84JO26bqc44O1")  // 实时在线人数：关
    val TOAST_REMIND_ON          get() = d("4ImW74CU6bqc47qG")  // 提醒：开
    val TOAST_REMIND_OFF          get() = d("4ImW74CU6bqc44O1")  // 提醒：关
    val TOAST_SELECTED          get() = d("47G074aP4I2v6bqc")  // 已选择：
    val TOAST_NEXT_ON          get() = d("4r6N4Kqn7rmd44Oj4JGw75Gy6bqc47qG")  // 下次进入时间：开
    val TOAST_NEXT_OFF          get() = d("4r6N4Kqn7rmd44Oj4JGw75Gy6bqc44O1")  // 下次进入时间：关
    val SYNC_ING          get() = d("45aK4Kuj4r6r5Iag")  // 同步中…
    val SYNC_OK          get() = d("47G04Kan44GA")  // 已校准
    val SYNC_FAIL          get() = d("4Jqs4Kan44GAJsSxJuCaquCavOCRsO+Rsg==")  // 未校准 · 本机时间
    val ONLINE_DASH          get() = d("45qu4by5Jisr")  // 在线 --
    val ONLINE_ERR          get() = d("45qu4by5Ju6IseOJkOOit+6yow==")  // 在线 获取失败
    val ONLINE_FMT          get() = d("45qu4by5JiNiJuK8vA==")  // 在线 %d 人
    val NEXT_ENTRY_FMT          get() = d("4r6N4Kqn7rmd44OjJiN1")  // 下次进入 %s
    val DLG_ADD_TITLE          get() = d("4LG944ym7oGs46ic4r+P4JqL44yn45+u")  // 添加自定义服务器
    val DLG_ADD_HINT          get() = d("4riN46CEJmhydihjfmdrdmpjKGVpaw==")  // 例如 ntp.example.com
    val DLG_ADD_MSG          get() = d("46et44CfJkhSVibgmovjjKfjn67jmrbjm4bpuo5TQlYm4a2p44mlJjc0Nem6jw==")  // 填写 NTP 服务器地址（UDP 端口 123）
    val DLG_ADD_OK          get() = d("4LG944ym")  // 添加
    val DLG_ADD_CANCEL          get() = d("44mQ4LCO")  // 取消
    val TOAST_EXISTS          get() = d("47G046ue45qu")  // 已存在
    val TOAST_NO_DEL          get() = d("772e7qii4JqL44yn45+u4r6L44mp446m75+i")  // 默认服务器不可删除
    val DLG_DEL_TITLE          get() = d("446m75+i4JqL44yn45+u")  // 删除服务器
    val DLG_DEL_MSG          get() = d("4aeo46ic446m75+iJiN1Jum6mQ==")  // 确定删除 %s ？
    val DLG_DEL_OK          get() = d("446m75+i")  // 删除
    val LATENCY_ING          get() = d("4LON74aZ4r6r5Iag")  // 测速中…
    val LATENCY_FMT          get() = d("472w7rmZJiNiJmt1")  // 延迟 %d ms
    val LATENCY_TIMEOUT          get() = d("472w7rmZJu6wg+CRsA==")  // 延迟 超时
    val LABEL_SETTINGS          get() = d("4JqL44yn45+u7qi44buo")  // 服务器设置
    val LABEL_CUSTOM          get() = d("6bqO7oGs46ic4r+P6bqP")  // （自定义）
    val LABEL_SELECTED          get() = d("5JqVJiN1")  // ✓ %s
}

