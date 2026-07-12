package dev.dominikstahl.einkaufsliste.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dominikstahl.einkaufsliste.data.local.dao.ShoppingListDao
import dev.dominikstahl.einkaufsliste.data.local.entities.ShoppingListEntity
import dev.dominikstahl.einkaufsliste.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dao: ShoppingListDao,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val activeLists = dao.observeActiveLists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isOffline = syncRepository.isOffline

    init {
        triggerSync()
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.synchronize()
        }
    }

    fun createNewList(name: String, icon: String?, color: String?) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            val newList = ShoppingListEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon ?: "list",
                color = color ?: "#18181B",
                createdAt = now,
                updatedAt = now,
                isSynced = false
            )
            dao.upsertLists(listOf(newList))
            triggerSync()
        }
    }

    fun updateList(id: String, name: String, icon: String?, color: String?) {
        viewModelScope.launch {
            val local = dao.getListById(id) ?: return@launch
            val now = Instant.now().toString()
            val updated = local.copy(
                name = name,
                icon = icon ?: local.icon,
                color = color ?: local.color,
                updatedAt = now,
                isSynced = false
            )
            dao.upsertLists(listOf(updated))
            triggerSync()
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            dao.softDeleteList(id, now, now)
            triggerSync()
        }
    }
}