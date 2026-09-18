package com.hg.xposed.core

/**
 * 全局常量。目标应用为红果免费短剧（抖音旗下）。
 */
object Target {
    /** 红果免费短剧包名 */
    const val PACKAGE = "com.phoenix.read"

    /** 模块在 [io.github.libxposed.api.XposedInterface.getRemotePreferences] 中使用的配置组名。
     *  UI 侧使用相同名称写入 SharedPreferences，Hook 侧通过 getRemotePreferences 读取。 */
    const val CONFIG_PREFS = "module_config"

    const val LOG_TAG = "HongGuoXposed"

    /** 常用 DexKit 搜索的包名前缀，缩小范围、提升命中速度与准确度。
     *  红果为字节跳动应用，主业务代码多位于 com.phoenix.* 与字节系包下。 */
    val SEARCH_PACKAGES = listOf(
        "com.phoenix",
        "com.bytedance",
        "com.ss.android",
    )
}
