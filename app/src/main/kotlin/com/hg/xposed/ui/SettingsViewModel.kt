package com.hg.xposed.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.hg.xposed.core.FeatureFlags
import com.hg.xposed.core.Target

/**
 * 持有功能开关状态。UI 进程直接写入 [Context.MODE_PRIVATE] 的 SharedPreferences，
 * Hook 进程通过 libxposed 的 [io.github.libxposed.api.XposedInterface.getRemotePreferences]
 * 以相同 group 名 [Target.CONFIG_PREFS] 读取——两端 key 严格对应。
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val sp = app.getSharedPreferences(Target.CONFIG_PREFS, Context.MODE_PRIVATE)

    var flags by mutableStateOf(FeatureFlags.from(sp))
        private set

    fun setVip(v: Boolean) = update(FeatureFlags.KEY_VIP, v) { flags.copy(vipUnlock = v) }
    fun setAd(v: Boolean) = update(FeatureFlags.KEY_AD, v) { flags.copy(adBlock = v) }
    fun setSplash(v: Boolean) = update(FeatureFlags.KEY_SPLASH, v) { flags.copy(splashSkip = v) }
    fun setDownload(v: Boolean) = update(FeatureFlags.KEY_DOWNLOAD, v) { flags.copy(downloadUnlock = v) }

    private inline fun update(key: String, value: Boolean, crossinline rebuild: () -> FeatureFlags) {
        sp.edit().putBoolean(key, value).apply()
        flags = rebuild()
    }
}
