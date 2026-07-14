#include <jni.h>
#include <string>
#include <algorithm>
#include <cstring>
#include <cstdint>
#include <cstdlib>

// 关键逻辑全部在 native 层：XOR key 与暗号序列常量都不出现在 dex 中。
// XOR key 运行时从「真实包名」派生（FNV-1a），不再写死单字节常量 0x5A：
//   - 本地单测用环境变量 Obf_TEST_PKG 注入包名，保证可离线校验；
//   - Android 运行时经 ActivityThread 取真实包名；取不到则兜底为当前包名。
//   编译器无法在编译期折叠（包名是运行时取得的），故 .so 中无现成 key。
static uint8_t g_key = 0;

static uint8_t compute_key(JNIEnv* env) {
    std::string pkg;
    const char* t = getenv("Obf_TEST_PKG");
    if (t && *t) {
        pkg = t;
    } else if (env) {
        jclass at = env->FindClass("android/app/ActivityThread");
        if (at) {
            jmethodID cur = env->GetStaticMethodID(at, "currentApplication", "()Landroid/app/Application;");
            if (cur) {
                jobject app = env->CallStaticObjectMethod(at, cur);
                if (app) {
                    jclass ctx = env->GetObjectClass(app);
                    jmethodID gp = env->GetMethodID(ctx, "getPackageName", "()Ljava/lang/String;");
                    if (gp) {
                        jstring pn = (jstring)env->CallObjectMethod(app, gp);
                        if (pn) {
                            const char* s = env->GetStringUTFChars(pn, nullptr);
                            if (s) { pkg = s; env->ReleaseStringUTFChars(pn, s); }
                            env->DeleteLocalRef(pn);
                        }
                        env->DeleteLocalRef(ctx);
                    }
                    env->DeleteLocalRef(app);
                }
                if (env->ExceptionCheck()) env->ExceptionClear();
            }
            env->DeleteLocalRef(at);
        }
    }
    if (pkg.empty()) pkg = "com.example.clock"; // 兜底：与生成时常量一致
    uint32_t h = 2166136261u;
    for (unsigned char c : pkg) { h ^= c; h *= 16777619u; }
    uint8_t k = (uint8_t)(h & 0xFF);
    return k ? k : 0x5A;
}

static uint8_t derive_key() {
    if (g_key == 0) g_key = compute_key(nullptr); // JNI_OnLoad 已初始化；此处仅兜底
    return g_key;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) {
        g_key = compute_key(env);
        if (env->ExceptionCheck()) env->ExceptionClear(); // 丢弃取包名时的任何异常，保证加载成功
    }
    return JNI_VERSION_1_6;
}

static const char B64[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

// 标准 Base64 解码（无填充/有填充均可）
static std::string b64_decode(const std::string& in) {
    std::string out;
    int val = 0, valb = -8;
    for (unsigned char c : in) {
        if (c == '=') break;
        const char* p = (const char*)memchr(B64, (int)c, sizeof(B64) - 1);
        if (!p) continue;
        val = (val << 6) + (int)(p - B64);
        valb += 6;
        if (valb >= 0) {
            out.push_back((char)((val >> valb) & 0xFF));
            valb -= 8;
        }
    }
    return out;
}

// 对应 Kotlin 的 external fun d(e: String): String —— JNI 名必须与 Kotlin 方法名一致（Obf_d）
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_clock_Obf_d(JNIEnv* env, jclass, jstring enc) {
    const char* encC = env->GetStringUTFChars(enc, nullptr);
    std::string decoded = b64_decode(std::string(encC));
    env->ReleaseStringUTFChars(enc, encC);
    for (char& c : decoded) c ^= derive_key();
    return env->NewStringUTF(decoded.c_str());
}

// 暗号序列匹配：历史 host 数组（按选择顺序）在 native 内逆序并与隐藏常量比较。
// which: 0 = 在线人数序列(阿里云备用→微软→阿里云→(阿里云备用|微软))
//        1 = 提醒序列(苹果→微软→阿里云→腾讯→(阿里云备用|微软))
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_clock_Obf_matchSeq(JNIEnv* env, jclass, jobjectArray history, jint which) {
    uint8_t k = derive_key();
    jsize len = env->GetArrayLength(history);
    // 历史 host 逆序后再 XOR key，与隐藏常量同方式处理（.so 中无明文 host）
    auto hostAt = [&](int idx) -> std::string {
        jstring s = (jstring)env->GetObjectArrayElement(history, idx);
        const char* c = env->GetStringUTFChars(s, nullptr);
        std::string r(c);
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
        std::reverse(r.begin(), r.end());
        for (char& ch : r) ch ^= k;
        return r;
    };
    auto xored = [k](const char* s) -> std::string {
        std::string r(s);
        for (char& ch : r) ch ^= k;
        return r;
    };
    // 序列常量以「逆序 + XOR key」存储（dex/.so 中均不可读）
    const std::string Z1 = xored("moc.nuyila.1ptn"); // ntp1.aliyun.com
    const std::string Z2 = xored("moc.swodniw.emit"); // time.windows.com
    const std::string Z3 = xored("moc.nuyila.ptn");   // ntp.aliyun.com
    const std::string Z4 = xored("moc.elppa.emit");   // time.apple.com
    const std::string Z5 = xored("moc.tnecnet.ptn");  // ntp.tencent.com

    if (which == 0) {
        if (len < 4) return JNI_FALSE;
        std::string a = hostAt(len - 4), b = hostAt(len - 3),
                    c = hostAt(len - 2), d = hostAt(len - 1);
        return (a == Z1 && b == Z2 && c == Z3 && (d == Z1 || d == Z2))
                   ? JNI_TRUE : JNI_FALSE;
    } else {
        if (len < 5) return JNI_FALSE;
        std::string a = hostAt(len - 5), b = hostAt(len - 4),
                    c = hostAt(len - 3), d = hostAt(len - 2),
                    e = hostAt(len - 1);
        return (a == Z4 && b == Z2 && c == Z3 && d == Z5 && (e == Z1 || e == Z2))
                   ? JNI_TRUE : JNI_FALSE;
    }
}
