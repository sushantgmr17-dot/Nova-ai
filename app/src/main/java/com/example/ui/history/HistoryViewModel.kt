package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.ChatRepository
import com.example.model.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites = _showOnlyFavorites.asStateFlow()

    val uiState: StateFlow<List<ChatSession>> = combine(
        repository.allSessions,
        _searchQuery,
        _showOnlyFavorites
    ) { sessions, query, onlyFavs ->
        sessions.filter { session ->
            val matchesQuery = session.title.contains(query, ignoreCase = true)
            val matchesFav = !onlyFavs || session.isFavorite
            matchesQuery && matchesFav
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun toggleSessionFavorite(sessionId: Int, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateSessionFavorite(sessionId, !currentStatus)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
