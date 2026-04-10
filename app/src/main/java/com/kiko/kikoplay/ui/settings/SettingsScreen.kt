package com.kiko.kikoplay.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val maxCacheSize by viewModel.maxCacheSize.collectAsStateWithLifecycle()
    val autoClearCache by viewModel.autoClearCache.collectAsStateWithLifecycle()
    val syncPlayProgress by viewModel.syncPlayProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCacheSizeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Cache settings group
        SettingsGroupHeader("缓存设置")

        SettingsClickItem(
            title = "最大缓存空间",
            subtitle = formatCacheSize(maxCacheSize),
            onClick = { showCacheSizeDialog = true }
        )

        SettingsSwitchItem(
            title = "自动清除过期缓存",
            subtitle = "自动删除超过30天的缓存",
            checked = autoClearCache,
            onCheckedChange = { viewModel.setAutoClearCache(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Sync settings group
        SettingsGroupHeader("同步")

        SettingsSwitchItem(
            title = "同步播放进度",
            subtitle = "播放时自动同步进度到 PC 端",
            checked = syncPlayProgress,
            onCheckedChange = { viewModel.setSyncPlayProgress(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // About group
        SettingsGroupHeader("关于与帮助")

        SettingsClickItem(
            title = "当前版本",
            subtitle = "1.0.0",
            onClick = {}
        )

        SettingsClickItem(
            title = "官方网站",
            subtitle = "kikoplay.fun",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kikoplay.fun"))
                context.startActivity(intent)
            }
        )

        SettingsClickItem(
            title = "反馈建议",
            subtitle = "通过邮件反馈",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        )

        SettingsClickItem(
            title = "开源协议",
            subtitle = "查看第三方库许可",
            onClick = {}
        )

        Spacer(Modifier.height(32.dp))
    }

    // Cache size dialog
    if (showCacheSizeDialog) {
        CacheSizeDialog(
            currentSize = maxCacheSize,
            onDismiss = { showCacheSizeDialog = false },
            onSelect = {
                viewModel.setMaxCacheSize(it)
                showCacheSizeDialog = false
            }
        )
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CacheSizeDialog(
    currentSize: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val options = listOf(
        1L * 1024 * 1024 * 1024 to "1 GB",
        5L * 1024 * 1024 * 1024 to "5 GB",
        10L * 1024 * 1024 * 1024 to "10 GB",
        0L to "无限制"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("最大缓存空间") },
        text = {
            Column {
                options.forEach { (size, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(size) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentSize == size) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "无限制"
    val gb = bytes / (1024.0 * 1024 * 1024)
    return if (gb >= 1) "%.0f GB".format(gb) else "%.0f MB".format(bytes / (1024.0 * 1024))
}
