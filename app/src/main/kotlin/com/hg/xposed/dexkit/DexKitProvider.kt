package com.hg.xposed.dexkit

import org.luckypray.dexkit.DexKitBridge

/**
 * DexKit 桥接创建。DexKit 创建较耗时，本模块在 [com.hg.xposed.MainHook.onPackageReady]
 * 中创建一次、查询完毕后随 use{} 关闭，避免泄漏。
 */
object DexKitProvider {
    fun create(apkPath: String): DexKitBridge? = try {
        DexKitBridge.create(apkPath)
    } catch (t: Throwable) {
        // 通常发生在 apk 路径无效或 dex 解析异常
        null
    }
}
