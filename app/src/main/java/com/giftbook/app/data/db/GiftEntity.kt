package com.giftbook.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gifts")
data class GiftEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",               // 姓名
    val amount: Double = 0.0,            // 金额
    val date: Long = System.currentTimeMillis(), // 日期
    val note: String = "",               // 备注（事由）
    val direction: String = "收入",       // 收入 / 支出
    val imageUrl: String = "",           // 云端图片 URL
    val localImagePath: String = "",     // 本地图片路径
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending",  // synced / pending / conflict
    val ownerId: String = ""             // 所属用户 ID
)
