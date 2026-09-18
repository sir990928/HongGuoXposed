package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.dexkit.Finders
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge

/**
 * 去广告。
 *
 * 由 DexKit 按方法名动态查询、不硬编码类名：
 * - **boolean 广告展示/开关方法**（canShowPauseAd / enablePauseAd / handleVideoEvent）
 *   → 强制返回 false，抑制广告展示。分布在 SeriesPauseAdImpl 与 AdIconLayer 等。
 * - **void 广告加载/触发方法**（requestAd / onPauseAdShow）→ 置空，广告不加载。
 *
 * 稳定方法名跨红果各版本不变，是 R8 未混淆的公开业务 API。
 */
class AdBlockHook(xp: XposedInterface) : BaseHook(xp) {
    override val name: String = "去广告"
    override fun isEnabled(flags: FeatureFlags) = flags.adBlock

    override fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags) {
        val booleans = Finders.adBooleanMethods(bridge, classLoader)
        booleans.forEach { hookReturnFalse(it, "adFalse") }

        val voids = Finders.adVoidMethods(bridge, classLoader)
        voids.forEach { hookNoOp(it, "adNoop") }

        Logger.i("[$name] 候选: boolean=${booleans.size} void=${voids.size}")
        if (booleans.isEmpty() && voids.isEmpty()) {
            Logger.w("[$name] DexKit 未命中广告方法。目标版本广告链可能改名，需在 Finders.AD_*_NAMES 增补")
        }
    }
}
