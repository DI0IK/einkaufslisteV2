package dev.dominikstahl.einkaufsliste.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ExitToApp
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListEntity

private val PRESET_ICONS = listOf(
    "shopping_cart" to Icons.Default.ShoppingCart,
    "list" to Icons.Default.List,
    "local_drinks" to Icons.Default.LocalDrink,
    "wine_bar" to Icons.Default.WineBar,
    "sports_bar" to Icons.Default.SportsBar,
    "local_cafe" to Icons.Default.LocalCafe,
    "apartment" to Icons.Default.Apartment,
    "store" to Icons.Default.Store,
    "restaurant" to Icons.Default.Restaurant,
    "checkroom" to Icons.Default.Checkroom,
    "pets" to Icons.Default.Pets,
    "book" to Icons.Default.Book,
    "local_pharmacy" to Icons.Default.LocalPharmacy,
    "build" to Icons.Default.Build,
    "home" to Icons.Default.Home,
    "star" to Icons.Default.Star,
    "place" to Icons.Default.Place,
    "face" to Icons.Default.Face,
    "settings" to Icons.Default.Settings
)

private val PRESET_COLORS = listOf(
    "#18181B" to Color(0xFF18181B), // Zinc
    "#EF4444" to Color(0xFFEF4444), // Red
    "#F59E0B" to Color(0xFFF59E0B), // Orange
    "#10B981" to Color(0xFF10B981), // Green
    "#3B82F6" to Color(0xFF3B82F6), // Blue
    "#8B5CF6" to Color(0xFF8B5CF6), // Purple
    "#EC4899" to Color(0xFFEC4899), // Pink
    "#64748B" to Color(0xFF64748B)  // Slate
)

fun parseHexColor(hex: String?, default: Color = Color(0xFF18181B)): Color {
    if (hex == null) return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        default
    }
}

@Composable
fun resolveListColor(hex: String?): Color {
    val isDark = isSystemInDarkTheme()
    val defaultColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF18181B)
    if (hex == null) return defaultColor
    if (hex == "#18181B" && isDark) {
        return Color(0xFFF4F4F5)
    }
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}


fun getMaterialIconByName(name: String?): ImageVector {
    return when (name) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "list" -> Icons.Default.List
        "local_drinks" -> Icons.Default.LocalDrink
        "wine_bar" -> Icons.Default.WineBar
        "sports_bar" -> Icons.Default.SportsBar
        "local_cafe" -> Icons.Default.LocalCafe
        "apartment" -> Icons.Default.Apartment
        "store" -> Icons.Default.Store
        "restaurant" -> Icons.Default.Restaurant
        "checkroom" -> Icons.Default.Checkroom
        "pets" -> Icons.Default.Pets
        "book" -> Icons.Default.Book
        "local_pharmacy" -> Icons.Default.LocalPharmacy
        "build" -> Icons.Default.Build
        "home" -> Icons.Default.Home
        "star" -> Icons.Default.Star
        "place" -> Icons.Default.Place
        "face" -> Icons.Default.Face
        "settings" -> Icons.Default.Settings
        else -> Icons.Default.List
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun HomeScreen(
    lists: List<ShoppingListEntity>,
    isOffline: Boolean,
    onListClick: (String) -> Unit,
    onCreateList: (name: String, icon: String?, color: String?) -> Unit,
    onUpdateList: (id: String, name: String, icon: String?, color: String?) -> Unit,
    onDeleteList: (id: String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedListForEdit by remember { mutableStateOf<ShoppingListEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Einkaufslisten",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    if (isOffline) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp).size(20.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Abmelden",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lists, key = { it.id }) { list ->
                val listColor = resolveListColor(list.color)
                val iconVector = remember(list.icon) { getMaterialIconByName(list.icon) }

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
                            onClick = { onListClick(list.id) },
                            onLongClick = {
                                selectedListForEdit = list
                                showEditSheet = true
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(listColor.copy(alpha = 0.08f))
                                .border(
                                    BorderStroke(1.5.dp, listColor.copy(alpha = 0.2f)),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = listColor
                            )
                        }

                        Text(
                            text = list.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.2).sp
                        )
                    }

                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (lists.isEmpty()) {
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
                                text = "Keine aktiven Listen vorhanden",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Erstelle eine neue Liste mit dem FAB",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Create List Bottom Sheet ---
    if (showCreateSheet) {
        var listName by remember { mutableStateOf("") }
        var selectedIcon by remember { mutableStateOf(PRESET_ICONS.first().first) }
        var selectedColorHex by remember { mutableStateOf(PRESET_COLORS.first().first) }

        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
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
                    text = "Neue Liste",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    placeholder = { Text("Name der Liste...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Symbol wählen",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_ICONS.forEach { (name, icon) ->
                        val isSelected = selectedIcon == name
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Farbe wählen",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_COLORS.forEach { (hex, color) ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 0.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                    ),
                                    CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (listName.isNotBlank()) {
                            onCreateList(listName, selectedIcon, selectedColorHex)
                            showCreateSheet = false
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

    // --- Edit List Bottom Sheet ---
    if (showEditSheet && selectedListForEdit != null) {
        val list = selectedListForEdit!!
        var listName by remember { mutableStateOf(list.name) }
        var selectedIcon by remember { mutableStateOf(list.icon ?: PRESET_ICONS.first().first) }
        var selectedColorHex by remember { mutableStateOf(list.color ?: PRESET_COLORS.first().first) }

        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
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
                    text = "Liste bearbeiten",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    placeholder = { Text("Name der Liste...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Symbol wählen",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_ICONS.forEach { (name, icon) ->
                        val isSelected = selectedIcon == name
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Farbe wählen",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_COLORS.forEach { (hex, color) ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 0.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                    ),
                                    CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDeleteList(list.id)
                            showEditSheet = false
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
                            if (listName.isNotBlank()) {
                                onUpdateList(list.id, listName, selectedIcon, selectedColorHex)
                                showEditSheet = false
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
}