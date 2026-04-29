package com.giftbook.app.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giftbook.app.GiftBookApp
import com.giftbook.app.data.db.GiftEntity
import com.giftbook.app.data.db.OverviewStats
import com.giftbook.app.data.db.PersonSummary
import com.giftbook.app.sync.SyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val overviewStats: OverviewStats = OverviewStats(),
    val recentGifts: List<GiftEntity> = emptyList(),
    val searchResults: List<PersonSummary> = emptyList(),
    val searchQuery: String = "",
    val syncState: SyncManager.SyncState = SyncManager.SyncState.IDLE,
    val isOnline: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GiftBookApp
    private val repository = app.repository
    private val authManager = app.authManager
    private val syncManager = app.syncManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = authManager.currentUserId ?: ""

    init {
        loadData()

        // 监听同步状态
        viewModelScope.launch {
            syncManager.syncState.collect { state ->
                _uiState.update { it.copy(syncState = state) }
            }
        }
        viewModelScope.launch {
            syncManager.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                repository.getOverviewStats(currentUserId).collect { stats ->
                    _uiState.update { it.copy(overviewStats = stats) }
                }
            }
            launch {
                repository.getAllGifts(currentUserId).collect { gifts ->
                    _uiState.update { it.copy(recentGifts = gifts.take(50)) }
                }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update { it.copy(searchResults = emptyList()) }
            } else {
                repository.searchPersonSummary(currentUserId, query).collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            syncManager.forceSync { success, error ->
                if (success) {
                    // 重新加载数据
                    loadData()
                }
            }
        }
    }

    fun logout() {
        authManager.logout()
    }
}
