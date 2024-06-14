package com.filippoorru.topout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.filippoorru.topout.utils.getAllVideos

@Composable
fun CutScreen(navController: NavHostController) {
    fun cutFile(path: String, newPath: String, start: Int, end: Int) {
        val session: FFmpegSession = FFmpegKit.execute("-i $path -ss 1234 -to 4567 -c copy $newPath")
        when (session.returnCode.value) {
            ReturnCode.SUCCESS -> println("Cutting successful")
            ReturnCode.CANCEL -> println("Cutting cancelled")
            else -> println("Cutting failed")
        }
    }

    val context = LocalContext.current

    val files = remember {
        getAllVideos(context)
    }

    Box {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Files", Modifier.padding(bottom = 8.dp), fontSize = 24.sp)

            for (file in files ?: emptyList()) {
                Button(onClick = { cutFile(file.name, "new_${file.name}", 0, 2000) }) {
                    Text(file.name)
                }
            }
        }
    }
}