package dev.dominikstahl.einkaufsliste.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String? = "📋",
    val color: String? = "#18181B",
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "available_items")
data class AvailableItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String,
    val category: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val itemId: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val checked: Boolean = false,
    val sortOrder: Int = 0,
    val note: String? = null,
    val addedBy: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val isSynced: Boolean = false
)