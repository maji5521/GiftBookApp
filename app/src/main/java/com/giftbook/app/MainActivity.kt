package com.giftbook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.giftbook.app.auth.AuthManager
import com.giftbook.app.ui.nav.AppNavigation
import com.giftbook.app.ui.nav.Routes
import com.giftbook.app.ui.theme.GiftBookTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as GiftBookApp

        setContent {
            GiftBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 检查登录状态决定起始页面
                    val isLoggedIn = remember {
                        app.authManager.isLoggedIn
                    }

                    val startDestination = if (isLoggedIn) Routes.HOME else Routes.LOGIN

                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
