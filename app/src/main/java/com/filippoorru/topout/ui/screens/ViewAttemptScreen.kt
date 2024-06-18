package com.filippoorru.topout.ui.screens

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.filippoorru.topout.ui.Center
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.model.ViewAttemptModel
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun ViewAttemptScreen(navController: NavHostController, routeVisitId: String, attemptId: String) {
    val viewModel = remember {
        ViewAttemptModel(routeVisitId, attemptId)
    }

    val routeVisit by viewModel.routeVisit.collectAsState(initial = null)
    val attempt by viewModel.attempt.collectAsState(initial = null)

    val visit = routeVisit
    val recording = visit?.recording
    val fileExists: Boolean = remember(recording?.filePath) { recording?.filePath?.let { File(it).exists() } == true }

    val showControls = remember { mutableStateOf(true) }

    @Composable
    fun DynamicControls(content: @Composable () -> Unit) {
        AnimatedVisibility(
            visible = showControls.value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            content()
        }
    }

    Scaffold(
        contentColor = Color.White,
        containerColor = Color.Black,
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

                // TODO only play attempt time range
                val playerView = remember {
                    PlayerView(context).apply {
                        setShowSubtitleButton(false)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setShowFastForwardButton(false)
                        setShowRewindButton(false)
                        controllerShowTimeoutMs = 1000
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                            showControls.value = visibility == PlayerControlView.VISIBLE
                        })

                        player = exoPlayer
                    }
                }

                AndroidView(
                    factory = {
                        playerView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                )

                DynamicControls {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = 96.dp),
                        verticalArrangement = Arrangement.Bottom,
                    ) {


                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            TextButton(onClick = {
                                navController.navigate(Routes.Cut.build(routeVisitId, attemptId))
                            }) {
                                Column(
                                    horizontalAlignment = CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                                    Text("Edit", color = Color.White)
                                }
                            }

                            TextButton(onClick = {
                                viewModel.delete()
                                navController.popBackStack()
                            }) {
                                Column(
                                    horizontalAlignment = CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White)
                                    Text("Delete", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        DynamicControls {
            IconButton(
                onClick = { navController.popBackStack() },
                Modifier.absoluteOffset(x = 16.dp, y = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    }
}