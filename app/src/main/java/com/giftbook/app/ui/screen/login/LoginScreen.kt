package com.giftbook.app.ui.screen.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wechat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftbook.app.GiftBookApp
import com.giftbook.app.auth.AuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as GiftBookApp
    val authManager = app.authManager
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=微信, 1=手机

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App 标题
        Text(
            text = "人情往来",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "记录每一份人情，传递每一份心意",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 登录方式切换
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("微信登录") },
                icon = { Icon(Icons.Default.Wechat, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("手机号登录") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (selectedTab) {
            0 -> {
                // 微信登录
                WechatLoginSection(
                    authManager = authManager,
                    isLoading = isLoading,
                    onLoadingChange = { isLoading = it },
                    onError = { errorMessage = it },
                    onSuccess = onLoginSuccess
                )
            }
            1 -> {
                // 手机号登录
                PhoneLoginSection(
                    phone = phone,
                    onPhoneChange = { phone = it },
                    smsCode = smsCode,
                    onSmsCodeChange = { smsCode = it },
                    countdown = countdown,
                    onCountdownChange = { countdown = it },
                    isLoading = isLoading,
                    onLoadingChange = { isLoading = it },
                    onError = { errorMessage = it },
                    onSuccess = onLoginSuccess,
                    authManager = authManager,
                    scope = scope
                )
            }
        }

        // 错误提示
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun WechatLoginSection(
    authManager: AuthManager,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    Button(
        onClick = {
            onLoadingChange(true)
            onError(null)
            // 微信登录流程：
            // 1. 调起微信 SDK 获取 code
            // 2. 通过后端换取 access_token + openid
            // 3. 调用 authManager.loginWithWechat()
            // 注意：需要先在微信开放平台注册应用
            onError("请先配置微信开放平台 AppID")
            onLoadingChange(false)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Wechat, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("微信一键登录", fontSize = 16.sp)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "点击即表示同意《用户协议》和《隐私政策》",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneLoginSection(
    phone: String,
    onPhoneChange: (String) -> Unit,
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    countdown: Int,
    onCountdownChange: (Int) -> Unit,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: () -> Unit,
    authManager: AuthManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    // 手机号输入
    OutlinedTextField(
        value = phone,
        onValueChange = { if (it.length <= 11) onPhoneChange(it) },
        label = { Text("手机号") },
        placeholder = { Text("请输入手机号") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 验证码输入 + 获取按钮
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = smsCode,
            onValueChange = { if (it.length <= 6) onSmsCodeChange(it) },
            label = { Text("验证码") },
            placeholder = { Text("请输入6位验证码") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        Button(
            onClick = {
                if (phone.length != 11) {
                    onError("请输入正确的手机号")
                    return@Button
                }
                onLoadingChange(true)
                onError(null)
                authManager.sendSmsCode(phone) { success, error ->
                    onLoadingChange(false)
                    if (success) {
                        onCountdownChange(60)
                        onError(null)
                    } else {
                        onError(error ?: "发送失败")
                    }
                }
            },
            enabled = !isLoading && countdown == 0,
            modifier = Modifier.height(56.dp)
        ) {
            if (countdown > 0) {
                Text("重新发送 ($countdown)")
            } else {
                Text("获取验证码")
            }
        }
    }

    // 倒计时
    if (countdown > 0) {
        LaunchedEffect(countdown) {
            delay(1000L)
            onCountdownChange(countdown - 1)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 登录按钮
    Button(
        onClick = {
            if (phone.length != 11) {
                onError("请输入正确的手机号")
                return@Button
            }
            if (smsCode.length < 4) {
                onError("请输入验证码")
                return@Button
            }
            onLoadingChange(true)
            onError(null)
            authManager.loginWithPhone(phone, smsCode) { success, error ->
                onLoadingChange(false)
                if (success) {
                    onSuccess()
                } else {
                    onError(error ?: "登录失败")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !isLoading && phone.length == 11 && smsCode.length >= 4
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("登录 / 注册", fontSize = 16.sp)
        }
    }
}
