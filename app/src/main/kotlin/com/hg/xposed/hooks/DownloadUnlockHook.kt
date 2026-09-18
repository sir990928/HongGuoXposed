package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.dexkit.Finders
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge

/** 解锁下载：下载权限 boolean 方法强制返回 true。 */
class DownloadUnlockHook(xp: XposedInterface) : BaseHook(xp) {
    override val name: String = "解锁下载"
    override fun isEnabled(flags: FeatureFlags) = flags.downloadUnlock

    override fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags) {
        val dls = Finders.downloadBooleanMethods(bridge, classLoader)
        Logger.i("[$name] 候选: 下载权限方法=${dls.size}")
        dls.forEach { hookReturnTrue(it) }
        if (dls.isEmpty()) Logger.w("[$name] 未命中任何方法，请在 Finders.Hints 校准关键词")
    }
}
