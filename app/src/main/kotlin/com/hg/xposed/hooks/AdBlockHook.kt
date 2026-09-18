package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.dexkit.Finders
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge

/** 去广告：广告加载/展示方法置空。 */
class AdBlockHook(xp: XposedInterface) : BaseHook(xp) {
    override val name: String = "去广告"
    override fun isEnabled(flags: FeatureFlags) = flags.adBlock

    override fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags) {
        val ads = Finders.adVoidMethods(bridge, classLoader)
        Logger.i("[$name] 候选: 广告方法=${ads.size}")
        ads.forEach { hookNoOp(it) }
        if (ads.isEmpty()) Logger.w("[$name] 未命中任何方法，请在 Finders.Hints 校准关键词")
    }
}
