package dev.dominikstahl.einkaufsliste.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- Shopping Lists ---
    @GET("shopping-lists")
    suspend fun getShoppingLists(
        @Query("updatedSince") updatedSince: String? = null
    ): Response<ApiResponse<List<NetworkShoppingList>>>

    @POST("shopping-lists")
    suspend fun createShoppingList(
        @Body request: CreateShoppingListRequest
    ): Response<ApiResponse<NetworkShoppingList>>

    @PATCH("shopping-lists/{listId}")
    suspend fun updateShoppingList(
        @Path("listId") listId: String,
        @Body request: UpdateShoppingListRequest
    ): Response<ApiResponse<NetworkShoppingList>>

    @DELETE("shopping-lists/{listId}")
    suspend fun deleteShoppingList(
        @Path("listId") listId: String
    ): Response<Unit>

    // --- Available Items (Catalog) ---
    @GET("shopping-lists/{listId}/catalog")
    suspend fun getAvailableItems(
        @Path("listId") listId: String,
        @Query("updatedSince") updatedSince: String? = null
    ): Response<ApiResponse<List<NetworkAvailableItem>>>

    @POST("shopping-lists/{listId}/catalog")
    suspend fun createAvailableItem(
        @Path("listId") listId: String,
        @Body request: CreateAvailableItemRequest
    ): Response<ApiResponse<NetworkAvailableItem>>

    @PATCH("shopping-lists/{listId}/catalog/{itemId}")
    suspend fun updateAvailableItem(
        @Path("listId") listId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateAvailableItemRequest
    ): Response<ApiResponse<NetworkAvailableItem>>

    @DELETE("shopping-lists/{listId}/catalog/{itemId}")
    suspend fun deleteAvailableItem(
        @Path("listId") listId: String,
        @Path("itemId") itemId: String
    ): Response<Unit>

    // --- Shopping List Items ---
    @GET("shopping-lists/{listId}/items")
    suspend fun getShoppingListItems(
        @Path("listId") listId: String,
        @Query("updatedSince") updatedSince: String? = null
    ): Response<ApiResponse<List<NetworkShoppingListItem>>>

    @POST("shopping-lists/{listId}/items")
    suspend fun addShoppingListItem(
        @Path("listId") listId: String,
        @Body request: CreateShoppingListItemRequest
    ): Response<ApiResponse<NetworkShoppingListItem>>

    @PATCH("shopping-lists/{listId}/items/{listItemId}")
    suspend fun updateShoppingListItem(
        @Path("listId") listId: String,
        @Path("listItemId") listItemId: String,
        @Body request: UpdateShoppingListItemRequest
    ): Response<ApiResponse<NetworkShoppingListItem>>

    @DELETE("shopping-lists/{listId}/items/{listItemId}")
    suspend fun deleteShoppingListItem(
        @Path("listId") listId: String,
        @Path("listItemId") listItemId: String
    ): Response<Unit>
}
