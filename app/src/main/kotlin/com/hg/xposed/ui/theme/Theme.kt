package com.hg.xposed.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

private val HongGuoColors = darkColorScheme(
    primary = Red,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = RedContainer,
    onPrimaryContainer = Red,
    secondary = RedDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = Outline,
)

@Composable
fun HongGuoTheme(content: @Composable () -> Unit) {
    // 本模块固定深色品牌主题
    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            (view.context as? Activity)?.window?.statusBarColor =
                android.graphics.Color.TRANSPARENT
        }
    }
    MaterialTheme(
        colorScheme = HongGuoColors,
        typography = Typography,
        content = content,
    )
}
