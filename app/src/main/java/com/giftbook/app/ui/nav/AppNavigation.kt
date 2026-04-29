package com.giftbook.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.giftbook.app.ui.screen.add.AddScreen
import com.giftbook.app.ui.screen.edit.EditScreen
import com.giftbook.app.ui.screen.home.HomeScreen
import com.giftbook.app.ui.screen.login.LoginScreen
import com.giftbook.app.ui.screen.person.PersonDetailScreen
import com.giftbook.app.ui.screen.scan.ScanScreen

/**
 * 导航路由定义
 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{giftId}"
    const val SCAN = "scan"
    const val PERSON = "person/{name}"

    fun editRoute(giftId: String) = "edit/$giftId"
    fun personRoute(name: String) = "person/$name"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToAdd = { navController.navigate(Routes.ADD) },
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToPerson = { name ->
                    navController.navigate(Routes.personRoute(name))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADD) {
            AddScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("giftId") { type = NavType.StringType })
        ) { backStackEntry ->
            val giftId = backStackEntry.arguments?.getString("giftId") ?: ""
            EditScreen(
                giftId = giftId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PERSON,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            PersonDetailScreen(
                name = name,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { giftId ->
                    navController.navigate(Routes.editRoute(giftId))
                }
            )
        }
    }
}
