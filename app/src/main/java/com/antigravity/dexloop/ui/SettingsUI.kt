package com.antigravity.dexloop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antigravity.dexloop.strategies.StrategyManager

@Composable
fun SettingsDialog(
    currentConfig: StrategyManager.DisplayConfig,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var widthStr by remember { mutableStateOf(currentConfig.width.toString()) }
    var heightStr by remember { mutableStateOf(currentConfig.height.toString()) }
    var densityStr by remember { mutableStateOf(currentConfig.density.toString()) }

    val context = LocalContext.current
    val metrics = context.resources.displayMetrics

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Text(
                    "Native Device: ${metrics.widthPixels}x${metrics.heightPixels} / ${metrics.densityDpi}dpi",
                    style = MaterialTheme.typography.body1
                )

                // Presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    OutlinedButton(onClick = {
                        widthStr = metrics.widthPixels.toString()
                        heightStr = metrics.heightPixels.toString()
                        densityStr = metrics.densityDpi.toString()
                    }) { Text("Auto") }

                    OutlinedButton(onClick = {
                        widthStr = "1920"
                        heightStr = "1080"
                        densityStr = "240"
                    }) { Text("FHD") }

                    OutlinedButton(onClick = {
                        widthStr = "2200"
                        heightStr = "1800"
                        densityStr = "320"
                    }) { Text("Fold") }

                    OutlinedButton(onClick = {
                        widthStr = "2560"
                        heightStr = "1440"
                        densityStr = "320"
                    }) { Text("QHD") }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Width") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("Height") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = densityStr,
                    onValueChange = { densityStr = it },
                    label = { Text("Density (DPI)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Note: Restart strategies after applying changes.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val w = widthStr.toIntOrNull() ?: currentConfig.width
                val h = heightStr.toIntOrNull() ?: currentConfig.height
                val d = densityStr.toIntOrNull() ?: currentConfig.density
                onSave(w, h, d)
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
