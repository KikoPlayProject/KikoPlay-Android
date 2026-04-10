package com.kiko.kikoplay.ui.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ScreenshotClipDialog(
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onScreenshot: () -> Unit,
    onClip: (durationSeconds: Double) -> Unit
) {
    var isClipMode by remember { mutableStateOf(false) }
    var clipDuration by remember { mutableFloatStateOf(5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isClipMode) "截取片段" else "远程截图/截取片段") },
        text = {
            Column {
                Text(
                    "当前位置: ${formatTime(currentPositionMs)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                if (isClipMode) {
                    Text("截取时长: ${clipDuration.toInt()} 秒", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = clipDuration,
                        onValueChange = { clipDuration = it },
                        valueRange = 1f..15f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isClipMode) {
                TextButton(onClick = {
                    onClip(clipDuration.toDouble())
                    onDismiss()
                }) { Text("截取") }
            } else {
                Row {
                    TextButton(onClick = {
                        onScreenshot()
                        onDismiss()
                    }) { Text("截图") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { isClipMode = true }) { Text("截取片段") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
