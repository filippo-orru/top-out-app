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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.createVideoThumbnail
import com.filippoorru.topout.ui.icons.ClimberIcon
import com.filippoorru.topout.ui.model.MainScreenViewModel
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
    viewModel: MainScreenViewModel = viewModel(MainScreenViewModel::class),
) {
    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }


    Scaffold(
        topBar = { TopOutAppBar(title = "TopOut") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        navController.navigate(Routes.Record.route)
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
            val routeVisits by viewModel.routeVisits.collectAsState()
            val visits = routeVisits

            val attempts by viewModel.attempts.collectAsState()

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
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 64.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        for ((dayString, visitsOnDay) in visits) {
                            if (visits.size > 1) {
                                Text(
                                    dayString,
                                    Modifier.padding(top = 16.dp, start = 8.dp, bottom = 8.dp),
                                )
                            }

                            for (routeVisit in visitsOnDay) {
                                Card(
                                    onClick = { navController.navigate(Routes.ViewRouteVisit.build(routeVisit.id)) },
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
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
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
                                                    Surface(
                                                        Modifier
                                                            .width(64.dp)
                                                            .height(64.dp)
                                                            .align(Alignment.Center),
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.33f),
                                                    ) {}
                                                    Text(
                                                        attempts.count { it.routeVisitId == routeVisit.id }.toString(),
                                                        Modifier
                                                            .align(Alignment.Center)
                                                            .scale(1.3f),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                    )
                                                } else {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            "Thumbnail not available",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onPrimary,
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

