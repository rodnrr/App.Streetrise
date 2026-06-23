package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CachedProviderProfile
import com.example.data.CachedService
import com.example.data.ServiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServiceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ServiceRepository(database.cachedServiceDao())

    val servicesWithProfiles: StateFlow<List<Pair<CachedService, CachedProviderProfile?>>> =
        repository.servicesWithProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.syncData()
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.syncData()
        }
    }
}
