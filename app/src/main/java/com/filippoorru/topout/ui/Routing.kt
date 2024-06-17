package com.filippoorru.topout.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filippoorru.topout.ui.screens.CutScreen
import com.filippoorru.topout.ui.screens.MainScreen
import com.filippoorru.topout.ui.screens.RecordScreen

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val duration = 250
    NavHost(
        navController = navController,
        startDestination = Routes.Main.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(duration, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(duration))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(duration, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(duration))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(duration, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(duration))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(duration, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(duration))
        }
    ) {
        composable(Routes.Main.route) { MainScreen(navController) }
        composable(Routes.Record.route) { RecordScreen(navController) }
        composable(Routes.Cut.route) { backStackEntry ->
            val routeVisitId = backStackEntry.arguments?.getString("routeVisitId")!!
            CutScreen(navController, routeVisitId)
        }
    }
}

sealed class Routes(val route: String) {
    data object Main : Routes("main")
    data object Record : Routes("record")
    data object Cut : Routes("cut/{routeVisitId}") {
        fun build(routeVisitId: String): String {
            return "cut/$routeVisitId"
        }
    }
}