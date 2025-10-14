package com.example.myapplicationv2.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.presentation.components.Itemcard
import com.example.myapplicationv2.R

@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { HomeScreenTopBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TODO: Update Home */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Update Home"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Update Home")
                }

                Button(
                    onClick = { /* TODO: Add Category */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Category"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Category")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ItemCardSection(
                modifier = Modifier.fillMaxWidth(),
//                toBuyItems = emptyList(),
                toBuyItems = listOf(
                    ToBuyItem(
                        name = "Milk",
                        category = "Dairy",
                        quantity = 1,
                        checked = true
                    ),
                    ToBuyItem(
                        name = "Bread",
                        category = "Bakery",
                        quantity = 2,
                        checked = false
                    ),
                    ToBuyItem(
                        name = "Eggs",
                        category = "Dairy",
                        quantity = 6,
                        checked = false
                    )
                ),
                onCheckBoxClick = { },
                onClick = { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "ShopSense",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    )
}

@Composable
private fun ItemCardSection(
    modifier: Modifier = Modifier,
    toBuyItems: List<ToBuyItem>,
    onCheckBoxClick: (ToBuyItem) -> Unit,
    onClick: (ToBuyItem) -> Unit
) {
    Column (modifier = modifier){
        if (toBuyItems.isEmpty()) {
            Image(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(R.drawable.img_no_items),
                contentDescription = "No Items"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "You don't have any items.\nClick the + button to add items.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

        } else {
            toBuyItems.forEach { toBuyItem ->
                Itemcard(
                    toBuyItem = toBuyItem,
                    onCheckBoxClick = { onCheckBoxClick(toBuyItem) },
                    onClick = { onClick(toBuyItem) },
                    onEditClick = { /* TODO: Edit item */ },
                    onDeleteClick = { /* TODO: Delete item */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
