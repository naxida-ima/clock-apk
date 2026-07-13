plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.clock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.clock"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/clock-apk.keystore")
            storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as String?)
            keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?)
            keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as String?)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 使用随仓库固定的 release 签名，避免 CI/本地签名不一致导致覆盖安装失败
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}
