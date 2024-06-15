package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.utils.getAllVideos
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    val context = LocalContext.current

    val files = remember {
        getAllVideos(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cameraPermissionState.status.isGranted) {
                    FloatingActionButton(
                        onClick = {
                            // Navigate to another screen
                            navController.navigate(Routes.Record.route)
                        },
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Record video", tint = MaterialTheme.colorScheme.onPrimary)
                    }

                } else {
                    Text("You need to grant camera permission to use this app", modifier = Modifier.padding(16.dp))

                    Button(
                        onClick = {
                            cameraPermissionState.launchPermissionRequest()
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Request camera permission")
                    }
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Files", Modifier.padding(bottom = 8.dp), fontSize = 24.sp)

                    for (file in files ?: emptyList()) {
                        Button(onClick = {
                            navController.navigate(Routes.Cut.route)
                        }) {
                            Text(file.name)
                        }
                    }
                }
                Button(
                    onClick = { },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Cut videos")
                }
            }
        }
    }
}

