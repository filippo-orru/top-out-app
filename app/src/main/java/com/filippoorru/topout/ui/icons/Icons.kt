package com.filippoorru.topout.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.filippoorru.topout.R

@Composable
fun RecordIcon() {
    Icon(
        painterResource(id = R.drawable.record),
        contentDescription = "Start recording",
        tint = Color.White,
    )
}

@Composable
fun RecordStopIcon() {
    Icon(
        painterResource(id = R.drawable.record_stop),
        contentDescription = "Stop recording",
        tint = Color.White,
    )
}

@Composable
fun ClimberIcon(color: Color) {
    Icon(
        painterResource(id = R.drawable.climber),
        contentDescription = "Climber",
        tint = color,
    )
}