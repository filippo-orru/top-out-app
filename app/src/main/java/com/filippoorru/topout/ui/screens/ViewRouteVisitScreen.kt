package com.filippoorru.topout.ui.screens


import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.filippoorru.topout.ui.Center
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.createVideoThumbnail
import com.filippoorru.topout.ui.model.ViewRouteVisitModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ViewRouteVisitScreen(navController: NavHostController, routeVisitId: String) {
    val viewModel = remember {
        ViewRouteVisitModel(routeVisitId)
    }
    val routeVisit by viewModel.routeVisits.collectAsState(initial = null)
    val attempts by viewModel.attempts.collectAsState(initial = emptyList())

    val title =
        remember(routeVisit?.timestamp) {
            routeVisit?.timestamp?.let { SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(it) } ?: ""
        }
    val visit = routeVisit

    Scaffold(
        topBar = {
            TopOutAppBar(
                navigateBack = { navController.popBackStack() },
                title = title,
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
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (attempts.isEmpty()) {
                        Text("No attempts detected. You can manually select them from the recording.")
                    }

                    attempts.forEachIndexed { index, attempt ->
                        Card(
                            onClick = {
                                navController.navigate(Routes.Cut.build(routeVisitId, attempt.id))
                            },
                            Modifier
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth(),
                            ) {
                                val thumbnail = remember {
                                    createVideoThumbnail(
                                        File(visit.recording.filePath),
                                        Size(512, 512),
                                        desiredTimestamp = attempt.partOfRouteVisitRecording?.startMs?.plus(1000),
                                    )?.asImageBitmap()
                                }

                                Box(
                                    Modifier
                                        .aspectRatio(1f)
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.33f)),
                                ) {
                                    if (thumbnail != null) {
                                        Image(
                                            thumbnail,
                                            contentDescription = "Thumbnail",
                                            Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.align(Alignment.Center))
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

                                Text(
                                    "Attempt ${index + 1}",
                                    Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val attemptId = viewModel.createNewAttempt(index = attempts.size)
                            navController.navigate(Routes.Cut.build(routeVisitId, attemptId))
                        },
                        Modifier
                            .align(Alignment.CenterHorizontally),
                    ) {
                        Row(
                            Modifier
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, Modifier)
                            Text("New attempt")
                        }
                    }
                }
            }
        }
    }
}