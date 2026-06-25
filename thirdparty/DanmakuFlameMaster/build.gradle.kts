// 本地 fork 自 DanmakuFlameMaster v0.9.25，仅用于修复 DrawHandler.syncTimer
// 的 A/B 双分支时钟跳变（GitHub issue #426）。源码原样保留，未做删减。
plugins {
    id("com.android.library")
}

android {
    namespace = "master.flame.danmaku"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    // DFM 原始 AndroidManifest.xml 位于 src/main 下，需显式指向。
    sourceSets {
        named("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs("src/main/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// flame module 是自包含的纯 Java 库，无需任何外部依赖。
// 原始 native (ndkbitmap) 加载条件为 11 <= SDK_INT < 23，minSdk 24 下永不
// 满足，NativeBitmapFactory 始终走 Java fallback，因此也不依赖 ndkbitmap。
dependencies {
}
