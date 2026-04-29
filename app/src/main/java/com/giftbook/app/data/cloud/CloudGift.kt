package com.giftbook.app.data.cloud

import cn.leancloud.LCObject
import cn.leancloud.LCFile
import java.util.*

/**
 * LeanCloud 云端数据结构映射
 * 对应云端的 "Gift" 表
 */
class CloudGift : LCObject("Gift") {

    constructor() : super()

    constructor(entity: com.giftbook.app.data.db.GiftEntity) : super() {
        put("clientId", entity.id)
        put("userId", entity.ownerId)
        put("name", entity.name)
        put("amount", entity.amount)
        put("date", Date(entity.date))
        put("note", entity.note)
        put("direction", entity.direction)
        put("createdAt", Date(entity.createdAt))
        put("updatedAt", Date(entity.updatedAt))
    }

    fun toEntity(): com.giftbook.app.data.db.GiftEntity {
        return com.giftbook.app.data.db.GiftEntity(
            id = getString("clientId") ?: objectId ?: UUID.randomUUID().toString(),
            name = getString("name") ?: "",
            amount = getDouble("amount"),
            date = getDate("date")?.time ?: System.currentTimeMillis(),
            note = getString("note") ?: "",
            direction = getString("direction") ?: "收入",
            imageUrl = getLCFile("image")?.url ?: "",
            createdAt = createdAt?.time ?: System.currentTimeMillis(),
            updatedAt = updatedAt?.time ?: System.currentTimeMillis(),
            syncStatus = "synced",
            ownerId = getString("userId") ?: ""
        )
    }

    companion object {
        const val CLASS_NAME = "Gift"
    }
}
