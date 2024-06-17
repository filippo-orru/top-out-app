package com.filippoorru.topout.ui.screens

import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.filippoorru.topout.ui.Routes
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
        topBar = { TopOutAppBar() },
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
                        val grouped = visits.groupBy { simpleDateFormat.format(Date(it.timestamp)) }
                        for ((dayString, visitsOnDay) in grouped) {
                            Text(
                                dayString,
                                Modifier.padding(top = 16.dp, start = 8.dp, bottom = 8.dp),
                            )

                            for (routeVisit in visitsOnDay) {
                                Card(
                                    onClick = { navController.navigate(Routes.Cut.build(routeVisit.id)) },
                                    modifier = Modifier,
                                ) {
                                    Column(
                                        Modifier,
                                        horizontalAlignment = Alignment.Start,
                                    ) {
                                        Box(
                                            Modifier
                                                .height(128.dp)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            @Suppress("DEPRECATION")
                                            val thumbnail = remember {
                                                val file = File(routeVisit.recording.filePath)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    context.contentResolver.loadThumbnail(Uri.fromFile(file), Size(512, 384), null)
                                                } else {
                                                    ThumbnailUtils.createVideoThumbnail(
                                                        file.absolutePath,
                                                        MediaStore.Images.Thumbnails.MINI_KIND
                                                    )
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
                                            ClimberIcon(Color.Black.copy(alpha = 0.7f))

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

