package com.giftbook.app.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import cn.leancloud.LCUser
import cn.leancloud.sms.LCSMS
import cn.leancloud.sms.LCSMSOption
import cn.leancloud.types.LCNull
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * 认证管理器
 * 管理登录状态、用户信息持久化
 */
class AuthManager(private val context: Context) {

    /** 当前用户是否已登录 */
    val isLoggedIn: Boolean
        get() = LCUser.currentUser() != null

    /** 当前用户 ID */
    val currentUserId: String?
        get() = LCUser.currentUser()?.objectId

    /** 当前用户名（手机号或微信昵称） */
    val currentUserName: String?
        get() = LCUser.currentUser()?.username

    /** 登录状态 Flow */
    val loginStateFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LOGIN_STATE_KEY] ?: false
    }

    // ==================== 手机号登录 ====================

    /**
     * 发送短信验证码
     */
    fun sendSmsCode(phone: String, callback: (Boolean, String?) -> Unit) {
        val options = LCSMSOption()
        options.setTemplateName("LoginTemplate")
        options.setSignatureName("人情往来")
        LCSMS.requestSMSCodeInBackground(phone, options)
            .subscribe(object : Observer<LCNull> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(t: LCNull) {
                    callback(true, null)
                }
                override fun onError(e: Throwable) {
                    callback(false, e.localizedMessage ?: "发送失败")
                }
                override fun onComplete() {}
            })
    }

    /**
     * 手机号 + 验证码登录
     * 手机号不存在则自动注册
     */
    fun loginWithPhone(phone: String, code: String, callback: (Boolean, String?) -> Unit) {
        LCUser.signUpOrLoginByMobilePhoneInBackground(phone, code)
            .subscribe(object : Observer<LCUser> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(user: LCUser) {
                    saveLoginState(true)
                    callback(true, null)
                }
                override fun onError(e: Throwable) {
                    callback(false, e.localizedMessage ?: "登录失败")
                }
                override fun onComplete() {}
            })
    }

    // ==================== 微信登录 ====================

    /**
     * 微信登录
     * @param authData 从微信 SDK 获取的 {access_token, openid}
     */
    fun loginWithWechat(authData: Map<String, Any>, unionId: String, callback: (Boolean, String?) -> Unit) {
        LCUser.loginWithAuthData(authData, "weixin", unionId, "", true)
            .subscribe(object : Observer<LCUser> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(user: LCUser) {
                    saveLoginState(true)
                    callback(true, null)
                }
                override fun onError(e: Throwable) {
                    callback(false, e.localizedMessage ?: "微信登录失败")
                }
                override fun onComplete() {}
            })
    }

    /**
     * 构建微信登录的 authData
     * 在微信 SDK 回调中获取 code 后，通过后端换取 access_token 和 openid
     */
    fun buildWechatAuthData(accessToken: String, openId: String): Map<String, Any> {
        return mapOf(
            "access_token" to accessToken,
            "openid" to openId
        )
    }

    // ==================== 登出 ====================

    /**
     * 退出登录
     */
    fun logout(callback: () -> Unit = {}) {
        LCUser.logOut()
        saveLoginState(false)
        callback()
    }

    // ==================== 状态持久化 ====================

    private fun saveLoginState(isLoggedIn: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[LOGIN_STATE_KEY] = isLoggedIn
            }
        }
    }

    companion object {
        private val LOGIN_STATE_KEY = booleanPreferencesKey("is_logged_in")
    }
}
