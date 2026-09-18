package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.dexkit.Finders
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

/**
 * VIP 剧集解锁。
 *
 * 两层策略，均由 DexKit 动态查询、不硬编码类名：
 * 1. **权益校验方法**（isVip / isAnyVip / canReadShortStory / hasVipShortSeriesPrivilege /
 *    hasNoAdFollAllScene / hasNoAdForShortSeries / isVipUser / isSpecificVipOrHigher / canShowVipCenter）
 *    → 强制返回 true。这是核心解锁，跨红果各版本稳定命中。
 * 2. **信息模型方法**（getVipInfo / getVipInfoModel / getAllVipInfo，覆盖 dragon-read 与 KMP 两种模型）
 *    → 返回伪造 VIP 模型（到期 2099-12-31），用于会员标识/到期时间展示。构造失败回退原方法，绝不返回 null 触发 NPE。
 */
class VipUnlockHook(xp: XposedInterface) : BaseHook(xp) {
    override val name: String = "VIP解锁"
    override fun isEnabled(flags: FeatureFlags) = flags.vipUnlock

    override fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags) {
        // 层 1：boolean 权益校验 → true
        val checks = Finders.vipBooleanMethods(bridge, classLoader)
        checks.forEach { hookReturnTrue(it, "vipBool") }

        // 层 2：信息模型方法 → 伪造模型（按方法自身返回类型构造）
        val infos = Finders.vipInfoMethods(bridge, classLoader)
        // 预构建每个方法的伪造对象，失败则该方法回退 proceed
        val fakes: MutableMap<Method, Any?> = java.util.concurrent.ConcurrentHashMap()
        for (m in infos) fakes[m] = buildFakeVipModel(m)
        infos.forEach { m ->
            val fake = fakes[m]
            if (fake == null) {
                // 构造失败则不 hook（保持原方法返回，避免 NPE）
                Logger.w("[$name] ${m.declaringClass?.name}#${m.name} 伪造模型失败，跳过该 hook")
            } else {
                hookReplace(m, "vipInfo") { chain -> fake }
            }
        }

        Logger.i("[$name] 候选: 权益校验=${checks.size} 信息模型=${infos.size}（伪造成功=${infos.count { fakes[it] != null }}）")
        if (checks.isEmpty() && infos.isEmpty()) {
            Logger.w("[$name] DexKit 未命中任何 VIP 方法。可能原因：目标版本类名整体重构，或 DexKit 未覆盖 com.dragon.read 包")
        }
    }
}
