package com.filippoorru.topout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.filippoorru.topout.ui.AppNavigator
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

