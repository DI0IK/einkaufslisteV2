package dev.dominikstahl.einkaufsliste.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dominikstahl.einkaufsliste.data.local.dao.ShoppingListDao
import dev.dominikstahl.einkaufsliste.data.local.entities.AvailableItemEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListItemEntity
import dev.dominikstahl.einkaufsliste.data.repository.SyncRepository
import dev.dominikstahl.einkaufsliste.data.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ShoppingListItemWithDetails(
    val listItem: ShoppingListItemEntity,
    val catalogItem: AvailableItemEntity?
)

@HiltViewModel
class ListDetailsViewModel @Inject constructor(
    private val dao: ShoppingListDao,
    private val syncRepository: SyncRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _listId = MutableStateFlow<String?>(null)
    val listId: StateFlow<String?> = _listId.asStateFlow()
    val isOffline = syncRepository.isOffline

    fun setListId(id: String) {
        if (_listId.value != id) {
            _listId.value = id
            triggerSync()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentList: StateFlow<ShoppingListEntity?> = _listId
        .filterNotNull()
        .flatMapLatest { id ->
            // In a production app, we would observe this. For simplicity, we can flow-map it:
            dao.observeActiveLists().map { lists ->
                lists.find { it.id == id }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val listItems: StateFlow<List<ShoppingListItemWithDetails>> = _listId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                dao.observeActiveItems(id),
                dao.observeAvailableItems(id)
            ) { items, catalog ->
                val catalogMap = catalog.associateBy { it.id }
                items.map { ShoppingListItemWithDetails(it, catalogMap[it.itemId]) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val catalogItems: StateFlow<List<AvailableItemEntity>> = _listId
        .filterNotNull()
        .flatMapLatest { id ->
            dao.observeAvailableItems(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.synchronize()
        }
    }

    fun toggleItemChecked(item: ShoppingListItemEntity) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            val updated = item.copy(
                checked = !item.checked,
                updatedAt = now,
                isSynced = false
            )
            dao.upsertItems(listOf(updated))
            triggerSync()
        }
    }

    fun deleteItem(item: ShoppingListItemEntity) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            dao.softDeleteItem(item.id, now, now)
            triggerSync()
        }
    }

    fun deleteCheckedItems() {
        val listIdVal = _listId.value ?: return
        viewModelScope.launch {
            val now = Instant.now().toString()
            dao.softDeleteCheckedItemsForList(listIdVal, now, now)
            triggerSync()
        }
    }

    fun updateItemDetails(itemId: String, quantity: Double, unit: String?, note: String?) {
        viewModelScope.launch {
            val local = dao.getItemById(itemId) ?: return@launch
            val now = Instant.now().toString()
            val updated = local.copy(
                quantity = quantity,
                unit = unit,
                note = note,
                updatedAt = now,
                isSynced = false
            )
            dao.upsertItems(listOf(updated))
            triggerSync()
        }
    }

    fun addCatalogItemToList(catalogItemId: String, quantity: Double, unit: String?, note: String?) {
        val listIdVal = _listId.value ?: return
        viewModelScope.launch {
            val now = Instant.now().toString()
            val newItem = ShoppingListItemEntity(
                id = UUID.randomUUID().toString(),
                listId = listIdVal,
                itemId = catalogItemId,
                quantity = quantity,
                unit = unit,
                checked = false,
                sortOrder = 0,
                note = note,
                addedBy = authManager.username.value,
                createdAt = now,
                updatedAt = now,
                isSynced = false
            )
            dao.upsertItems(listOf(newItem))
            triggerSync()
        }
    }

    fun createNewCatalogItem(name: String, category: String?) {
        val listIdVal = _listId.value ?: return
        viewModelScope.launch {
            val now = Instant.now().toString()
            val newItem = AvailableItemEntity(
                id = UUID.randomUUID().toString(),
                listId = listIdVal,
                name = name,
                category = category?.takeIf { it.isNotBlank() } ?: "Sonstiges",
                createdAt = now,
                updatedAt = now,
                isSynced = false
            )
            dao.upsertAvailableItems(listOf(newItem))
            triggerSync()
        }
    }

    fun updateCatalogItem(catalogItemId: String, name: String, category: String?) {
        viewModelScope.launch {
            val local = dao.getAvailableItemById(catalogItemId) ?: return@launch
            val now = Instant.now().toString()
            val updated = local.copy(
                name = name,
                category = category?.takeIf { it.isNotBlank() } ?: "Sonstiges",
                updatedAt = now,
                isSynced = false
            )
            dao.upsertAvailableItems(listOf(updated))
            triggerSync()
        }
    }

    fun deleteCatalogItem(catalogItemId: String) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            dao.softDeleteAvailableItem(catalogItemId, now, now)
            triggerSync()
        }
    }
}
