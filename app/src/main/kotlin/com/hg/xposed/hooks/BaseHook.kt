package com.hg.xposed.hooks

import com.hg.xposed.core.Logger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedInterface.Hooker
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

/**
 * 功能 Hook 基类。所有具体 Hook 点均由 [com.hg.xposed.dexkit.Finders] 动态查询得到，
 * 这里只负责把查询到的 [Method] 安装成拦截器。
 *
 * 使用 libxposed API 102 的拦截器模型：
 * - `xp.hook(method).setExceptionMode(PROTECTIVE).intercept { chain -> ... }`
 * - lambda 中返回自定义值即替换原返回值（不调用 chain.proceed() 表示跳过原方法体）；
 * - 返回 `chain.proceed()` 即正常执行原方法。
 *
 * 所有 Hook 均设为 PROTECTIVE 异常模式：Hook 内部抛出的任何异常都会被框架捕获并记录，
 * 绝不会因此导致红果客户端崩溃。
 */
abstract class BaseHook(protected val xp: XposedInterface) {
    abstract val name: String
    abstract fun isEnabled(flags: com.hg.xposed.core.FeatureFlags): Boolean
    abstract fun apply(bridge: DexKitBridge, classLoader: ClassLoader, flags: com.hg.xposed.core.FeatureFlags)

    /** 安装一个 [Method] 拦截器，带 PROTECTIVE 异常模式与成功/失败日志。 */
    private fun install(method: Method, hookId: String, block: (Chain) -> Any?) {
        runCatching {
            xp.hook(method)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(Hooker { block(it) })
        }.onSuccess {
            Logger.i("[$name] hooked  ${method.declaringClass?.name}#${method.name}  ($hookId)")
        }.onFailure {
            Logger.w("[$name] hook 失败 ${method.declaringClass?.name}#${method.name} ($hookId)", it)
        }
    }

    /** 强制原方法返回 true（适用于 boolean/Boolean 权益校验）。 */
    protected fun hookReturnTrue(method: Method, hookId: String = "true") {
        install(method, hookId) { java.lang.Boolean.TRUE }
    }

    /** 强制原方法返回 false（适用于 boolean 广告展示/开关校验）。 */
    protected fun hookReturnFalse(method: Method, hookId: String = "false") {
        install(method, hookId) { java.lang.Boolean.FALSE }
    }

    /** 置空原方法（适用于 void 广告加载/触发，原方法体被跳过）。 */
    protected fun hookNoOp(method: Method, hookId: String = "noop") {
        install(method, hookId) { null }
    }

    /** 通用拦截：lambda 自行决定 proceed 还是替换。 */
    protected fun hookReplace(method: Method, hookId: String = "replace", block: (Chain) -> Any?) {
        install(method, hookId, block)
    }

    /**
     * 对一个返回对象类型的方法，构造并返回「伪造的 VIP 模型」。
     * 依据 [method] 的反射返回类型动态选构造器、按类型填充 VIP 正向值，连模型类名都不需要硬编码。
     * 构造失败返回 null（调用方应回退 `chain.proceed()`，避免返回 null 造成 NPE）。
     *
     * - 返回类型是 [List]/Collection 时，包装为单元素列表（适配 getAllVipInfo 等）；
     * - 否则直接返回单个伪造对象。
     */
    protected fun buildFakeVipModel(method: Method): Any? {
        val rt = method.returnType ?: return null
        if (rt == java.lang.Void.TYPE || rt == java.lang.Boolean.TYPE) return null
        return try {
            val single = instantiateFake(rt)
            if (java.util.Collection::class.java.isAssignableFrom(rt) || rt.isArray) {
                java.util.Collections.singletonList(single)
            } else single
        } catch (e: Throwable) {
            Logger.w("[$name] 伪造 ${rt.name} 失败，将回退原方法: ${e.message}")
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun instantiateFake(cls: Class<*>): Any? {
        // 优先选含枚举参数的构造器（VIP 模型的典型形态：(..., VipCommonSubType)）
        val ctors = cls.declaredConstructors
        val ctor = ctors.maxByOrNull { it.parameterTypes.count { p -> p.isEnum } }
            ?: ctors.maxByOrNull { it.parameterTypes.size }
            ?: return null
        ctor.isAccessible = true
        val args = ctor.parameterTypes.map { p ->
            when {
                p == java.lang.String::class.java -> "2099-12-31 23:59:59"      // 到期时间
                p == java.lang.Boolean::class.java || p == java.lang.Boolean.TYPE -> true
                p == java.lang.Integer::class.java || p == java.lang.Integer.TYPE -> 1
                p == java.lang.Long::class.java || p == java.lang.Long.TYPE -> 99999999L
                p.isEnum -> p.enumConstants?.firstOrNull()
                java.lang.Number::class.java.isAssignableFrom(p) -> 1
                p == java.util.List::class.java -> java.util.ArrayList<Any>()
                else -> null
            }
        }.toTypedArray()
        return ctor.newInstance(*args)
    }
}

/**
 * 拦截器：libxposed API 102 的 [Hooker] 是 Java SAM 接口（`Object intercept(Chain)`），
 * Kotlin lambda 可直接适配。返回值即替换原方法返回值（不调用 chain.proceed() = 跳过原方法体）。
 */

