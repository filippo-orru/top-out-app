package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.filippoorru.topout.ui.Center
import com.filippoorru.topout.ui.model.CutScreenModel
import java.io.File

@Composable
fun CutScreen(navController: NavHostController, routeVisitId: String, attemptId: String) {
    fun cutFile(path: String, newPath: String, startTimestamp: Int, endTimestamp: Int) {
        val session: FFmpegSession = FFmpegKit.execute("-y -i $path -ss $startTimestamp -to $endTimestamp -c copy $newPath")
        when (session.returnCode.value) {
            ReturnCode.SUCCESS -> println("Cutting successful")
            ReturnCode.CANCEL -> println("Cutting cancelled")
            else -> println("Cutting failed")
        }
    }

    val viewModel = remember {
        CutScreenModel(routeVisitId, attemptId)
    }

    val routeVisit by viewModel.routeVisit.collectAsState(initial = null)
    val attempt by viewModel.attempt.collectAsState(initial = null)

    val visit = routeVisit
    val recording = visit?.recording
    val fileExists: Boolean = remember(recording?.filePath) { recording?.filePath?.let { File(it).exists() } == true }

    val cutStart: MutableState<Long?> =
        remember(attempt?.partOfRouteVisitRecording?.startMs) { mutableStateOf(attempt?.partOfRouteVisitRecording?.startMs) }
    val cutEnd: MutableState<Long?> =
        remember(attempt?.partOfRouteVisitRecording?.endMs) { mutableStateOf(attempt?.partOfRouteVisitRecording?.endMs) }

    Scaffold(
        topBar = {
            TopOutAppBar(
                navigateBack = { navController.popBackStack() },
                title = "Edit Attempt",
            )
        },
    ) { padding ->
        Box(
            Modifier.padding(padding),
        ) {
            if (visit == null) {
                Center {
                    Text("Visit not found")
                }

            } else if (!fileExists) {
                Center {
                    Text("Recorded video file not found")
                }

            } else {
                recording!! // Safe to unwrap here because fileExists

                val context = LocalContext.current

                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(recording.filePath))
                        prepare()
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                // TODO remove player controls
                val playerView = remember {
                    PlayerView(context).apply {
                        player = exoPlayer
                    }
                }

                Column(
                    Modifier.fillMaxSize(),
                ) {
                    Text("Cutting ${visit.id}", Modifier.padding(16.dp))

                    AndroidView(
                        factory = {
                            playerView
                        },
                        modifier = Modifier
                            .background(Color.Black)
                            .fillMaxWidth()
                            .fillMaxHeight(0.55f) // TODO take all available space?
                    )

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // TODO seek bar
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (cutStart.value != null) {
                                Text("Cut Start: ${cutStart.value}")
                            } else {
                                Box {}
                            }
                            if (cutEnd.value != null) {
                                Text("Cut End: ${cutEnd.value}")
                            } else {
                                Box {}
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Button(onClick = {
                                cutStart.value = exoPlayer.currentPosition
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                Text("Cut Start")
                            }

                            Button(onClick = {
                                cutEnd.value = exoPlayer.currentPosition
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                                Text("Cut End")
                            }
                        }

                        Column(
                            Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {

                            // divider
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))

                            // cancel & save buttons
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(onClick = {
                                    navController.popBackStack()
                                }) {
                                    Text("Cancel")
                                }

                                Button(onClick = {
                                    cutFile(recording.filePath, recording.filePath, 0, 1000)
                                }) {
                                    Text("Save")
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}