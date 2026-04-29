package com.giftbook.app.data.cloud

import android.content.Context
import cn.leancloud.*
import cn.leancloud.core.LeanCloud
import cn.leancloud.core.LeanService
import cn.leancloud.json.JSONArray
import cn.leancloud.types.LCNull
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.*

/**
 * LeanCloud 云服务管理器
 * 负责初始化、用户管理和数据操作
 */
object LeanCloudManager {

    private var initialized = false

    /**
     * 初始化 LeanCloud SDK
     * 请在 Application.onCreate() 中调用
     */
    fun init(context: Context, appId: String, appKey: String, serverUrl: String) {
        if (initialized) return

        LeanCloud.initialize(appId, appKey, serverUrl)
        LeanCloud.setLogLevel(LCLogger.Level.ERROR)

        initialized = true
    }

    // ==================== 云存储操作 ====================

    /**
     * 保存礼品记录到云端
     */
    fun saveGift(entity: com.giftbook.app.data.db.GiftEntity, callback: (Boolean, String?) -> Unit) {
        val cloudGift = CloudGift(entity)
        cloudGift.saveInBackground().subscribe(object : Observer<LCObject> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: LCObject) {
                callback(true, t.objectId)
            }
            override fun onError(e: Throwable) {
                callback(false, e.message)
            }
            override fun onComplete() {}
        })
    }

    /**
     * 批量保存礼品记录
     */
    fun saveGifts(entities: List<com.giftbook.app.data.db.GiftEntity>, callback: (Boolean, String?) -> Unit) {
        val objects = entities.map { CloudGift(it) as LCObject }
        LCObject.saveAllInBackground(objects).subscribe(object : Observer<JSONArray> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: JSONArray) {
                callback(true, null)
            }
            override fun onError(e: Throwable) {
                callback(false, e.message)
            }
            override fun onComplete() {}
        })
    }

    /**
     * 查询当前用户的所有记录
     */
    fun queryAllGifts(userId: String, callback: (List<com.giftbook.app.data.db.GiftEntity>?, String?) -> Unit) {
        val query = LCQuery<CloudGift>(CloudGift.CLASS_NAME)
        query.whereEqualTo("userId", userId)
        query.orderByDescending("date")
        query.findInBackground().subscribe(object : Observer<List<CloudGift>> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: List<CloudGift>) {
                val entities = t.mapNotNull { it.toEntity() }
                callback(entities, null)
            }
            override fun onError(e: Throwable) {
                callback(null, e.message)
            }
            override fun onComplete() {}
        })
    }

    /**
     * 更新记录
     */
    fun updateGift(entity: com.giftbook.app.data.db.GiftEntity, cloudObjectId: String?, callback: (Boolean, String?) -> Unit) {
        if (cloudObjectId == null) {
            saveGift(entity, callback)
            return
        }
        val cloudGift = CloudGift()
        cloudGift.objectId = cloudObjectId
        cloudGift.put("clientId", entity.id)
        cloudGift.put("name", entity.name)
        cloudGift.put("amount", entity.amount)
        cloudGift.put("date", Date(entity.date))
        cloudGift.put("note", entity.note)
        cloudGift.put("direction", entity.direction)
        cloudGift.put("updatedAt", Date(entity.updatedAt))
        cloudGift.saveInBackground().subscribe(object : Observer<LCObject> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: LCObject) {
                callback(true, t.objectId)
            }
            override fun onError(e: Throwable) {
                callback(false, e.message)
            }
            override fun onComplete() {}
        })
    }

    /**
     * 删除记录
     */
    fun deleteGift(cloudObjectId: String, callback: (Boolean, String?) -> Unit) {
        val cloudGift = CloudGift()
        cloudGift.objectId = cloudObjectId
        cloudGift.deleteInBackground().subscribe(object : Observer<LCNull> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: LCNull) {
                callback(true, null)
            }
            override fun onError(e: Throwable) {
                callback(false, e.message)
            }
            override fun onComplete() {}
        })
    }

    /**
     * 上传图片
     */
    fun uploadImage(imagePath: String, callback: (String?, String?) -> Unit) {
        val file = LCFile("receipt.jpg", java.io.File(imagePath))
        file.saveInBackground().subscribe(object : Observer<LCFile> {
            override fun onSubscribe(d: Disposable) {}
            override fun onNext(t: LCFile) {
                callback(t.url, null)
            }
            override fun onError(e: Throwable) {
                callback(null, e.message)
            }
            override fun onComplete() {}
        })
    }
}
