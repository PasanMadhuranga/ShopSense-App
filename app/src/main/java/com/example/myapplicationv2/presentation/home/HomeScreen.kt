package com.example.myapplicationv2.presentation.home

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplicationv2.domain.model.ToBuyItem
import com.example.myapplicationv2.presentation.components.Itemcard
import com.example.myapplicationv2.R
import com.example.myapplicationv2.domain.model.Category
import com.example.myapplicationv2.presentation.components.AddEditItemDialog
import com.example.myapplicationv2.presentation.components.DeleteDialog
import com.example.myapplicationv2.presentation.components.HomeLocationPickerScreen
import com.example.myapplicationv2.presentation.components.UpdateHomeDialog
import com.example.myapplicationv2.shopping.ShoppingModeStatusReceiver
import com.example.myapplicationv2.util.SnackBarEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialHighlightIds: List<Int> = emptyList()
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Highlight state for items opened from notification
    var highlightedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isHighlighting by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialHighlightIds) {
        if (initialHighlightIds.isNotEmpty()) {
            highlightedIds = initialHighlightIds.toSet()
            isHighlighting = true
            kotlinx.coroutines.delay(3000)
            isHighlighting = false
            highlightedIds = emptySet()
        }
    }

    val onEvent = viewModel::onEvent
    val snackBarEvent = viewModel.snackbarEventFlow

    var isAddItemDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Category filter state (null = all)
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCategoryName = selectedCategoryId?.let { id ->
        state.categories.find { it.id == id }?.name
    } ?: "All categories"

    // Filter items according to selected category
    val filteredItems = remember(state.toBuyItems, selectedCategoryId) {
        if (selectedCategoryId == null) {
            state.toBuyItems
        } else {
            state.toBuyItems.filter { it.categoryId == selectedCategoryId }
        }
    }

    // Receiver that listens for SHOPPING_MODE_STOPPED from the service
    val shoppingStoppedReceiver = remember {
        ShoppingModeStatusReceiver {
            onEvent(HomeEvent.SetShoppingModeOff)
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter("SHOPPING_MODE_STOPPED")

        ContextCompat.registerReceiver(
            context,
            shoppingStoppedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(shoppingStoppedReceiver)
        }
    }

    // Launcher to ask for location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            onEvent(HomeEvent.UseCurrentLocation)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission is required to use current location.")
            }
        }
    }

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || android.os.Build.VERSION.SDK_INT < 33) {
            // Now we can safely toggle Shopping Mode ON
            onEvent(HomeEvent.ToggleShoppingMode)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Notification permission is needed to show store suggestions."
                )
            }
        }
    }

    // Location permission launcher specifically for Shopping Mode
    val shoppingLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            // If notifications also need runtime permission, request that next
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onEvent(HomeEvent.ToggleShoppingMode)
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Location permission is required to use Shopping Mode."
                )
            }
        }
    }

    LaunchedEffect(key1 = true) {
        snackBarEvent.collectLatest { event ->
            when (event) {
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
        onPickOnMap = {
            onEvent(HomeEvent.DismissUpdateHome)
            onEvent(HomeEvent.StartSelectHomeOnMap)
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
            // Update home button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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

            Spacer(modifier = Modifier.height(16.dp))

            // Shopping mode toggle
            // Shopping mode section with light red background
            val shoppingModeColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = shoppingModeColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Shopping Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Get nearby store suggestions based on your list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isShoppingModeActive,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // User is trying to enable Shopping Mode
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                val coarseGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                // Background location: needed for "Allow all the time"
                                val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }

                                val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }

                                when {
                                    // Ask for foreground location first
                                    !fineGranted && !coarseGranted -> {
                                        shoppingLocationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }

                                    // Foreground is granted but "all the time" is not
                                    !backgroundGranted -> {
                                        showBackgroundLocationDialog = true
                                    }

                                    // Location is fine, but notifications are not
                                    !notificationGranted && Build.VERSION.SDK_INT >= 33 -> {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }

                                    // Everything is granted
                                    else -> {
                                        onEvent(HomeEvent.ToggleShoppingMode)
                                    }
                                }
                            } else {
                                // Turning Shopping Mode OFF does not need permissions
                                onEvent(HomeEvent.ToggleShoppingMode)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category filter dropdown
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter by category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    // "All categories" item
                    DropdownMenuItem(
                        text = { Text("All categories") },
                        onClick = {
                            selectedCategoryId = null
                            isCategoryDropdownExpanded = false
                        }
                    )
                    // One entry per category
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategoryId = category.id
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ItemCardSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                toBuyItems = filteredItems,
                categories = state.categories,
                highlightedIds = if (isHighlighting) highlightedIds else emptySet(),
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

    if (showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            title = { Text("Allow location all the time") },
            text = {
                Text(
                    "Shopping Mode needs access to your location even when the app is not open. " +
                            "In the next screen, go to Permissions > Location > Allow all the time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundLocationDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackgroundLocationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.isSelectingHomeOnMap) {
        HomeLocationPickerScreen(
            initialLat = state.homeLat,
            initialLng = state.homeLng,
            onConfirm = { lat, lng ->
                onEvent(HomeEvent.OnHomeLocationSelected(lat, lng))
            },
            onCancel = {
                onEvent(HomeEvent.DismissUpdateHome)
            }
        )
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
    highlightedIds: Set<Int> = emptySet(),
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
                val isHighlighted = toBuyItem.id != null && highlightedIds.contains(toBuyItem.id)

                Itemcard(
                    toBuyItem = toBuyItem,
                    categories = categories,
                    isHighlighted = isHighlighted,
                    onCheckBoxClick = { onCheckBoxClick(toBuyItem) },
                    onClick = { onClick(toBuyItem) },
                    onEditClick = { onEditClick(toBuyItem) },
                    onDeleteClick = { onDeleteClick(toBuyItem) }
                )
            }
        }
    }
}
