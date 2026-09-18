package com.hg.xposed.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hg.xposed.R
import com.hg.xposed.ui.screens.AboutScreen
import com.hg.xposed.ui.screens.FeaturesScreen
import com.hg.xposed.ui.screens.HomeScreen
import com.hg.xposed.ui.theme.HongGuoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { HongGuoTheme { RootScreen() } }
    }
}

@Composable
private fun RootScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val vm: SettingsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavItem(0, tab, Icons.Rounded.Home, stringResource(R.string.nav_home)) { tab = 0 }
                NavItem(1, tab, Icons.Rounded.Settings, stringResource(R.string.nav_features)) { tab = 1 }
                NavItem(2, tab, Icons.Rounded.Info, stringResource(R.string.nav_about)) { tab = 2 }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen()
                1 -> FeaturesScreen(vm)
                else -> AboutScreen()
            }
        }
    }
}

// NavigationBarItem 是 RowScope 扩展，故此处也声明为 RowScope 扩展，
// 必须在 NavigationBar { } 内容 lambda 中调用。
@Composable
private fun RowScope.NavItem(
    index: Int,
    current: Int,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = index == current,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}
