package dev.dominikstahl.einkaufsliste.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class NetworkShoppingList(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null
)

@Serializable
data class NetworkAvailableItem(
    val id: String,
    val name: String,
    val category: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null
)

@Serializable
data class NetworkShoppingListItem(
    val id: String,
    val item: NetworkAvailableItem? = null, // Backend returns item object on GET
    val quantity: Double,
    val unit: String? = null,
    val checked: Boolean = false,
    val sortOrder: Int = 0,
    val note: String? = null,
    val addedBy: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null
)

// Request payloads
@Serializable
data class CreateShoppingListRequest(
    val id: String,
    val name: String,
    val icon: String?,
    val color: String?
)

@Serializable
data class CreateAvailableItemRequest(
    val id: String,
    val name: String,
    val category: String?
)

@Serializable
data class CreateShoppingListItemRequest(
    val id: String,
    val itemId: String,
    val quantity: Double,
    val unit: String?,
    val checked: Boolean,
    val sortOrder: Int,
    val note: String?
)

@Serializable
data class UpdateShoppingListRequest(
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null
)

@Serializable
data class UpdateAvailableItemRequest(
    val name: String? = null,
    val category: String? = null
)

@Serializable
data class UpdateShoppingListItemRequest(
    val checked: Boolean? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val sortOrder: Int? = null,
    val note: String? = null
)
