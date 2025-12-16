package com.example.myapplicationv2.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLocationPickerScreen(
    initialLat: Double?,
    initialLng: Double?,
    onConfirm: (lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit
) {
    // If home already set, start from there. Otherwise Colombo.
    val defaultLatLng = LatLng(initialLat ?: 6.9271, initialLng ?: 79.8612)

    var markerState by remember {
        mutableStateOf<MarkerState?>(null)
    }

    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Home on Map") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        markerState?.position?.let { pos ->
                            onConfirm(pos.latitude, pos.longitude)
                        }
                    },
                    enabled = markerState != null
                ) {
                    Text("Save as Home")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // user taps → create or move the pin
                onMapClick = { latLng ->
                    markerState = MarkerState(position = latLng)
                }
            ) {
                markerState?.let { state ->
                    Marker(
                        state = state,
                        title = "Home",
                        draggable = true
                    )
                }
            }
        }
    }
}