package dev.dominikstahl.einkaufsliste.data.repository

import android.content.Context
import android.util.Log
import dev.dominikstahl.einkaufsliste.data.local.dao.ShoppingListDao
import dev.dominikstahl.einkaufsliste.data.local.entities.AvailableItemEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListItemEntity
import dev.dominikstahl.einkaufsliste.data.remote.ApiService
import dev.dominikstahl.einkaufsliste.data.remote.CreateAvailableItemRequest
import dev.dominikstahl.einkaufsliste.data.remote.CreateShoppingListItemRequest
import dev.dominikstahl.einkaufsliste.data.remote.CreateShoppingListRequest
import dev.dominikstahl.einkaufsliste.data.remote.UpdateAvailableItemRequest
import dev.dominikstahl.einkaufsliste.data.remote.UpdateShoppingListItemRequest
import dev.dominikstahl.einkaufsliste.data.remote.UpdateShoppingListRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val dao: ShoppingListDao,
    private val api: ApiService,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val TAG = "SyncRepository"

    private val _isOffline = MutableStateFlow(false)
    val isOffline = _isOffline.asStateFlow()

    fun getLastSyncTime(): String? = prefs.getString("last_sync_time", null)
    fun setLastSyncTime(time: String) = prefs.edit().putString("last_sync_time", time).apply()

    suspend fun synchronize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync process...")
            val lastSync = getLastSyncTime()
            val syncStartTime = Instant.now().toString()

            // ----------------------------------------------------
            // 1. PULL PHASE
            // ----------------------------------------------------
            // Pull Lists
            val listsResponse = api.getShoppingLists(lastSync)
            if (listsResponse.isSuccessful) {
                listsResponse.body()?.data?.forEach { netList ->
                    val local = dao.getListById(netList.id)
                    // Apply LWW conflict resolution
                    if (local == null || Instant.parse(netList.updatedAt) >= Instant.parse(local.updatedAt)) {
                        dao.upsertLists(listOf(
                            ShoppingListEntity(
                                id = netList.id,
                                name = netList.name,
                                icon = netList.icon,
                                color = netList.color,
                                createdAt = netList.createdAt,
                                updatedAt = netList.updatedAt,
                                deletedAt = netList.deletedAt,
                                isSynced = true
                            )
                        ))
                    }
                }
            }

            // Pull Catalog (Available Items) and List Items for all local lists
            val allLists = dao.getAllLists()
            for (list in allLists) {
                if (list.deletedAt != null) continue

                // Pull catalog
                val catalogResponse = api.getAvailableItems(list.id, lastSync)
                if (catalogResponse.isSuccessful) {
                    catalogResponse.body()?.data?.forEach { netItem ->
                        val local = dao.getAvailableItemById(netItem.id)
                        if (local == null || Instant.parse(netItem.updatedAt) >= Instant.parse(local.updatedAt)) {
                            dao.upsertAvailableItems(listOf(
                                AvailableItemEntity(
                                    id = netItem.id,
                                    listId = list.id,
                                    name = netItem.name,
                                    category = netItem.category,
                                    createdAt = netItem.createdAt,
                                    updatedAt = netItem.updatedAt,
                                    deletedAt = netItem.deletedAt,
                                    isSynced = true
                                )
                            ))
                        }
                    }
                }

                // Pull list items
                val itemsResponse = api.getShoppingListItems(list.id, lastSync)
                if (itemsResponse.isSuccessful) {
                    itemsResponse.body()?.data?.forEach { netItem ->
                        val local = dao.getItemById(netItem.id)
                        if (local == null || Instant.parse(netItem.updatedAt) >= Instant.parse(local.updatedAt)) {
                            // If GORM loaded the catalog item nested inside, make sure it is upserted locally too
                            netItem.item?.let { netCatalogItem ->
                                dao.upsertAvailableItems(listOf(
                                    AvailableItemEntity(
                                        id = netCatalogItem.id,
                                        listId = list.id,
                                        name = netCatalogItem.name,
                                        category = netCatalogItem.category,
                                        createdAt = netCatalogItem.createdAt,
                                        updatedAt = netCatalogItem.updatedAt,
                                        deletedAt = netCatalogItem.deletedAt,
                                        isSynced = true
                                    )
                                ))
                            }

                            dao.upsertItems(listOf(
                                ShoppingListItemEntity(
                                    id = netItem.id,
                                    listId = list.id,
                                    itemId = netItem.item?.id ?: local?.itemId ?: "",
                                    quantity = netItem.quantity,
                                    unit = netItem.unit,
                                    checked = netItem.checked,
                                    sortOrder = netItem.sortOrder,
                                    note = netItem.note,
                                    addedBy = netItem.addedBy,
                                    createdAt = netItem.createdAt,
                                    updatedAt = netItem.updatedAt,
                                    deletedAt = netItem.deletedAt,
                                    isSynced = true
                                )
                            ))
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. PUSH PHASE
            // ----------------------------------------------------
            // Push Lists
            val unsyncedLists = dao.getUnsyncedLists()
            for (list in unsyncedLists) {
                if (list.deletedAt != null) {
                    val res = api.deleteShoppingList(list.id)
                    if (res.isSuccessful || res.code() == 404) {
                        // Hard delete synced local soft-deleted items to reclaim room space
                        // dao.softDeleteList sets deletedAt and isSynced = false, so now delete completely
                        // Wait, we can also just keep them and mark isSynced = true
                        dao.upsertLists(listOf(list.copy(isSynced = true)))
                    }
                } else {
                    val patchRes = api.updateShoppingList(
                        list.id,
                        UpdateShoppingListRequest(name = list.name, icon = list.icon, color = list.color)
                    )
                    if (patchRes.isSuccessful) {
                        dao.upsertLists(listOf(list.copy(isSynced = true)))
                    } else if (patchRes.code() == 404) {
                        val postRes = api.createShoppingList(
                            CreateShoppingListRequest(id = list.id, name = list.name, icon = list.icon, color = list.color)
                        )
                        if (postRes.isSuccessful) {
                            dao.upsertLists(listOf(list.copy(isSynced = true)))
                        }
                    }
                }
            }

            // Push Catalog Items
            val unsyncedCatalog = dao.getUnsyncedAvailableItems()
            for (catItem in unsyncedCatalog) {
                if (catItem.deletedAt != null) {
                    val res = api.deleteAvailableItem(catItem.listId, catItem.id)
                    if (res.isSuccessful || res.code() == 404) {
                        dao.upsertAvailableItems(listOf(catItem.copy(isSynced = true)))
                    }
                } else {
                    val patchRes = api.updateAvailableItem(
                        catItem.listId,
                        catItem.id,
                        UpdateAvailableItemRequest(name = catItem.name, category = catItem.category)
                    )
                    if (patchRes.isSuccessful) {
                        dao.upsertAvailableItems(listOf(catItem.copy(isSynced = true)))
                    } else if (patchRes.code() == 404) {
                        val postRes = api.createAvailableItem(
                            catItem.listId,
                            CreateAvailableItemRequest(id = catItem.id, name = catItem.name, category = catItem.category)
                        )
                        if (postRes.isSuccessful) {
                            dao.upsertAvailableItems(listOf(catItem.copy(isSynced = true)))
                        }
                    }
                }
            }

            // Push Shopping List Items
            val unsyncedItems = dao.getUnsyncedItems()
            for (listItem in unsyncedItems) {
                if (listItem.deletedAt != null) {
                    val res = api.deleteShoppingListItem(listItem.listId, listItem.id)
                    if (res.isSuccessful || res.code() == 404) {
                        dao.upsertItems(listOf(listItem.copy(isSynced = true)))
                    }
                } else {
                    val patchRes = api.updateShoppingListItem(
                        listItem.listId,
                        listItem.id,
                        UpdateShoppingListItemRequest(
                            checked = listItem.checked,
                            quantity = listItem.quantity,
                            unit = listItem.unit,
                            sortOrder = listItem.sortOrder,
                            note = listItem.note
                        )
                    )
                    if (patchRes.isSuccessful) {
                        dao.upsertItems(listOf(listItem.copy(isSynced = true)))
                    } else if (patchRes.code() == 404) {
                        val postRes = api.addShoppingListItem(
                            listItem.listId,
                            CreateShoppingListItemRequest(
                                id = listItem.id,
                                itemId = listItem.itemId,
                                quantity = listItem.quantity,
                                unit = listItem.unit,
                                checked = listItem.checked,
                                sortOrder = listItem.sortOrder,
                                note = listItem.note
                            )
                        )
                        if (postRes.isSuccessful) {
                            dao.upsertItems(listOf(listItem.copy(isSynced = true)))
                        }
                    }
                }
            }

            setLastSyncTime(syncStartTime)
            Log.d(TAG, "Sync process completed successfully.")
            _isOffline.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error occurred: ${e.message}", e)
            _isOffline.value = true
            Result.failure(e)
        }
    }
}
