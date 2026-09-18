package com.hg.xposed.hooks

import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Logger
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

/**
 * 功能 Hook 基类。所有具体 Hook 点均由 [com.hg.xposed.dexkit.Finders] 动态查询得到，
 * 这里只负责把查询到的 [Method] 安装成拦截器。
 *
 * 使用 libxposed API 102 的拦截器模型：
 * - `xp.hook(method).intercept { ... }`
 * - 在 lambda 中返回自定义值即替换原返回值（不调用 chain.proceed() 表示跳过原方法体）。
 */
abstract class BaseHook(protected val xp: XposedInterface) {
    abstract val name: String
    abstract fun isEnabled(flags: FeatureFlags): Boolean
    abstract fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: FeatureFlags)

    /** 强制原方法返回 true（适用于 boolean 权限/状态校验）。 */
    protected fun hookReturnTrue(method: Method) {
        runCatching {
            xp.hook(method).intercept { java.lang.Boolean.TRUE }
        }.onSuccess {
            Logger.i("[$name] return-true  ->  ${method.declaringClass?.name}.${method.name}")
        }.onFailure {
            Logger.w("[$name] hook 失败: $method", it)
        }
    }

    /** 置空原方法（适用于 void 的广告加载 / 付费墙触发，原方法体被跳过）。 */
    protected fun hookNoOp(method: Method) {
        runCatching {
            xp.hook(method).intercept { null }
        }.onSuccess {
            Logger.i("[$name] no-op      ->  ${method.declaringClass?.name}.${method.name}")
        }.onFailure {
            Logger.w("[$name] hook 失败: $method", it)
        }
    }
}
