package com.kiko.kikoplay.ui.player.danmaku

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiko.kikoplay.data.model.PlayerPreferences
import com.kiko.kikoplay.ui.player.components.KikoSlider

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

fun PlayerPreferences.toDanmakuSettings(): DanmakuSettings {
    return DanmakuSettings(
        alpha = danmakuAlpha,
        fontSize = danmakuFontSize,
        speed = danmakuSpeed,
        displayArea = danmakuDisplayArea,
        showScroll = showScrollDanmaku,
        showTop = showTopDanmaku,
        showBottom = showBottomDanmaku,
        playbackSpeed = playbackSpeed
    )
}

fun DanmakuSettings.toPlayerPreferences(isDanmakuVisible: Boolean): PlayerPreferences {
    return PlayerPreferences(
        isDanmakuVisible = isDanmakuVisible,
        danmakuAlpha = alpha,
        danmakuFontSize = fontSize,
        danmakuSpeed = speed,
        danmakuDisplayArea = displayArea,
        showScrollDanmaku = showScroll,
        showTopDanmaku = showTop,
        showBottomDanmaku = showBottom,
        playbackSpeed = playbackSpeed
    )
}

private val PanelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DanmakuSettingsPanel(
    settings: DanmakuSettings,
    onSettingsChange: (DanmakuSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val panelBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.36f),
            Color(0xD911151A),
            Color(0xF214171C)
        )
    )

    Surface(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        color = Color.Transparent,
        shape = PanelShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(panelBrush)
                .padding(start = 42.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "弹幕设置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )

                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StyledSettingSlider(
                            label = "显示区域",
                            value = settings.displayArea,
                            valueRange = 0.25f..1f,
                            valueText = "${(settings.displayArea * 100).toInt()}%",
                            onValueChange = { onSettingsChange(settings.copy(displayArea = it)) }
                        )
                        StyledSettingSlider(
                            label = "不透明度",
                            value = settings.alpha,
                            valueText = "${(settings.alpha * 100).toInt()}%",
                            onValueChange = { onSettingsChange(settings.copy(alpha = it)) }
                        )
                        StyledSettingSlider(
                            label = "字体大小",
                            value = settings.fontSize,
                            valueRange = 0.5f..2f,
                            valueText = "${(settings.fontSize * 100).toInt()}%",
                            onValueChange = { onSettingsChange(settings.copy(fontSize = it)) }
                        )
                        StyledSettingSlider(
                            label = "弹幕速度",
                            value = settings.speed,
                            valueRange = 0.5f..2f,
                            valueText = "${(settings.speed * 100).toInt()}%",
                            onValueChange = { onSettingsChange(settings.copy(speed = it)) }
                        )
                        SectionCaption(text = "按弹幕类型屏蔽")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StyledTogglePill(
                                label = "滚动",
                                selected = settings.showScroll,
                                onClick = { onSettingsChange(settings.copy(showScroll = !settings.showScroll)) }
                            )
                            StyledTogglePill(
                                label = "顶部",
                                selected = settings.showTop,
                                onClick = { onSettingsChange(settings.copy(showTop = !settings.showTop)) }
                            )
                            StyledTogglePill(
                                label = "底部",
                                selected = settings.showBottom,
                                onClick = { onSettingsChange(settings.copy(showBottom = !settings.showBottom)) }
                            )
                        }
                    }
                }

                Text(
                    text = "播放设置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )

                SettingsCard(title = "播放速度") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            StyledTogglePill(
                                label = "${speed}x",
                                selected = settings.playbackSpeed == speed,
                                onClick = { onSettingsChange(settings.copy(playbackSpeed = speed)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 10.dp),
        color = Color(0xFF23272D).copy(alpha = 0.74f),
        contentColor = Color.White,
        shape = CardShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
            content()
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.62f)
    )
}

@Composable
private fun StyledSettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
        KikoSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = Color.White.copy(alpha = 0.16f),
            trackHeight = 5.dp,
            thumbDiameter = 14.dp,
            draggingThumbDiameter = 16.dp,
            sliderHeight = 22.dp
        )
    }
}

@Composable
private fun StyledTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }
    val textColor = if (selected) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.82f)
    }

    Surface(
        onClick = onClick,
        color = backgroundColor,
        contentColor = textColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}
