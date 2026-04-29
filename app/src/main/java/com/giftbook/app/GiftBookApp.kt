package com.giftbook.app

import android.app.Application
import com.giftbook.app.auth.AuthManager
import com.giftbook.app.data.cloud.LeanCloudManager
import com.giftbook.app.data.db.AppDatabase
import com.giftbook.app.data.repository.GiftRepository
import com.giftbook.app.sync.SyncManager

/**
 * 应用 Application 类
 * 初始化 LeanCloud、数据库、全局管理器
 */
class GiftBookApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: GiftRepository
        private set
    lateinit var authManager: AuthManager
        private set
    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 初始化 LeanCloud（替换为你在 leancloud.cn 申请的凭据）
        LeanCloudManager.init(
            context = this,
            appId = LEANCLOUD_APP_ID,
            appKey = LEANCLOUD_APP_KEY,
            serverUrl = LEANCLOUD_SERVER_URL
        )

        // 初始化数据库
        database = AppDatabase.getInstance(this)

        // 初始化仓库
        repository = GiftRepository(database.giftDao())

        // 初始化认证管理器
        authManager = AuthManager(this)

        // 初始化同步管理器
        syncManager = SyncManager(this, database.giftDao())
        syncManager.startMonitoring()
    }

    override fun onTerminate() {
        super.onTerminate()
        syncManager.stopMonitoring()
    }

    companion object {
        // ==========================================================
        //  请替换为你在 LeanCloud 申请的正式凭据
        //  注册地址: https://leancloud.cn
        // ==========================================================
        private const val LEANCLOUD_APP_ID = "YOUR_LEANCLOUD_APP_ID"
        private const val LEANCLOUD_APP_KEY = "YOUR_LEANCLOUD_APP_KEY"
        // 国内节点: https://api.leancloud.cn
        // 国际节点: https://api-us.leancloud.cn
        private const val LEANCLOUD_SERVER_URL = "https://api.leancloud.cn"
    }
}
