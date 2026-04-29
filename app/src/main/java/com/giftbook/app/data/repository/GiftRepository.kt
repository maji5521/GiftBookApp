package com.giftbook.app.data.repository

import com.giftbook.app.data.cloud.LeanCloudManager
import com.giftbook.app.data.db.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

/**
 * 统一数据仓库
 * 管理本地 Room 和云端 LeanCloud 的数据操作
 */
class GiftRepository(
    private val giftDao: GiftDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    // ==================== 本地查询 ====================

    fun getAllGifts(ownerId: String): Flow<List<GiftEntity>> =
        giftDao.getAllGifts(ownerId)

    fun searchByName(ownerId: String, keyword: String): Flow<List<GiftEntity>> =
        giftDao.searchByName(ownerId, keyword)

    fun searchPersonSummary(ownerId: String, keyword: String): Flow<List<PersonSummary>> =
        giftDao.searchPersonSummary(ownerId, keyword)

    fun getOverviewStats(ownerId: String): Flow<OverviewStats> =
        giftDao.getOverviewStats(ownerId)

    fun getPersonStats(ownerId: String, name: String): Flow<PersonStats> =
        giftDao.getPersonStats(ownerId, name)

    fun getGiftsByName(ownerId: String, name: String): Flow<List<GiftEntity>> =
        giftDao.getGiftsByName(ownerId, name)

    fun getAllNames(ownerId: String): Flow<List<String>> =
        giftDao.getAllNames(ownerId)

    // ==================== 本地写入 ====================

    suspend fun insertGift(gift: GiftEntity) {
        giftDao.insert(gift)
    }

    suspend fun insertGifts(gifts: List<GiftEntity>) {
        giftDao.insertAll(gifts)
    }

    suspend fun updateGift(gift: GiftEntity) {
        giftDao.update(gift)
    }

    suspend fun deleteGift(gift: GiftEntity) {
        giftDao.delete(gift)
    }

    suspend fun deleteGiftById(id: String) {
        giftDao.deleteById(id)
    }

    // ==================== 云端同步 ====================

    /**
     * 同步所有本地 pending 数据到云端
     */
    suspend fun syncPendingToCloud(ownerId: String) {
        val pendingItems = giftDao.getPendingSyncGifts()
            .filter { it.ownerId == ownerId }
        if (pendingItems.isEmpty()) return

        LeanCloudManager.saveGifts(pendingItems) { success, _ ->
            if (success) {
                scope.launch {
                    pendingItems.forEach { item ->
                        giftDao.updateSyncStatus(item.id, "synced")
                    }
                }
            }
        }
    }

    /**
     * 从云端拉取数据并合并到本地
     */
    suspend fun syncFromCloud(ownerId: String, callback: (Boolean, String?) -> Unit) {
        LeanCloudManager.queryAllGifts(ownerId) { cloudItems, error ->
            if (cloudItems != null) {
                scope.launch {
                    val items = cloudItems.map { it.copy(syncStatus = "synced", ownerId = ownerId) }
                    giftDao.insertAll(items)
                    callback(true, null)
                }
            } else {
                callback(false, error)
            }
        }
    }

    /**
     * 保存记录并同步到云端
     */
    suspend fun saveAndSync(gift: GiftEntity) {
        val entity = gift.copy(syncStatus = "pending")
        giftDao.insert(entity)
        LeanCloudManager.saveGift(entity) { success, _ ->
            if (success) {
                scope.launch {
                    giftDao.updateSyncStatus(entity.id, "synced")
                }
            }
        }
    }

    /**
     * 删除记录
     */
    suspend fun deleteAndSync(gift: GiftEntity, cloudObjectId: String? = null) {
        giftDao.delete(gift)
        if (cloudObjectId != null) {
            LeanCloudManager.deleteGift(cloudObjectId) { _, _ -> }
        }
    }
}
