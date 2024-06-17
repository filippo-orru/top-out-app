package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.filippoorru.topout.database.Database

@Composable
fun CutScreen(navController: NavHostController, routeVisitId: String) {
    fun cutFile(path: String, newPath: String, startTimestamp: Int, endTimestamp: Int) {
        val session: FFmpegSession = FFmpegKit.execute("-i $path -ss $startTimestamp -to $endTimestamp -c copy $newPath")
        when (session.returnCode.value) {
            ReturnCode.SUCCESS -> println("Cutting successful")
            ReturnCode.CANCEL -> println("Cutting cancelled")
            else -> println("Cutting failed")
        }
    }

    val routeVisit by Database.i.routeVisits().get(routeVisitId.toInt()).collectAsState(initial = null)
    val visit = routeVisit

    if (visit == null) {
        Box {
            Text("Recording not found")
        }
    } else {

        val context = LocalContext.current

//        val exoPlayer = remember {
//            ExoPlayer.Builder(context).build().apply {
//                setMediaItem(MediaItem.fromUri(visit.recording?.filePath))
//                prepare()
//            }
//        }
//        val playbackState by exoPlayer.rememberPlaybackState()
//        val isPlaying = playbackState?.isPlaying ?: false
//
//        AndroidView(
//            factory = { context ->
//                PlayerView(context).apply {
//                    player = exoPlayer
//                }
//            },
//            modifier = Modifier.fillMaxSize()
//        )


        Box {
            Text("Cutting ${visit.id} ${visit.routeId}")
            //cutFile(file.path, file.path.substringBeforeLast("/") + "/new_${file.name}", 0, 50_000)
        }
    }
}