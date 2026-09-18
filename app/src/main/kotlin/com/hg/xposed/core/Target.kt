package com.hg.xposed.core

/**
 * 全局常量。目标应用为红果免费短剧（抖音旗下，appId = com.phoenix.read）。
 *
 * 注意：红果的 Java 业务代码包前缀是 `com.dragon.read`（红果旧称 Dragon Read），
 * 而非 `com.phoenix`。这是 DexKit 搜索范围的关键——历史上曾因此漏掉全部真实目标。
 * 本常量同时覆盖国内版与海外版。
 */
object Target {
    /** 红果免费短剧（国内版）包名 */
    const val PACKAGE = "com.phoenix.read"

    /** 红果短剧（海外版）包名 */
    const val PACKAGE_OVERSEA = "com.phoenix.read.oversea.gp"

    /** 目标包集合（onPackageReady 时匹配） */
    val TARGET_PACKAGES = setOf(PACKAGE, PACKAGE_OVERSEA)

    /**
     * 模块在 [io.github.libxposed.api.XposedInterface.getRemotePreferences] 中使用的配置组名。
     * UI 侧使用相同名称写入 SharedPreferences，Hook 侧通过 getRemotePreferences 读取。
     */
    const val CONFIG_PREFS = "module_config"

    const val LOG_TAG = "HongGuoXposed"

    /**
     * DexKit 搜索的包名前缀，缩小范围、提升命中速度与准确度。
     * 红果主业务代码位于 `com.dragon.read.*`，广告/账号在 `com.dragon.read.ad` / `com.dragon.read.component`，
     * 视频引擎在 `com.ss.ttvideoengine`，字节系公共库在 `com.bytedance`。
     */
    val SEARCH_PACKAGES = listOf(
        "com.dragon.read",
        "com.dragon",
        "com.ss.ttvideoengine",
        "com.bytedance",
        "com.phoenix",
        "com.ss.android",
    )

    /**
     * 沙箱/渲染进程名片段：这些进程不承载业务 UI，跳过 Hook 安装可避免误伤 WebView。
     */
    val SANDBOX_PROCESS_MARKERS = listOf(
        ":sandboxed_process", ":privileged_process", ":renderer",
    )
}
