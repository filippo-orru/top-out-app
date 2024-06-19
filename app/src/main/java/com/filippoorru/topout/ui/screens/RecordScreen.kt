package com.filippoorru.topout.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.filippoorru.topout.ui.Routes
import com.filippoorru.topout.ui.icons.RecordIcon
import com.filippoorru.topout.ui.icons.RecordStopIcon
import com.filippoorru.topout.ui.model.ClimbingState
import com.filippoorru.topout.ui.model.RecordViewModel
import com.filippoorru.topout.ui.model.RecordingState
import com.filippoorru.topout.utils.getCameraProvider
import com.filippoorru.topout.utils.zero


@Composable
fun RecordScreen(navController: NavController) {
    val context = LocalContext.current

    val viewModel = remember {
        RecordViewModel(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val poseState by viewModel.poseState.collectAsStateWithLifecycle(lifecycleOwner)
    val segmentationState by viewModel.segmentationState.collectAsStateWithLifecycle(lifecycleOwner)
    val climbingState by viewModel.climbingState.collectAsStateWithLifecycle(lifecycleOwner)
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle(lifecycleOwner)

    val previewView = remember {
        PreviewView(context).apply {
//            layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                0
//            )
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    // Execute when closing the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRecording()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = getCameraProvider(context)
        cameraProvider.unbindAll()
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setResolutionSelector(
//                ResolutionSelector.Builder()
//                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
//                    .build()
//            )
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//            .addCameraFilter { cameraInfos ->
//                val wideAngle = cameraInfos.firstOrNull { it.intrinsicZoomRatio < 1.0 }
//                if (wideAngle != null) {
//                    listOf(wideAngle)
//                } else {
//                    // If there is no wide angle camera, return all cameras
//                    listOf(*cameraInfos.toTypedArray()) // Not sure why we need to create a new array
//                }
//            }
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(previewView.viewPort!!)
            .addUseCase(preview)

        for (useCase in viewModel.useCases) {
            useCaseGroup.addUseCase(useCase)
        }

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            useCaseGroup.build()
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentColor = Color.White,
        containerColor = Color.Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .matchParentSize(),
                    color = Color.Black.copy(alpha = 0.15f),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val height = size.height
                        val width = size.width

                        segmentationState?.let { state ->
                            val byteArray = state.mask
                            val pixels = IntArray(byteArray.size)
                            for (i in pixels.indices) {
                                // TODO can this be optimized?
                                val byte = byteArray[i]
                                val color: Int =
                                    if (byte != zero) android.graphics.Color.TRANSPARENT else android.graphics.Color.RED
                                pixels[i] = color
                            }

                            val imgWidth = state.width
                            val imgHeight = state.height

                            drawImage(
                                Bitmap.createScaledBitmap(
                                    Bitmap.createBitmap(
                                        Bitmap.createBitmap(pixels, imgWidth, imgHeight, Bitmap.Config.ARGB_8888),
                                        0, 0, imgWidth, imgHeight, Matrix().apply { postRotate(90f) }, true
                                    ),
                                    width.toInt(), height.toInt(), true
                                ).asImageBitmap(),
                                alpha = 0.33f,
                            )

                            // Draw the segmentation points
                            viewModel.segmentationPoints.forEach { (x, y) ->
                                drawCircle(
                                    color = Color.White,
                                    center = Offset(x * width, y * height),
                                    radius = 7f
                                )
                                drawCircle(
                                    color = Color.Blue,
                                    center = Offset(x * width, y * height),
                                    radius = 5f
                                )
                            }
                        }

                        poseState?.let { state ->
                            state.feetTrackingPoints.forEach { point ->
                                val (x, y) = point
                                val center = Offset((1 - y.toFloat()) * size.width, x.toFloat() * size.height)
                                // TODO function to transform landmark position into canvas position and back

                                if (point.isInMask) {
                                    val arcRadius = 15f
                                    drawArc(
                                        color = Color.White,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = true,
                                        topLeft = center - Offset(arcRadius, arcRadius),
                                        size = Size(2 * arcRadius, 2 * arcRadius)
                                    )
                                }

                                drawCircle(
                                    color = Color.White,
                                    center = center,
                                    radius = 10f
                                )
                                drawCircle(
                                    color = Color.Blue,
                                    center = center,
                                    radius = 7f
                                )
                            }

                            state.feet.forEach { foot ->
                                val (x, y) = foot
                                val center = Offset((1 - y.toFloat()) * size.width, x.toFloat() * size.height)
                                // TODO function to transform landmark position into canvas position and back

//                                    if (!foot.isInMask) {
//                                        val arcRadius = 10f
//                                        drawArc(
//                                            color = Color.White,
//                                            startAngle = 0f,
//                                            sweepAngle = 360f,
//                                            useCenter = true,
//                                            topLeft = center - Offset(arcRadius, arcRadius),
//                                            size = Size(2 * arcRadius, 2 * arcRadius)
//                                        )
//                                    }

                                drawCircle(
                                    color = Color.White,
                                    center = center,
                                    radius = 10f
                                )
                                drawCircle(
                                    color = Color.Green,
                                    center = center,
                                    radius = 7f
                                )
                            }
                        }
                    }

                }
            }

            Box(Modifier.padding(16.dp)) {
                when (climbingState) {
                    ClimbingState.NotDetected -> {
                        Text(
                            "...",
                            color = Color.White
                        )
                    }

                    ClimbingState.Idle -> {
                        Text(
                            "...",
                            color = Color.White
                        )
                    }

                    ClimbingState.Climbing -> {
                        Text(
                            "🔴 Climbing 🔴",
                            color = Color.White
                        )
                    }

                }
            }

            Box(Modifier.padding(bottom = 64.dp)) {
                IconButton(
                    onClick = {
                        when (recordingState) {
                            RecordingState.NotRecording -> viewModel.startRecording()
                            is RecordingState.Recording -> {
                                viewModel.stopRecording()
                                navController.navigate(
                                    Routes.ViewRouteVisit.build(viewModel.routeVisitId),
                                    NavOptions.Builder().setPopUpTo(Routes.Main.route, inclusive = false).build()
                                )
                            }
                        }
                    },
                    Modifier
                        .scale(1.75f)
                        .padding(16.dp)
                ) {
                    when (recordingState) {
                        RecordingState.NotRecording -> RecordIcon()
                        is RecordingState.Recording -> RecordStopIcon()
                    }
                }
            }
        }

        IconButton(
            onClick = { navController.popBackStack() },
            Modifier.absoluteOffset(x = 16.dp, y = 16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}
