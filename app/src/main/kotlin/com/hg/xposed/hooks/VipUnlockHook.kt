package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.dexkit.Finders
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge

/** VIP 剧集解锁：会员校验方法强制返回 true + 付费墙触发方法置空。 */
class VipUnlockHook(xp: XposedInterface) : BaseHook(xp) {
    override val name: String = "VIP解锁"
    override fun isEnabled(flags: FeatureFlags) = flags.vipUnlock

    override fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags) {
        val checks = Finders.vipBooleanCheckMethods(bridge, classLoader)
        val paywalls = Finders.paywallVoidMethods(bridge, classLoader)
        Logger.i("[$name] 候选: 会员校验=${checks.size} 付费墙=${paywalls.size}")
        checks.forEach { hookReturnTrue(it) }
        paywalls.forEach { hookNoOp(it) }
        if (checks.isEmpty() && paywalls.isEmpty()) {
            Logger.w("[$name] 未命中任何方法，请在 Finders.Hints 校准关键词")
        }
    }
}
