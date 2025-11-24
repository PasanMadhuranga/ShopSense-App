package com.example.myapplicationv2.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun UpdateHomeDialog(
    isOpen: Boolean,
    currentLat: Double?,
    currentLng: Double?,
    tempRadius: Int,
    isLoading: Boolean,
    onUseCurrentLocation: () -> Unit,
    onPickOnMap: () -> Unit,
    onTempRadiusChange: (Int) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Home Location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (currentLat != null && currentLng != null)
                        "Home: (%.5f, %.5f)".format(currentLat, currentLng)
                    else "Home not set"
                )

                // current location button
                Button(
                    onClick = onUseCurrentLocation,
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Getting location..." else "Use current location")
                }

                // map selection button
                OutlinedButton(onClick = onPickOnMap) {
                    Text("Pick on map")
                }

                Spacer(Modifier.height(8.dp))
                Text("Radius (meters): $tempRadius")
                Slider(
                    value = tempRadius.toFloat(),
                    onValueChange = { onTempRadiusChange(it.toInt()) },
                    valueRange = 100f..1000f,
                    steps = 8
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
