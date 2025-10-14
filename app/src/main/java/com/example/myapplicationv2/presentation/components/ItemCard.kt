package com.example.myapplicationv2.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.domain.model.ToBuyItem

@Composable
fun Itemcard(
    modifier: Modifier = Modifier,
    toBuyItem: ToBuyItem,
    categories: List<Category>,
    onCheckBoxClick: () -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section (aligned text fields)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = toBuyItem.checked,
                    onCheckedChange = { onCheckBoxClick() }
                )

                // Add fixed widths or weights for neat alignment
                Text(
                    text = toBuyItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.width(100.dp)
                )

                Text(
                    text = toBuyItem.quantity.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(50.dp)
                )

                Text(
                    text = categories.find { it.id == toBuyItem.categoryId }?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )
            }

            // Right section (icons)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit item",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
