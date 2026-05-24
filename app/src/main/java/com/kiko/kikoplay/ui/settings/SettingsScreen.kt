package com.kiko.kikoplay.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiko.kikoplay.data.model.SubtitleStylePreset
import com.kiko.kikoplay.data.model.SubtitleTextSizePreset

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val syncPlayProgress by viewModel.syncPlayProgress.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fetchPcRecent by viewModel.fetchPcRecent.collectAsStateWithLifecycle()
    val smallWindowPlayback by viewModel.smallWindowPlayback.collectAsStateWithLifecycle()
    val backgroundPlayback by viewModel.backgroundPlayback.collectAsStateWithLifecycle()
    val playerPreferences by viewModel.playerPreferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showSubtitleStyleDialog by remember { mutableStateOf(false) }
    var showSubtitleTextSizeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsGroupHeader("外观")

        SettingsClickItem(
            title = "深色模式",
            subtitle = when (themeMode) {
                "light" -> "浅色"
                "dark" -> "深色"
                else -> "跟随系统"
            },
            onClick = { showThemeModeDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingsGroupHeader("播放")

        SettingsSwitchItem(
            title = "小窗播放",
            subtitle = "播放时返回桌面后以小窗口继续播放，显示视频和字幕",
            checked = smallWindowPlayback,
            onCheckedChange = { viewModel.setSmallWindowPlayback(it) }
        )

        SettingsSwitchItem(
            title = "后台继续播放",
            subtitle = "关闭时返回桌面会暂停，回到播放页后自动恢复",
            checked = backgroundPlayback,
            onCheckedChange = { viewModel.setBackgroundPlayback(it) }
        )

        SettingsClickItem(
            title = "字幕样式",
            subtitle = playerPreferences.subtitleStylePreset.label(),
            onClick = { showSubtitleStyleDialog = true }
        )

        SettingsClickItem(
            title = "字幕大小",
            subtitle = playerPreferences.subtitleTextSizePreset.label(),
            onClick = { showSubtitleTextSizeDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingsGroupHeader("同步")

        SettingsSwitchItem(
            title = "同步播放进度",
            subtitle = "播放时自动同步进度到 PC 端",
            checked = syncPlayProgress,
            onCheckedChange = { viewModel.setSyncPlayProgress(it) }
        )

        SettingsSwitchItem(
            title = "获取 PC 最近播放",
            subtitle = "连接后在最近观看中显示 PC 端最近播放",
            checked = fetchPcRecent,
            onCheckedChange = { viewModel.setFetchPcRecent(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
            subtitle = "点击复制 QQ 群号：874761809",
            onClick = {
                val groupNumber = "874761809"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("QQ 群号", groupNumber))
                Toast.makeText(context, "QQ 群号已复制：$groupNumber", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(Modifier.height(32.dp))
    }

    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = themeMode,
            onDismiss = { showThemeModeDialog = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeModeDialog = false
            }
        )
    }

    if (showSubtitleStyleDialog) {
        SelectionDialog(
            title = "字幕样式",
            currentValue = playerPreferences.subtitleStylePreset.storageValue,
            options = SubtitleStylePreset.entries.map { it.storageValue to it.label() },
            onDismiss = { showSubtitleStyleDialog = false },
            onSelect = { selected ->
                viewModel.updatePlayerPreferences { preferences ->
                    preferences.copy(subtitleStylePreset = SubtitleStylePreset.fromStorage(selected))
                }
                showSubtitleStyleDialog = false
            }
        )
    }

    if (showSubtitleTextSizeDialog) {
        SelectionDialog(
            title = "字幕大小",
            currentValue = playerPreferences.subtitleTextSizePreset.storageValue,
            options = SubtitleTextSizePreset.entries.map { it.storageValue to it.label() },
            onDismiss = { showSubtitleTextSizeDialog = false },
            onSelect = { selected ->
                viewModel.updatePlayerPreferences { preferences ->
                    preferences.copy(subtitleTextSizePreset = SubtitleTextSizePreset.fromStorage(selected))
                }
                showSubtitleTextSizeDialog = false
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
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    currentValue: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentValue == value) MaterialTheme.colorScheme.primary
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

@Composable
private fun ThemeModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "system" to "跟随系统",
        "light" to "浅色",
        "dark" to "深色"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("深色模式") },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentMode == mode) MaterialTheme.colorScheme.primary
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

private fun SubtitleStylePreset.label(): String {
    return when (this) {
        SubtitleStylePreset.SYSTEM -> "跟随系统"
        SubtitleStylePreset.BLACK_BACKGROUND -> "黑底白字"
        SubtitleStylePreset.TRANSLUCENT_BLACK_BACKGROUND -> "半透明黑底白字"
        SubtitleStylePreset.OUTLINE -> "白字描边"
        SubtitleStylePreset.SHADOW -> "白字阴影"
        SubtitleStylePreset.YELLOW_OUTLINE -> "黄字描边"
        SubtitleStylePreset.YELLOW_SHADOW -> "黄字阴影"
    }
}

private fun SubtitleTextSizePreset.label(): String {
    return when (this) {
        SubtitleTextSizePreset.SYSTEM -> "跟随系统"
        SubtitleTextSizePreset.SMALL -> "小"
        SubtitleTextSizePreset.MEDIUM -> "中"
        SubtitleTextSizePreset.LARGE -> "大"
        SubtitleTextSizePreset.EXTRA_LARGE -> "超大"
    }
}
