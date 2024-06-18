package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.ui.Center
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

    val routeVisit by Database.i.routeVisits().get(routeVisitId).collectAsState(initial = null)
    val visit = routeVisit
    val recording = visit?.recording
    val fileExists: Boolean = remember(recording?.filePath) { recording?.filePath?.let { File(it).exists() } == true }

    Scaffold(
        topBar = {
            TopOutAppBar(
                navigateBack = { navController.popBackStack() },
            )
        },
        floatingActionButton = {
            if (recording != null) {
                FloatingActionButton(
                    onClick = {
                        // Save
                        cutFile(
                            recording.filePath,
                            // TODO proper path building
                            recording.filePath.substringBeforeLast("/") + "/new_${recording.filePath.substringAfterLast("/")}",
                            0,
                            5_000
                        )
                    },
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
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
                            .fillMaxHeight(0.4f)
                    )

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Player seek & cut bar
                        // divider
                        // cancel & save buttons
                    }
                }
            }
        }
    }
}