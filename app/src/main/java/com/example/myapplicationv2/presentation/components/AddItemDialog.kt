package com.example.myapplicationv2.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    isOpen: Boolean,
    title: String = "Add Item",
    name: String,
    quantity: String,
    category: String,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit
) {
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var quantityError by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryError by rememberSaveable { mutableStateOf<String?>(null) }

    nameError = when {
        name.isBlank() -> "Please enter an item name"
        name.length > 20 -> "Name cannot exceed 20 characters"
        else -> null
    }

    quantityError = when {
        quantity.isBlank() -> "Please enter the quantity"
        quantity.toIntOrNull() == null -> "Quantity must be a number"
        else -> null
    }

    categoryError = when {
        category.isBlank() -> "Please enter a category"
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
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = onQuantityChange,
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = quantityError != null && quantity.isNotBlank(),
                        supportingText = { Text(quantityError.orEmpty()) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = onCategoryChange,
                        label = { Text("Category") },
                        singleLine = true,
                        isError = categoryError != null && category.isNotBlank(),
                        supportingText = { Text(categoryError.orEmpty()) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmButtonClick,
                    enabled = nameError == null && quantityError == null && categoryError == null
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
