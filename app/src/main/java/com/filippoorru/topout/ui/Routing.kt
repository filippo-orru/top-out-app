package com.filippoorru.topout.ui

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
    NavHost(navController = navController, startDestination = Routes.Main.route) {
        composable(Routes.Main.route) { MainScreen(navController) }
        composable(Routes.Record.route) { RecordScreen(navController) }
        composable(Routes.Cut.route) { CutScreen(navController) }
    }
}

sealed class Routes(val route: String) {
    data object Main : Routes("main")
    data object Record : Routes("record")
    data object Cut : Routes("cut")
}