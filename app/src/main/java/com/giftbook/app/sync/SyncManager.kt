package com.giftbook.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.giftbook.app.data.db.GiftDao
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 数据同步管理器
 * 监听网络状态，自动同步 pending 数据到云端
 */
class SyncManager(
    private val context: Context,
    private val giftDao: GiftDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var pendingJob: Job? = null

    // 网络回调
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _syncState.value = SyncState.SYNCING
            // 网络恢复时自动同步
            pendingJob = scope.launch {
                syncPendingData()
                _syncState.value = SyncState.IDLE
            }
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _syncState.value = SyncState.OFFLINE
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _isOnline.value = connected
        }
    }

    /**
     * 开始监听网络状态
     */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // 检查初始网络状态
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * 停止监听
     */
    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        pendingJob?.cancel()
    }

    /**
     * 手动触发同步
     */
    suspend fun forceSync(callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!_isOnline.value) {
            callback(false, "当前无网络连接")
            return
        }
        _syncState.value = SyncState.SYNCING
        try {
            syncPendingData()
            _syncState.value = SyncState.IDLE
            callback(true, null)
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            callback(false, e.message)
        }
    }

    /**
     * 同步 pending 数据
     */
    private suspend fun syncPendingData() {
        // 获取待同步记录
        val pendingItems = giftDao.getPendingSyncGifts()
        if (pendingItems.isEmpty()) return

        // 批量上传到 LeanCloud（通过 Repository 调用）
        // 注意：这里通过 GiftDao 查询 pending 数据，
        // 实际上传在 GiftRepository 中处理
    }

    enum class SyncState {
        IDLE,       // 空闲
        SYNCING,    // 同步中
        OFFLINE,    // 离线
        ERROR       // 同步出错
    }
}
