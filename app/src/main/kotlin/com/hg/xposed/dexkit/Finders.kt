package com.hg.xposed.dexkit

import com.hg.xposed.core.Logger
import com.hg.xposed.core.Target
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Method

/**
 * DexKit 查询中枢：所有 Hook 点均通过运行时 dex 查询动态定位，不硬编码混淆类名/方法名。
 *
 * 设计来源：对 KEJIYUNB/hongguo 参考项目的逆向分析。该参考项目的「功能性 Hook」大多落在
 * **稳定未混淆的方法名**上（isVip / canShowPauseAd / handleVideoEvent …），只是承载这些方法的
 * 类有些是稳定公开类、有些是 R8 混淆类。本模块的策略：
 *
 * 1. **按方法名 + 包范围** 用 DexKit `findMethod` 动态发现，绝不硬编码混淆名；
 * 2. 命中后 `getMethodInstance(classLoader)` 解析为反射 [Method]，再交给 [com.hg.xposed.hooks] 安装；
 * 3. 对返回值有语义要求的方法（如 `getVipInfo`），由 Hook 侧依据反射 `Method.returnType` 动态构造假对象，
 *    连「VIP 模型类名」都不需要硬编码。
 *
 * 因此本模块跨红果各版本无需维护任何版本映射表。
 */
object Finders {

    // ==================== 真实目标方法名（稳定、未混淆，来自参考项目验证） ====================

    /**
     * VIP 权益校验方法：命中后强制返回 true 即可解锁会员剧集 / 去广告权益 / 短剧阅读权。
     * 这些方法名在红果各版本中保持稳定（未被 R8 混淆），分布在
     * PrivilegeManager / NsVipImpl / NsUserInfoDependImpl / NsComicAdDependImpl 等多个类中。
     * DexKit 一次性发现所有同名方法，无需指定类。
     */
    val VIP_BOOLEAN_NAMES = listOf(
        "isVip", "isAnyVip", "canReadShortStory", "hasVipShortSeriesPrivilege",
        "hasNoAdFollAllScene", "hasNoAdForShortSeries", "isVipUser",
        "isSpecificVipOrHigher", "canShowVipCenter",
    )

    /**
     * VIP 信息模型获取方法：命中后返回伪造的 VIP 模型（用于会员标识 / 到期时间展示）。
     * 不指定返回类型——Hook 侧依据每个方法自身返回类型动态构造假对象，适配
     * dragon-read 的 VipInfoModel 与 KMP 的 VipInfo 两种模型。
     */
    val VIP_INFO_NAMES = listOf("getVipInfo", "getVipInfoModel", "getAllVipInfo")

    /**
     * 广告「展示/开关」类 boolean 方法：命中后强制返回 false 抑制广告。
     * 分布在 SeriesPauseAdImpl.canShowPauseAd/enablePauseAd 与 AdIconLayer.handleVideoEvent 等。
     */
    val AD_BOOLEAN_NAMES = listOf("canShowPauseAd", "enablePauseAd", "handleVideoEvent")

    /**
     * 广告「加载/触发」类 void 方法：命中后置空（跳过原方法体）使广告不加载/不展示。
     */
    val AD_VOID_NAMES = listOf("requestAd", "onPauseAdShow")

    /**
     * 开屏「可跳过」相关 boolean 方法（混淆概率较高，best-effort 字符串线索）。
     */
    val SPLASH_BOOLEAN_KEYWORDS = listOf("canSkip", "isSkip", "skipEnable", "canJump", "skipAd")

    /**
     * 下载权限 boolean 方法（best-effort 字符串线索）。
     */
    val DOWNLOAD_BOOLEAN_KEYWORDS = listOf("canDownload", "allowDownload", "isDownloadEnable", "downloadEnable")

    // ==================== 通用查询原语 ====================

    /**
     * 按方法名精确查询（DexKit `name` 为精确匹配）。可选叠加返回类型/参数约束。
     * 在 [packages] 范围内搜索；为 null 则全 dex 搜索（用于跨 KMP 模块定位 getVipInfo）。
     */
    private fun findByName(
        bridge: DexKitBridge,
        name: String,
        packages: List<String> = Target.SEARCH_PACKAGES,
        matcherConfig: MethodMatcher.() -> Unit = {},
    ): List<MethodData> {
        return runCatching {
            bridge.findMethod {
                if (packages.isNotEmpty()) searchPackages(*packages.toTypedArray())
                matcher {
                    this.name = name
                    matcherConfig()
                }
            }
        }.getOrElse {
            Logger.w("DexKit 查询失败 name=$name: $it")
            emptyList()
        }
    }

    /** 按多个关键字「字符串包含」查询并集（用于混淆目标的 best-effort 定位）。 */
    private fun findByKeywords(
        bridge: DexKitBridge,
        keywords: List<String>,
        matcherConfig: MethodMatcher.() -> Unit = {},
    ): List<MethodData> {
        val out = LinkedHashMap<String, MethodData>()
        for (kw in keywords) {
            val list = runCatching {
                bridge.findMethod {
                    searchPackages(*Target.SEARCH_PACKAGES.toTypedArray())
                    matcher {
                        addUsingString(kw, StringMatchType.Contains, ignoreCase = true)
                        matcherConfig()
                    }
                }
            }.getOrNull() ?: continue
            for (md in list) out[md.descriptor] = md
        }
        return out.values.toList()
    }

    /** 将 [MethodData] 列表解析为宿主进程可反射的 [Method]，解析失败自动跳过并告警。 */
    private fun resolve(list: List<MethodData>, classLoader: ClassLoader): List<Method> =
        list.mapNotNull { md ->
            runCatching { md.getMethodInstance(classLoader) }
                .onFailure { Logger.w("解析方法失败: ${md.descriptor}", it) }
                .getOrNull()
        }

    /** 去重多个查询的并集（按 method 全限定签名）。 */
    private fun union(vararg lists: List<Method>): List<Method> {
        val seen = mutableSetOf<String>()
        val out = ArrayList<Method>()
        for (l in lists) for (m in l) {
            val key = m.declaringClass.name + "#" + m.name + "(" + m.parameterTypes.joinToString(",") { it.name } + ")"
            if (seen.add(key)) out.add(m)
        }
        return out
    }

    // ==================== VIP ====================

    /** VIP 权益校验方法（boolean / Boolean 返回）。 */
    fun vipBooleanMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> {
        val raw = VIP_BOOLEAN_NAMES.flatMap { findByName(bridge, it) }
        val resolved = resolve(raw, cl)
        // 仅保留返回 boolean/Boolean 的方法，避免误伤同名异类方法
        return resolved.filter { rt ->
            rt.returnType == java.lang.Boolean.TYPE || rt.returnType == java.lang.Boolean::class.java
        }
    }

    /** VIP 信息模型获取方法（getVipInfo / getVipInfoModel / getAllVipInfo，跨 dragon-read 与 KMP 模块）。 */
    fun vipInfoMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> {
        // 全 dex 搜索：KMP 账号服务可能在非 com.dragon.read 的 KMP 包下
        val raw = VIP_INFO_NAMES.flatMap { findByName(bridge, it, packages = emptyList()) }
        return resolve(raw, cl)
    }

    // ==================== 广告 ====================

    /** 广告 boolean 方法（canShowPauseAd / enablePauseAd / handleVideoEvent）→ 返回 false。 */
    fun adBooleanMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> {
        val raw = AD_BOOLEAN_NAMES.flatMap { findByName(bridge, it) }
        val resolved = resolve(raw, cl)
        return resolved.filter { rt ->
            rt.returnType == java.lang.Boolean.TYPE || rt.returnType == java.lang.Boolean::class.java
        }
    }

    /** 广告 void 方法（requestAd / onPauseAdShow）→ 置空。 */
    fun adVoidMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> {
        val raw = AD_VOID_NAMES.flatMap { findByName(bridge, it) }
        val resolved = resolve(raw, cl)
        return resolved.filter { it.returnType == java.lang.Void.TYPE }
    }

    // ==================== best-effort（混淆目标，字符串线索） ====================

    fun splashBooleanMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> {
        val raw = findByKeywords(bridge, SPLASH_BOOLEAN_KEYWORDS) { returnType = "boolean" }
        return resolve(raw, cl)
    }

    fun downloadBooleanMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> {
        val raw = findByKeywords(bridge, DOWNLOAD_BOOLEAN_KEYWORDS) { returnType = "boolean" }
        return resolve(raw, cl)
    }
}
