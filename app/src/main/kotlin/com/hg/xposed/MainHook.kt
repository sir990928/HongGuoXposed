package com.hg.xposed

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import com.hg.xposed.core.Target
import com.hg.xposed.dexkit.DexKitProvider
import com.hg.xposed.hooks.AdBlockHook
import com.hg.xposed.hooks.BaseHook
import com.hg.xposed.hooks.DownloadUnlockHook
import com.hg.xposed.hooks.SplashSkipHook
import com.hg.xposed.hooks.VipUnlockHook
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * libxposed API 102 模块入口。
 *
 * - 框架以无参构造实例化本类，再通过 attachFramework 注入 [io.github.libxposed.api.XposedInterface]。
 * - [onPackageReady] 在目标包 AppComponentFactory 就绪、Application 创建前触发，
 *   此时拿到真实 ClassLoader 与 APK 路径，正是 DexKit 查询 + 安装 Hook 的时机。
 * - 同时覆盖国内版 `com.phoenix.read` 与海外版 `com.phoenix.read.oversea.gp`，
 *   并跳过 WebView 沙箱/渲染进程，避免误伤。
 */
class MainHook : XposedModule() {

    @Volatile private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        Logger.attach(this)
        processName = param.processName ?: ""
        val fw = runCatching { frameworkName }.getOrDefault("?")
        Logger.i("模块已加载 进程=$processName 框架=$fw API=${apiVersion}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName !in Target.TARGET_PACKAGES) return

        // 跳过 WebView 沙箱/渲染进程：不承载业务 UI，安装 Hook 反易误伤
        if (Target.SANDBOX_PROCESS_MARKERS.any { processName.contains(it) }) {
            Logger.i("沙箱/渲染进程，跳过 Hook 安装: $processName")
            return
        }

        Logger.i("命中红果短剧: ${param.packageName} 进程=$processName")
        val classLoader = param.classLoader
        val apkPath = param.applicationInfo.sourceDir

        val flags = runCatching {
            FeatureFlags.from(getRemotePreferences(Target.CONFIG_PREFS))
        }.getOrElse {
            Logger.w("读取远端配置失败，回退默认(全开)", it)
            FeatureFlags()
        }
        Logger.i("功能开关: $flags")

        val bridge = DexKitProvider.create(apkPath)
        if (bridge == null) {
            Logger.e("DexKit 桥接创建失败 apk=$apkPath")
            return
        }

        val hooks: List<BaseHook> = listOf(
            VipUnlockHook(this),
            AdBlockHook(this),
            SplashSkipHook(this),
            DownloadUnlockHook(this),
        )

        bridge.use {
            hooks.forEach { h ->
                if (!h.isEnabled(flags)) {
                    Logger.i("[${h.name}] UI 中已关闭，跳过")
                    return@forEach
                }
                runCatching { h.apply(bridge, classLoader, flags) }
                    .onFailure { Logger.e("[${h.name}] 安装失败", it) }
            }
        }
        Logger.i("全部 Hook 处理完成")
    }
}
