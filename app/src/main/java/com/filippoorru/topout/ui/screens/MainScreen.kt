package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.database.RouteVisitEntity
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.model.AppViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalPermissionsApi::class)
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
    val coroutineScope = rememberCoroutineScope()

    val routeVisits by viewModel.routeVisits.collectAsState()

    Scaffold(
        topBar = { TopOutAppBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        //navController.navigate(Routes.Record.route)
                        coroutineScope.launch {
                            Database.i.routeVisits().save(
                                RouteVisitEntity(
                                    id = Random.nextLong().toString(),
                                    recording = null,
//                                    routeId = "routeidtest",
                                    timestamp = System.currentTimeMillis(),
                                )
                            )
                        }
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.padding(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record video", tint = MaterialTheme.colorScheme.primary)
            }
        }
    ) { padding ->
        Box(
            Modifier.padding(padding),
        ) {
            val visits = routeVisits
            if (visits == null) {
                CircularProgressIndicator()

            } else {
                if (visits.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    ) {
                        Text("No recordings found 🤷")
                        Button(
                            onClick = { navController.navigate(Routes.Record.route) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create a new recording")
                                Text("Create a new recording")
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {

                        for (routeVisit in visits) {
                            Card(
                                onClick = { navController.navigate(Routes.Cut.build(routeVisit.id)) },
                                modifier = Modifier,
                            ) {
                                Column(
                                    Modifier,
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .height(128.dp)
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        // Thumbnail here
                                    }

                                    Text(
                                        routeVisit.id,
                                        Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopOutAppBar(
    navigateBack: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text("TopOut") },
        colors = topAppBarColors().copy(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            if (navigateBack != null) {
                IconButton(onClick = {
                    navigateBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
    )
}

