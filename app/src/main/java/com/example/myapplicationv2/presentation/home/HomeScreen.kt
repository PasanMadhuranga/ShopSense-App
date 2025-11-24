package com.example.myapplicationv2.presentation.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.myapplicationv2.presentation.components.AddEditItemDialog
import com.example.myapplicationv2.presentation.components.DeleteDialog
import com.example.myapplicationv2.presentation.components.UpdateHomeDialog
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Launcher to ask for location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            // Now we actually try to get the location
            onEvent(HomeEvent.UseCurrentLocation)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission is required to use current location.")
            }
        }
    }

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

    AddEditItemDialog(
        isOpen = isAddItemDialogOpen,
        title = if (state.editingItemId == null) "Add Item" else "Edit Item",
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

    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        onDismissRequest = { isDeleteDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(HomeEvent.DeleteItem)
            isDeleteDialogOpen = false
        }
    )

    UpdateHomeDialog(
        isOpen = state.isUpdatingHome,
        currentLat = state.homeLat,
        currentLng = state.homeLng,
        tempRadius = state.tempRadiusMeters,
        isLoading = state.isLocationLoading,
        onUseCurrentLocation = {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onTempRadiusChange = { onEvent(HomeEvent.OnTempRadiusChange(it)) },
        onSave = { onEvent(HomeEvent.SaveHome) },
        onClear = { onEvent(HomeEvent.ClearHome) },
        onDismiss = { onEvent(HomeEvent.DismissUpdateHome) }
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
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onEvent(HomeEvent.StartUpdateHome) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Update Home"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Update Home")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ItemCardSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                toBuyItems = state.toBuyItems,
                categories = state.categories,
                onCheckBoxClick = { onEvent(HomeEvent.onCheckBoxClick(it)) },
                onClick = { onEvent(HomeEvent.onCheckBoxClick(it)) },
                onEditClick = { item ->
                    onEvent(HomeEvent.StartEdit(item))
                    isAddItemDialogOpen = true
                },
                onDeleteClick = { item ->
                    onEvent(HomeEvent.StartDelete(item))
                    isDeleteDialogOpen = true
                }
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
    if (toBuyItems.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.size(120.dp),
                painter = painterResource(R.drawable.img_no_items),
                contentDescription = "No Items"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You don't have any items.\nClick the + button to add items.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = modifier,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp)
        ) {
            items(toBuyItems) { toBuyItem ->
                Itemcard(
                    toBuyItem = toBuyItem,
                    categories = categories,
                    onCheckBoxClick = { onCheckBoxClick(toBuyItem) },
                    onClick = { onClick(toBuyItem) },
                    onEditClick = { onEditClick(toBuyItem) },
                    onDeleteClick = { onDeleteClick(toBuyItem) }
                )
            }
        }
    }
}
