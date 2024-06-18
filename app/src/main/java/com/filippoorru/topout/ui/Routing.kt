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
import com.filippoorru.topout.ui.screens.ViewAttemptScreen
import com.filippoorru.topout.ui.screens.ViewRouteVisitScreen

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val fadeDuration = 250
    val slideDuration = 300

    NavHost(
        navController = navController,
        startDestination = Routes.Main.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(slideDuration, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(fadeDuration))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(slideDuration, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(fadeDuration))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(slideDuration, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(fadeDuration))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(slideDuration, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(fadeDuration))
        }
    ) {
        composable(Routes.Main.route) { MainScreen(navController) }
        composable(Routes.Record.route) { RecordScreen(navController) }
        composable(Routes.ViewRouteVisit.route) { backStackEntry ->
            val routeVisitId = backStackEntry.arguments?.getString("routeVisitId")!!
            ViewRouteVisitScreen(navController, routeVisitId)
        }
        composable(Routes.Cut.route) { backStackEntry ->
            val routeVisitId = backStackEntry.arguments?.getString("routeVisitId")!!
            val attemptId = backStackEntry.arguments?.getString("attemptId")!!
            CutScreen(navController, routeVisitId, attemptId)
        }
        composable(Routes.ViewAttempt.route) { backStackEntry ->
            val routeVisitId = backStackEntry.arguments?.getString("routeVisitId")!!
            val attemptId = backStackEntry.arguments?.getString("attemptId")!!
            ViewAttemptScreen(navController, routeVisitId, attemptId)
        }
    }
}

sealed class Routes(val route: String) {
    data object Main : Routes("main")

    data object Record : Routes("record")

    data object ViewRouteVisit : Routes("{routeVisitId}/view") {
        fun build(routeVisitId: String): String {
            return "$routeVisitId/view"
        }
    }

    data object Cut : Routes("{routeVisitId}/{attemptId}/cut") {
        fun build(routeVisitId: String, attemptId: String): String {
            return "$routeVisitId/$attemptId/cut"
        }
    }

    data object ViewAttempt : Routes("{routeVisitId}/{attemptId}/view") {
        fun build(routeVisitId: String, attemptId: String): String {
            return "$routeVisitId/$attemptId/view"
        }
    }
}