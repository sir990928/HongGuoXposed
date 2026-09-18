package com.hg.xposed.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hg.xposed.BuildConfig
import com.hg.xposed.core.Target
import com.hg.xposed.ui.components.SectionTitle

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val installed = remember {
        runCatching { pm.getPackageInfo(Target.PACKAGE, 0) }.isSuccess
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        // ---- Hero ----
        Box(
            Modifier
                .fillMaxWidth()
                .height(172.dp)
                .background(
                    Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                    RoundedCornerShape(24.dp),
                )
                .padding(22.dp),
        ) {
            Column {
                Text("红果增强", style = MaterialTheme.typography.headlineMedium, color = androidx.compose.ui.graphics.Color.White)
                Spacer(Modifier.height(2.dp))
                Text("libxposed API 102 · DexKit 2.0", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .9f))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("运行时动态查询 · 零硬编码", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("VIP · 去广告 · 跳过开屏 · 解锁下载", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }

        // ---- 快速开始 ----
        SectionTitle("快速开始")
        Button(
            onClick = {
                val launch = pm.getLaunchIntentForPackage(Target.PACKAGE)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(launch)
                }
            },
            enabled = installed,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (installed) "打开红果短剧" else "未检测到红果短剧")
        }
        if (!installed) {
            Spacer(Modifier.height(8.dp))
            Text(
                "未检测到红果免费短剧（${Target.PACKAGE}），请先从应用商店安装后再使用本模块。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        // ---- 使用须知 ----
        SectionTitle("使用须知")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("生效步骤", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    buildString {
                        append("1. 安装本模块并在 LSPosed 中启用；\n")
                        append("2. 作用域勾选：红果免费短剧（").append(Target.PACKAGE).append("）；\n")
                        append("3. 强制停止红果后重新打开；\n")
                        append("4. 修改功能开关后需重启红果生效。")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
