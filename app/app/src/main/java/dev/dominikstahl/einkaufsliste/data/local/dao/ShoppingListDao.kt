package dev.dominikstahl.einkaufsliste.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.dominikstahl.einkaufsliste.data.local.entities.AvailableItemEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    // --- Shopping Lists ---
    @Query("SELECT * FROM shopping_lists WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun observeActiveLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getListById(id: String): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists")
    suspend fun getAllLists(): List<ShoppingListEntity>

    @Upsert
    suspend fun upsertLists(lists: List<ShoppingListEntity>)

    @Query("UPDATE shopping_lists SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteList(id: String, deletedAt: String, updatedAt: String)

    @Query("SELECT * FROM shopping_lists WHERE isSynced = 0")
    suspend fun getUnsyncedLists(): List<ShoppingListEntity>


    // --- Available Items (Catalog) ---
    @Query("SELECT * FROM available_items WHERE listId = :listId AND deletedAt IS NULL ORDER BY category ASC, name ASC")
    fun observeAvailableItems(listId: String): Flow<List<AvailableItemEntity>>

    @Query("SELECT * FROM available_items WHERE listId = :listId AND deletedAt IS NULL")
    suspend fun getAvailableItemsSync(listId: String): List<AvailableItemEntity>

    @Query("SELECT * FROM available_items WHERE id = :id")
    suspend fun getAvailableItemById(id: String): AvailableItemEntity?

    @Upsert
    suspend fun upsertAvailableItems(items: List<AvailableItemEntity>)

    @Query("UPDATE available_items SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteAvailableItem(id: String, deletedAt: String, updatedAt: String)

    @Query("SELECT * FROM available_items WHERE isSynced = 0")
    suspend fun getUnsyncedAvailableItems(): List<AvailableItemEntity>


    // --- Shopping List Items ---
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId AND deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActiveItems(listId: String): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    suspend fun getItemById(id: String): ShoppingListItemEntity?

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId")
    suspend fun getItemsByListIdSync(listId: String): List<ShoppingListItemEntity>

    @Upsert
    suspend fun upsertItems(items: List<ShoppingListItemEntity>)

    @Query("UPDATE shopping_list_items SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteItem(id: String, deletedAt: String, updatedAt: String)

    @Query("UPDATE shopping_list_items SET deletedAt = :deletedAt, isSynced = 0, updatedAt = :updatedAt WHERE listId = :listId AND checked = 1 AND deletedAt IS NULL")
    suspend fun softDeleteCheckedItemsForList(listId: String, deletedAt: String, updatedAt: String)

    @Query("SELECT * FROM shopping_list_items WHERE isSynced = 0")
    suspend fun getUnsyncedItems(): List<ShoppingListItemEntity>
}