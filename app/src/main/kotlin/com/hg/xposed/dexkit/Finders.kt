package com.hg.xposed.dexkit

import com.hg.xposed.core.Logger
import com.hg.xposed.core.Target
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Method

/**
 * DexKit 查询中枢：所有 Hook 点均通过运行时 dex 查询动态定位，不硬编码类名/方法名。
 *
 * 由于红果短剧为字节跳动应用、类名随版本高度混淆，这里采用「关键字 + 返回类型 + 包范围」
 * 的多策略并集查询。命中结果会按方法 descriptor 去重。
 *
 * ## 如何精调
 * 真实逆向得到新的字符串线索后，只需修改下方 [Hints] 中的关键字列表即可，
 * 无需改动任何 Hook 逻辑。
 */
object Finders {

    /**
     * 可调线索。这里的字符串都是短剧类 App 常见的高信号词；
     * 在真实 APK 上用 jadx / frida-trace 校准后替换为更精确的值即可。
     */
    object Hints {
        /** 返回 boolean 的会员校验方法（命中后强制返回 true） */
        var vipBooleanKeywords: List<String> = listOf(
            "is_vip", "isVip", "vipStatus", "is_subscribe", "isVipUser",
            "checkVip", "isMember", "vip"
        )

        /** 付费墙触发方法（命中后置空，弹窗不弹出） */
        var paywallVoidKeywords: List<String> = listOf(
            "开通会员", "立即开通", "解锁全集", "续费", "解锁", "购买会员", "升级会员"
        )

        /** 广告加载/展示方法（命中后置空，广告不加载） */
        var adVoidKeywords: List<String> = listOf(
            "loadAd", "load_ad", "ad_load", "showAd", "show_ad",
            "splashAd", "feedAd", "interstitial", "csj", "pangle", "广告"
        )

        /** 开屏跳过相关 boolean 方法（命中后强制返回 true） */
        var splashBooleanKeywords: List<String> = listOf(
            "canSkip", "can_skip", "isSkip", "skipEnable", "canJump", "canJumpAd"
        )

        /** 下载权限 boolean 方法（命中后强制返回 true） */
        var downloadBooleanKeywords: List<String> = listOf(
            "canDownload", "can_download", "allowDownload", "allow_download",
            "isDownloadEnable", "downloadEnable", "downloadAllow"
        )
    }

    /**
     * 在目标包范围内，对每个关键字执行一次「包含字符串」查询，并集去重。
     * [matcherConfig] 可叠加返回类型/参数等约束。
     */
    private fun findByKeywords(
        bridge: DexKitBridge,
        keywords: List<String>,
        matcherConfig: MethodMatcher.() -> Unit = {},
    ): List<MethodData> {
        val out = LinkedHashMap<String, MethodData>()
        for (kw in keywords) {
            val list = runCatching {
                bridge.findMethod {
                    searchPackages(Target.SEARCH_PACKAGES)
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

    /** 将 [MethodData] 解析为宿主进程可反射的 [Method]，解析失败自动跳过并告警。 */
    private fun resolve(list: List<MethodData>, classLoader: ClassLoader): List<Method> =
        list.mapNotNull { md ->
            runCatching { md.getMethodInstance(classLoader) }
                .onFailure { Logger.w("解析方法失败: ${md.descriptor}", it) }
                .getOrNull()
        }

    fun vipBooleanCheckMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> =
        resolve(findByKeywords(bridge, Hints.vipBooleanKeywords) { returnType = "boolean" }, cl)

    fun paywallVoidMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> =
        resolve(findByKeywords(bridge, Hints.paywallVoidKeywords) { returnType = "void" }, cl)

    fun adVoidMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> =
        resolve(findByKeywords(bridge, Hints.adVoidKeywords) { returnType = "void" }, cl)

    fun splashBooleanMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> =
        resolve(findByKeywords(bridge, Hints.splashBooleanKeywords) { returnType = "boolean" }, cl)

    fun downloadBooleanMethods(bridge: DexKitBridge, cl: ClassLoader): List<Method> =
        resolve(findByKeywords(bridge, Hints.downloadBooleanKeywords) { returnType = "boolean" }, cl)
}
