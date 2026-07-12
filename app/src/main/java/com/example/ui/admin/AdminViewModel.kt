package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: ChatRepository) : ViewModel() {
    val totalSessions: StateFlow<Int> = repository.getTotalSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val totalMessages: StateFlow<Int> = repository.getTotalMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllData()
        }
    }
}
