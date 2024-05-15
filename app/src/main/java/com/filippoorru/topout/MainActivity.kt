package com.filippoorru.topout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filippoorru.topout.screens.DetectScreen
import com.filippoorru.topout.ui.theme.TopOutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TopOutTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.MainScreen.route) {
        composable(Screen.MainScreen.route) { MainScreen(navController) }
        composable(Screen.DetectScreen.route) { DetectScreen(navController) }
    }
}

sealed class Screen(val route: String) {
    data object MainScreen : Screen("main_screen")
    data object DetectScreen : Screen("detail_screen")
}