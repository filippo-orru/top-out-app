package com.filippoorru.topout.ui.screens

import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.filippoorru.topout.ui.Center
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.createVideoThumbnail
import com.filippoorru.topout.ui.icons.ClimberIcon
import com.filippoorru.topout.ui.model.AppViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        topBar = { TopOutAppBar(title = "TopOut") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        navController.navigate(Routes.Record.route)
//                        coroutineScope.launch {
//                            Database.i.routeVisits().save(
//                                RouteVisitEntity(
//                                    id = Random.nextLong().toString(),
//                                    recording = null,
////                                    routeId = "routeidtest",
//                                    timestamp = System.currentTimeMillis(),
//                                )
//                            )
//                        }
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
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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
                            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 64.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val simpleDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                        val grouped = visits
                            .sortedByDescending { it.timestamp }
                            .groupBy { simpleDateFormat.format(Date(it.timestamp)) }
                        for ((dayString, visitsOnDay) in grouped) {
                            Text(
                                dayString,
                                Modifier.padding(top = 16.dp, start = 8.dp, bottom = 8.dp),
                            )

                            for (routeVisit in visitsOnDay) {
                                Card(
                                    onClick = { navController.navigate(Routes.View.build(routeVisit.id)) },
                                    modifier = Modifier,
                                ) {
                                    Column(
                                        Modifier,
                                        horizontalAlignment = Alignment.Start,
                                    ) {

                                        Box(
                                            Modifier
                                                .height(196.dp)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            val thumbnail = remember {
                                                createVideoThumbnail(
                                                    File(routeVisit.recording.filePath),
                                                    Size(512, 512),
                                                    desiredTimestamp = null
                                                )?.asImageBitmap()
                                            }

                                            Box(
                                                Modifier.fillMaxSize(),
                                            ) {
                                                if (thumbnail != null) {
                                                    Image(
                                                        thumbnail,
                                                        contentDescription = "Thumbnail",
                                                        Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Center {
                                                        Text(
                                                            "Thumbnail not available",
                                                            Modifier.fillMaxSize(),
                                                            style = MaterialTheme.typography.labelMedium,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ClimberIcon(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))

                                        Text(
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(routeVisit.timestamp)),
                                            Modifier,
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
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopOutAppBar(
    navigateBack: (() -> Unit)? = null,
    title: String = "TopOut",
) {
    TopAppBar(
        title = { Text(title) },
        colors = topAppBarColors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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

