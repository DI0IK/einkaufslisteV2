package dev.dominikstahl.einkaufsliste.di

import android.content.Context
import androidx.room.Room
import dev.dominikstahl.einkaufsliste.data.local.AppDatabase
import dev.dominikstahl.einkaufsliste.data.local.dao.ShoppingListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "einkaufsliste_db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    @Singleton
    fun provideShoppingListDao(database: AppDatabase): ShoppingListDao {
        return database.shoppingListDao()
    }
}