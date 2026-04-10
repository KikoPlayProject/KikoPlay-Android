package com.kiko.kikoplay.ui.player.danmaku

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class DanmakuSettings(
    val alpha: Float = 1f,
    val fontSize: Float = 1f,
    val speed: Float = 1f,
    val displayArea: Float = 1f,
    val showScroll: Boolean = true,
    val showTop: Boolean = true,
    val showBottom: Boolean = true,
    val playbackSpeed: Float = 1f
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DanmakuSettingsPanel(
    settings: DanmakuSettings,
    onSettingsChange: (DanmakuSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("弹幕设置", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))

            // Opacity
            SettingSlider(
                label = "透明度",
                value = settings.alpha,
                valueText = "${(settings.alpha * 100).toInt()}%",
                onValueChange = { onSettingsChange(settings.copy(alpha = it)) }
            )

            // Font size
            SettingSlider(
                label = "字体大小",
                value = settings.fontSize,
                valueRange = 0.5f..2f,
                valueText = "${(settings.fontSize * 100).toInt()}%",
                onValueChange = { onSettingsChange(settings.copy(fontSize = it)) }
            )

            // Speed
            SettingSlider(
                label = "弹幕速度",
                value = settings.speed,
                valueRange = 0.5f..2f,
                valueText = "${(settings.speed * 100).toInt()}%",
                onValueChange = { onSettingsChange(settings.copy(speed = it)) }
            )

            // Display area
            SettingSlider(
                label = "显示区域",
                value = settings.displayArea,
                valueRange = 0.25f..1f,
                valueText = "${(settings.displayArea * 100).toInt()}%",
                onValueChange = { onSettingsChange(settings.copy(displayArea = it)) }
            )

            Spacer(Modifier.height(12.dp))

            // Type filters
            Text("弹幕类型", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.showScroll,
                    onClick = { onSettingsChange(settings.copy(showScroll = !settings.showScroll)) },
                    label = { Text("滚动") }
                )
                FilterChip(
                    selected = settings.showTop,
                    onClick = { onSettingsChange(settings.copy(showTop = !settings.showTop)) },
                    label = { Text("顶部") }
                )
                FilterChip(
                    selected = settings.showBottom,
                    onClick = { onSettingsChange(settings.copy(showBottom = !settings.showBottom)) },
                    label = { Text("底部") }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Playback speed
            Text("播放速度", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                    FilterChip(
                        selected = settings.playbackSpeed == speed,
                        onClick = { onSettingsChange(settings.copy(playbackSpeed = speed)) },
                        label = { Text("${speed}x") }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            Text(valueText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
