package com.hg.xposed.core

import android.util.Log
import io.github.libxposed.api.XposedInterface

/**
 * 日志门面：在 Hook 进程内优先使用 [XposedInterface.log]（写入 LSPosed 日志）；
 * 在模块 UI 进程（未挂载框架）回退到 [android.util.Log]。
 */
object Logger {
    @Volatile
    private var base: XposedInterface? = null

    /** 由 [com.hg.xposed.MainHook] 在框架挂载后调用。 */
    fun attach(base: XposedInterface) {
        this.base = base
    }

    fun d(msg: String) = log(Log.DEBUG, msg)
    fun i(msg: String) = log(Log.INFO, msg)
    fun w(msg: String, t: Throwable? = null) = log(Log.WARN, msg, t)
    fun e(msg: String, t: Throwable? = null) = log(Log.ERROR, msg, t)

    private fun log(priority: Int, msg: String, t: Throwable? = null) {
        val tag = Target.LOG_TAG
        val xposed = base
        if (xposed != null) {
            if (t != null) xposed.log(priority, tag, msg, t) else xposed.log(priority, tag, msg)
        } else {
            // UI 进程或框架未挂载
            if (t != null) Log.println(priority, tag, "$msg\n${Log.getStackTraceString(t)}")
            else Log.println(priority, tag, msg)
        }
    }
}
