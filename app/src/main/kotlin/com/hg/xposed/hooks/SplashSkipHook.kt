package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.dexkit.Finders
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge

/** 跳过开屏：开屏「可跳过」相关 boolean 方法强制返回 true。 */
class SplashSkipHook(xp: XposedInterface) : BaseHook(xp) {
    override val name: String = "跳过开屏"
    override fun isEnabled(flags: FeatureFlags) = flags.splashSkip

    override fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags) {
        val skips = Finders.splashBooleanMethods(bridge, classLoader)
        Logger.i("[$name] 候选: 跳过方法=${skips.size}")
        skips.forEach { hookReturnTrue(it) }
        if (skips.isEmpty()) Logger.w("[$name] 未命中，请在 Finders.SPLASH_BOOLEAN_KEYWORDS 校准线索")
    }
}
