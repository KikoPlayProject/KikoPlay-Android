package com.kiko.kikoplay.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun KikoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.White.copy(alpha = 0.28f),
    trackHeight: Dp = 3.dp,
    thumbDiameter: Dp = 10.dp,
    draggingThumbDiameter: Dp = 12.dp,
    sliderHeight: Dp = 18.dp
) {
    val minValue = valueRange.start
    val maxValue = valueRange.endInclusive
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val clampedValue = value.coerceIn(minValue, maxValue)

    var trackWidthPx by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(clampedValue) }

    LaunchedEffect(clampedValue, isDragging) {
        if (!isDragging) {
            dragValue = clampedValue
        }
    }

    fun positionFromOffset(offsetX: Float): Float {
        if (trackWidthPx <= 0) return minValue
        val fraction = (offsetX / trackWidthPx.toFloat()).coerceIn(0f, 1f)
        return minValue + range * fraction
    }

    Canvas(
        modifier = modifier
            .height(sliderHeight)
            .onSizeChanged { trackWidthPx = it.width }
            .pointerInput(minValue, maxValue, trackWidthPx) {
                detectTapGestures { offset ->
                    isDragging = false
                    dragValue = positionFromOffset(offset.x)
                    onValueChange(dragValue)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(minValue, maxValue, trackWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragValue = positionFromOffset(offset.x)
                        onValueChange(dragValue)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = if (trackWidthPx > 0) dragAmount / trackWidthPx.toFloat() * range else 0f
                        dragValue = (dragValue + delta).coerceIn(minValue, maxValue)
                        onValueChange(dragValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    }
                )
            }
    ) {
        val centerY = size.height / 2f
        val trackStroke = trackHeight.toPx()
        val fraction = ((clampedValue - minValue) / range).coerceIn(0f, 1f)
        val progressX = size.width * fraction
        val thumbRadius = (if (isDragging) draggingThumbDiameter else thumbDiameter).toPx() / 2f

        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(progressX, centerY),
            strokeWidth = trackStroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = Offset(progressX.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius)), centerY)
        )
    }
}
