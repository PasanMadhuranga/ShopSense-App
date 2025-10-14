package com.example.myapplicationv2.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    isOpen: Boolean,
    title: String = "Add Category",
    name: String,
    onNameChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit
) {
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }

    nameError = when {
        name.isBlank() -> "Please enter a category name"
        name.length > 20 -> "Name cannot exceed 20 characters"
        else -> null
    }

    if (isOpen) {

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        isError = nameError != null && name.isNotBlank(),
                        supportingText = { Text(nameError.orEmpty()) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmButtonClick,
                    enabled = nameError == null
                ) {
                    Text(text = "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
