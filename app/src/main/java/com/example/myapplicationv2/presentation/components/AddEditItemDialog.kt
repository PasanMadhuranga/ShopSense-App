package com.example.myapplicationv2.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplicationv2.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    isOpen: Boolean,
    title: String = "Add Item",
    name: String,
    quantity: String,
    categories: List<Category>,
    selectedCategoryId: Int?,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit
) {
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var quantityError by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryError by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val selectedCategoryName = remember(selectedCategoryId, categories) {
        categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()
    }

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
        categories.isEmpty() -> "No categories available. Add one first."
        selectedCategoryId == null -> "Please select a category"
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

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            isError = categoryError != null && selectedCategoryName.isNotBlank(),
                            supportingText = { Text(categoryError.orEmpty()) },
                            modifier = Modifier
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                val id = category.id
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        if (id != null) {
                                            onCategoryChange(id)
                                            expanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val enabled = nameError == null && quantityError == null && categoryError == null
                TextButton(onClick = onConfirmButtonClick, enabled = enabled) {
                    Text(text = "Confirm")
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
