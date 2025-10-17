package com.example.myapplicationv2.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.presentation.components.Itemcard
import com.example.myapplicationv2.R
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.presentation.components.AddCategoryDialog
import com.example.myapplicationv2.presentation.components.AddItemDialog
import com.example.myapplicationv2.presentation.components.DeleteDialog
import com.example.myapplicationv2.util.SnackBarEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen() {

    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val onEvent = viewModel::onEvent
    val snackBarEvent = viewModel.snackbarEventFlow

    var isAddItemDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isCategoryDialogOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        snackBarEvent.collectLatest { event ->
            when(event) {
                is SnackBarEvent.ShowSnackBar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = event.duration
                    )
                }
            }
        }
    }

    AddItemDialog(
        isOpen = isAddItemDialogOpen,
        name = state.itemName,
        quantity = state.itemQuantity,
        categories = state.categories,
        selectedCategoryId = state.itemCategoryId,
        onNameChange = { onEvent(HomeEvent.onNameChange(it)) },
        onQuantityChange = { onEvent(HomeEvent.onQuantityChange(it)) },
        onCategoryChange = { onEvent(HomeEvent.onCategoryChange(it)) },
        onDismissRequest = { isAddItemDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(HomeEvent.SaveItem)
            isAddItemDialogOpen = false
        }
    )

    AddCategoryDialog(
        isOpen = isCategoryDialogOpen,
        name = state.newCategoryName,
        onNameChange = { onEvent(HomeEvent.onNewCategoryNameChange(it)) },
        onDismissRequest = { isCategoryDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(HomeEvent.SaveCategory)
            isCategoryDialogOpen = false
        }
    )

    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        onDismissRequest = { isDeleteDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(HomeEvent.DeleteItem)
            isDeleteDialogOpen = false
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { HomeScreenTopBar() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddItemDialogOpen = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add") },
                text = { Text(text = "Add Item") },
            )
        }
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
                    onClick = { isCategoryDialogOpen = true },
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
                toBuyItems = state.toBuyItems,
                categories = state.categories,
                onCheckBoxClick = { onEvent(HomeEvent.onCheckBoxClick(it)) },
                onClick = { onEvent(HomeEvent.onCheckBoxClick(it)) },
                onEditClick = { /* TODO: edit item */ },
                onDeleteClick = { isDeleteDialogOpen = true }
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
    categories: List<Category>,
    onCheckBoxClick: (ToBuyItem) -> Unit,
    onClick: (ToBuyItem) -> Unit,
    onEditClick: (ToBuyItem) -> Unit = {},
    onDeleteClick: (ToBuyItem) -> Unit = {}
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
                    categories = categories,
                    onCheckBoxClick = { onCheckBoxClick(toBuyItem) },
                    onClick = { onClick(toBuyItem) },
                    onEditClick = { onEditClick(toBuyItem) },
                    onDeleteClick = { onDeleteClick(toBuyItem) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
