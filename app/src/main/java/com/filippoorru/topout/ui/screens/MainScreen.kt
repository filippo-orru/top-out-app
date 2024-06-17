package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.model.AppViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: AppViewModel = viewModel(AppViewModel::class),
) {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    val context = LocalContext.current

    val routeVisits by viewModel.routeVisits.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TopOut") })
        },

        ) { padding ->
        Box(
            Modifier.padding(padding),
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Route Visits", Modifier.padding(bottom = 8.dp), fontSize = 24.sp)

                val visits = routeVisits
                if (visits != null) {
                    for (routeVisit in visits) {
                        Button(onClick = {
                            navController.navigate(Routes.Cut.build(routeVisit.id))
                        }) {
                            Text(routeVisit.id)
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }

            if (cameraPermissionState.status.isGranted) {
                FloatingActionButton(
                    onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            navController.navigate(Routes.Record.route)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record video", tint = MaterialTheme.colorScheme.onPrimary)
                }

            }
        }
    }
}

