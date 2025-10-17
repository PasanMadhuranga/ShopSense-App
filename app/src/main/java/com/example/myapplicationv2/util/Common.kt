package com.example.myapplicationv2.util

import androidx.compose.material3.SnackbarDuration

sealed class SnackBarEvent {
    data class ShowSnackBar(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackBarEvent()
}