package com.hg.xposed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hg.xposed.R
import com.hg.xposed.ui.SettingsViewModel
import com.hg.xposed.ui.components.FeatureCard
import com.hg.xposed.ui.components.SectionTitle

@Composable
fun FeaturesScreen(vm: SettingsViewModel) {
    val flags = vm.flags
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        SectionTitle("功能开关")
        FeatureCard(
            icon = Icons.Rounded.WorkspacePremium,
            title = stringResource(R.string.feat_vip),
            desc = stringResource(R.string.feat_vip_desc),
            checked = flags.vipUnlock,
            onChange = { vm.setVip(it) },
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            icon = Icons.Rounded.Block,
            title = stringResource(R.string.feat_ad),
            desc = stringResource(R.string.feat_ad_desc),
            checked = flags.adBlock,
            onChange = { vm.setAd(it) },
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            icon = Icons.Rounded.SkipNext,
            title = stringResource(R.string.feat_splash),
            desc = stringResource(R.string.feat_splash_desc),
            checked = flags.splashSkip,
            onChange = { vm.setSplash(it) },
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            icon = Icons.Rounded.Download,
            title = stringResource(R.string.feat_download),
            desc = stringResource(R.string.feat_download_desc),
            checked = flags.downloadUnlock,
            onChange = { vm.setDownload(it) },
        )

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Text(
                "Hook 点由 DexKit 在红果运行时动态定位，多数版本无需更新模块即可适配。若某项未生效，请用 jadx / frida 校准 Finders.Hints 中的关键词。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}
