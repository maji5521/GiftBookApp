package com.giftbook.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GiftDao {

    @Query("SELECT * FROM gifts WHERE ownerId = :ownerId ORDER BY date DESC")
    fun getAllGifts(ownerId: String): Flow<List<GiftEntity>>

    @Query("SELECT * FROM gifts WHERE ownerId = :ownerId AND name LIKE '%' || :keyword || '%' ORDER BY date DESC")
    fun searchByName(ownerId: String, keyword: String): Flow<List<GiftEntity>>

    @Query("SELECT * FROM gifts WHERE ownerId = :ownerId AND id = :id")
    suspend fun getGiftById(ownerId: String, id: String): GiftEntity?

    @Query("""
        SELECT name, COUNT(*) as count, SUM(amount) as total
        FROM gifts
        WHERE ownerId = :ownerId AND name LIKE '%' || :keyword || '%'
        GROUP BY name
        ORDER BY total DESC
    """)
    fun searchPersonSummary(ownerId: String, keyword: String): Flow<List<PersonSummary>>

    @Query("""
        SELECT COUNT(*) as count,
               SUM(CASE WHEN direction = '收入' THEN amount ELSE 0 END) as totalIncome,
               SUM(CASE WHEN direction = '支出' THEN amount ELSE 0 END) as totalExpense
        FROM gifts
        WHERE ownerId = :ownerId
    """)
    fun getOverviewStats(ownerId: String): Flow<OverviewStats>

    @Query("""
        SELECT COUNT(*) as count,
               SUM(CASE WHEN direction = '收入' THEN amount ELSE 0 END) as totalIncome,
               SUM(CASE WHEN direction = '支出' THEN amount ELSE 0 END) as totalExpense
        FROM gifts
        WHERE ownerId = :ownerId AND name = :name
    """)
    fun getPersonStats(ownerId: String, name: String): Flow<PersonStats>

    @Query("SELECT * FROM gifts WHERE ownerId = :ownerId AND name = :name ORDER BY date DESC")
    fun getGiftsByName(ownerId: String, name: String): Flow<List<GiftEntity>>

    @Query("SELECT DISTINCT name FROM gifts WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getAllNames(ownerId: String): Flow<List<String>>

    @Query("SELECT * FROM gifts WHERE syncStatus = 'pending'")
    suspend fun getPendingSyncGifts(): List<GiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gift: GiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gifts: List<GiftEntity>)

    @Update
    suspend fun update(gift: GiftEntity)

    @Query("UPDATE gifts SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Delete
    suspend fun delete(gift: GiftEntity)

    @Query("DELETE FROM gifts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM gifts WHERE ownerId = :ownerId")
    suspend fun clearAll(ownerId: String)
}

data class PersonSummary(
    val name: String,
    val count: Int,
    val total: Double
)

data class OverviewStats(
    val count: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

data class PersonStats(
    val count: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)
