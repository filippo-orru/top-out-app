package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.filippoorru.topout.ui.Routes
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            if (cameraPermissionState.status.isGranted) {
                Button(
                    onClick = {
                        // Navigate to another screen
                        navController.navigate(Routes.Record.route)
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Start detection")
                }
            } else {
                Button(
                    onClick = {
                        cameraPermissionState.launchPermissionRequest()
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Request camera permission")
                }
            }
        }
    }
}

