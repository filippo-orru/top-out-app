package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode

@Composable
fun CutScreen(navController: NavHostController, recordingId: String) {
    fun cutFile(path: String, newPath: String, startTimestamp: Int, endTimestamp: Int) {
        val session: FFmpegSession = FFmpegKit.execute("-i $path -ss $startTimestamp -to $endTimestamp -c copy $newPath")
        when (session.returnCode.value) {
            ReturnCode.SUCCESS -> println("Cutting successful")
            ReturnCode.CANCEL -> println("Cutting cancelled")
            else -> println("Cutting failed")
        }
    }


    Box {
        cutFile(file.path, file.path.substringBeforeLast("/") + "/new_${file.name}", 0, 50_000)
    }
}