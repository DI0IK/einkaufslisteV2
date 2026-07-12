package dev.dominikstahl.einkaufsliste.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dominikstahl.einkaufsliste.data.local.entities.AvailableItemEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListItemEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailsScreen(
    viewModel: ListDetailsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentList by viewModel.currentList.collectAsStateWithLifecycle()
    val listItems by viewModel.listItems.collectAsStateWithLifecycle()
    val catalogItems by viewModel.catalogItems.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Bottom Sheet States
    var showAddItemSheet by remember { mutableStateOf(false) }
    var showCreateCatalogSheet by remember { mutableStateOf(false) }
    var showEditCatalogSheet by remember { mutableStateOf(false) }
    var showEditItemDialog by remember { mutableStateOf(false) }
    var showDeleteCheckedConfirmDialog by remember { mutableStateOf(false) }

    var selectedCatalogItemForEdit by remember { mutableStateOf<AvailableItemEntity?>(null) }
    var selectedListItemForEdit by remember { mutableStateOf<ShoppingListItemWithDetails?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentList?.name ?: "Einkaufsliste",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text(
                            text = "←",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isOffline) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 16.dp).size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            val hasCheckedItems = remember(listItems) {
                listItems.any { it.listItem.checked }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
            ) {
                if (selectedTabIndex == 0 && hasCheckedItems) {
                    FloatingActionButton(
                        onClick = { showDeleteCheckedConfirmDialog = true },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Erledigte Artikel löschen",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (selectedTabIndex == 0) {
                            showAddItemSheet = true
                        } else {
                            showCreateCatalogSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Modern Sub-Tab bar
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Artikel", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Katalog", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // --- Tab 1: Items List ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listItems, key = { it.listItem.id }) { item ->
                        SwipeableListItem(
                            item = item,
                            onCheckedToggle = { viewModel.toggleItemChecked(item.listItem) },
                            onDelete = { viewModel.deleteItem(item.listItem) },
                            onLongClick = {
                                selectedListItemForEdit = item
                                showEditItemDialog = true
                            }
                        )
                    }

                    if (listItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Liste ist leer",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Füge Artikel über das FAB (+) hinzu.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // --- Tab 2: Catalog Tab ---
                val groupedCatalog = remember(catalogItems) {
                    catalogItems.groupBy { it.category ?: "Sonstiges" }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedCatalog.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        items(items, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedCatalogItemForEdit = item
                                        showEditCatalogSheet = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Bearbeiten",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (catalogItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Keine Vorlagen im Katalog",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Erstelle eine neue Vorlage mit dem FAB (+)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FAB Slidein: Search + Select Catalog Item to Add to List ---
        if (showAddItemSheet) {
            var searchQuery by remember { mutableStateOf("") }
            var selectedCatalogItem by remember { mutableStateOf<AvailableItemEntity?>(null) }
            var quantityStr by remember { mutableStateOf("1") }
            var unit by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            val filteredCatalog = remember(searchQuery, catalogItems) {
                if (searchQuery.isBlank()) {
                    catalogItems
                } else {
                    catalogItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }
            }

            val groupedFilteredCatalog = remember(filteredCatalog) {
                filteredCatalog.groupBy { it.category ?: "Sonstiges" }
            }

            ModalBottomSheet(
                onDismissRequest = { showAddItemSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        text = "Artikel hinzufügen",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedCatalogItem == null) {
                        // Phase 1: Search and Select Product
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Artikel suchen...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Match Catalog list
                        Box(modifier = Modifier.heightIn(max = 250.dp)) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                groupedFilteredCatalog.forEach { (category, items) ->
                                    item {
                                        Text(
                                            text = category.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                        )
                                    }

                                    items(items) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { selectedCatalogItem = item }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (filteredCatalog.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Kein Treffer. Erstelle erst im 'Katalog'-Tab Vorlagen.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Phase 2: Input amount and note/prefix details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedCatalogItem!!.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedCatalogItem!!.category ?: "",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { selectedCatalogItem = null }) {
                                Text(
                                    text = "Ändern",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("Menge") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Einheit (z.B. kg, Stk)") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Zusatz/Notiz (z.B. Bio, Großpackung)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val qty = quantityStr.toDoubleOrNull() ?: 1.0
                                viewModel.addCatalogItemToList(
                                    selectedCatalogItem!!.id,
                                    qty,
                                    unit.takeIf { it.isNotBlank() },
                                    note.takeIf { it.isNotBlank() }
                                )
                                showAddItemSheet = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Hinzufügen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Create Catalog AvailableItem Bottom Sheet ---
        if (showCreateCatalogSheet) {
            var itemName by remember { mutableStateOf("") }
            var itemCategory by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { showCreateCatalogSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        text = "Neuer Katalog-Eintrag",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("Produktname (z.B. Milch)...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = itemCategory,
                        onValueChange = { itemCategory = it },
                        placeholder = { Text("Kategorie (z.B. Milchprodukte)...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                viewModel.createNewCatalogItem(itemName, itemCategory)
                                showCreateCatalogSheet = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Erstellen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Edit Catalog Item Bottom Sheet ---
        if (showEditCatalogSheet && selectedCatalogItemForEdit != null) {
            val item = selectedCatalogItemForEdit!!
            var itemName by remember { mutableStateOf(item.name) }
            var itemCategory by remember { mutableStateOf(item.category ?: "") }

            ModalBottomSheet(
                onDismissRequest = { showEditCatalogSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        text = "Vorlage bearbeiten",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("Produktname...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = itemCategory,
                        onValueChange = { itemCategory = it },
                        placeholder = { Text("Kategorie...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteCatalogItem(item.id)
                                showEditCatalogSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Löschen", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (itemName.isNotBlank()) {
                                    viewModel.updateCatalogItem(item.id, itemName, itemCategory)
                                    showEditCatalogSheet = false
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Speichern", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Long Press Dialog: Edit Active Item Details ---
        if (showEditItemDialog && selectedListItemForEdit != null) {
            val item = selectedListItemForEdit!!
            var quantityStr by remember { mutableStateOf(item.listItem.quantity.toString()) }
            var unit by remember { mutableStateOf(item.listItem.unit ?: "") }
            var note by remember { mutableStateOf(item.listItem.note ?: "") }

            AlertDialog(
                onDismissRequest = { showEditItemDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val qty = quantityStr.toDoubleOrNull() ?: 1.0
                            viewModel.updateItemDetails(
                                item.listItem.id,
                                qty,
                                unit.takeIf { it.isNotBlank() },
                                note.takeIf { it.isNotBlank() }
                            )
                            showEditItemDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Speichern", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditItemDialog = false }) {
                        Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                title = {
                    Text(
                        text = item.catalogItem?.name ?: "Artikel bearbeiten",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("Menge") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Einheit") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Notiz/Zusatz") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showDeleteCheckedConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteCheckedConfirmDialog = false },
                title = {
                    Text(
                        text = "Erledigte Artikel löschen?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Möchtest du wirklich alle erledigten Artikel von dieser Liste unwiderruflich entfernen?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCheckedItems()
                            showDeleteCheckedConfirmDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Löschen", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteCheckedConfirmDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Abbrechen")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableListItem(
    item: ShoppingListItemWithDetails,
    onCheckedToggle: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentOnCheckedToggle by rememberUpdatedState(onCheckedToggle)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val isItemChecked by rememberUpdatedState(item.listItem.checked)

    var dismissStateRef: SwipeToDismissBoxState? = null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    coroutineScope.launch {
                        dismissStateRef?.snapTo(SwipeToDismissBoxValue.Settled)
                        currentOnCheckedToggle()
                    }
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isItemChecked) {
                        currentOnDelete()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    )
    dismissStateRef = dismissState

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981)
                SwipeToDismissBoxValue.EndToStart -> {
                    if (item.listItem.checked) MaterialTheme.colorScheme.error else Color.Transparent
                }
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (item.listItem.checked) Icons.Default.Clear else Icons.Default.Check
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (item.listItem.checked) Icons.Default.Delete else null
                }
                else -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    RoundedCornerShape(12.dp)
                )
                .combinedClickable(
                    onClick = { onCheckedToggle() },
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (item.listItem.checked) Color(0xFF10B981) else Color.Transparent)
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (item.listItem.checked) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.listItem.checked) {
                        Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column {
                    Text(
                        text = item.catalogItem?.name ?: "Unbekanntes Produkt",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.listItem.checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(
                            textDecoration = if (item.listItem.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    )
                    if (!item.listItem.note.isNullOrBlank()) {
                        Text(
                            text = item.listItem.note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (item.listItem.quantity > 0.0) {
                val qtyText = if (item.listItem.quantity % 1.0 == 0.0) {
                    item.listItem.quantity.toInt().toString()
                } else {
                    item.listItem.quantity.toString()
                }
                val unitText = item.listItem.unit ?: ""
                Text(
                    text = "$qtyText $unitText".trim(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
