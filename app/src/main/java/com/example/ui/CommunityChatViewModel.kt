package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommunityChatRepository
import com.example.data.CommunityMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommunityChatRepository(application)

    val isAlwaysOn: StateFlow<Boolean> = repository.isAlwaysOn
    val userProfileName: StateFlow<String> = repository.userProfileName
    val userRole: StateFlow<String> = repository.userRole

    private val _isChatSheetOpen = MutableStateFlow(false)
    val isChatSheetOpen: StateFlow<Boolean> = _isChatSheetOpen.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _rawMessages = repository.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMessages: StateFlow<List<CommunityMessage>> = combine(
        _rawMessages,
        _selectedCategory
    ) { messages, category ->
        if (category == "All") {
            messages
        } else {
            messages.filter { it.category.equals(category, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestMessage: StateFlow<CommunityMessage?> = _rawMessages.map { list ->
        list.lastOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var previousMessageCount = 0

    init {
        viewModelScope.launch {
            _rawMessages.collect { list ->
                if (previousMessageCount > 0 && list.size > previousMessageCount && !_isChatSheetOpen.value) {
                    _unreadCount.value += (list.size - previousMessageCount)
                }
                previousMessageCount = list.size
            }
        }
    }

    fun toggleAlwaysOn(enabled: Boolean) {
        repository.toggleAlwaysOn(enabled)
    }

    fun openChat() {
        _isChatSheetOpen.value = true
        _unreadCount.value = 0
    }

    fun closeChat() {
        _isChatSheetOpen.value = false
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun sendMessage(content: String, category: String = "General", locationTag: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                content = content,
                category = category,
                locationTag = locationTag
            )
        }
    }

    fun reactToMessage(messageId: String) {
        viewModelScope.launch {
            repository.reactToMessage(messageId)
        }
    }

    fun updateProfile(name: String, role: String) {
        repository.updateProfile(name, role)
    }
}
