package com.hg.xposed.core

import android.content.SharedPreferences

/**
 * 各功能开关。从远端 SharedPreferences（模块 UI 写入）读取。
 *
 * 这些字段与 [com.hg.xposed.ui.SettingsViewModel] 写入的 key 一一对应，
 * 修改 key 需同步两端。
 */
data class FeatureFlags(
    val vipUnlock: Boolean = true,
    val adBlock: Boolean = true,
    val splashSkip: Boolean = true,
    val downloadUnlock: Boolean = true,
) {
    companion object {
        // ====== SharedPreferences keys（UI 与 Hook 共享） ======
        const val KEY_VIP = "feat_vip_unlock"
        const val KEY_AD = "feat_ad_block"
        const val KEY_SPLASH = "feat_splash_skip"
        const val KEY_DOWNLOAD = "feat_download_unlock"
        const val KEY_INITIALIZED = "initialized"

        /** 从任意 [SharedPreferences]（包括 LSPosed 远端 prefs）解析配置。 */
        fun from(sp: SharedPreferences): FeatureFlags = FeatureFlags(
            vipUnlock = sp.getBoolean(KEY_VIP, true),
            adBlock = sp.getBoolean(KEY_AD, true),
            splashSkip = sp.getBoolean(KEY_SPLASH, true),
            downloadUnlock = sp.getBoolean(KEY_DOWNLOAD, true),
        )
    }
}
