package dev.dominikstahl.einkaufsliste.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.dominikstahl.einkaufsliste.data.local.dao.ShoppingListDao
import dev.dominikstahl.einkaufsliste.data.local.entities.AvailableItemEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListEntity
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListItemEntity

@Database(
    entities = [
        ShoppingListEntity::class,
        AvailableItemEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
}