package com.filippoorru.topout.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .addCameraFilter { cameraInfos ->
                val wideAngle = cameraInfos.firstOrNull { it.intrinsicZoomRatio < 1.0 }
                if (wideAngle != null) {
                    listOf(wideAngle)
                } else {
                    // If there is no wide angle camera, return all cameras
                    listOf(*cameraInfos.toTypedArray()) // Not sure why we need to create a new array
                }
            }
            .build()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            *viewModel.useCases,
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopOutAppBar(title = "TopOut") },
        contentColor = Color(0xFF121212),
    ) { padding ->
        Surface(
            modifier = Modifier.padding(padding),
            color = Color.Black,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        factory = { previewView }
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
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
                                state.feet.forEach { foot ->
                                    val (x, y) = foot
                                    val center = Offset((1 - y.toFloat()) * size.width, x.toFloat() * size.height)
                                    // TODO function to transform landmark position into canvas position and back

                                    if (!foot.isInMask) {
                                        val arcRadius = 10f
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
                                        color = Color.Green,
                                        center = center,
                                        radius = 7f
                                    )
                                }
                            }
                        }

                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Center,
                ) {
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

                IconButton(
                    onClick = {
                        when (recordingState) {
                            RecordingState.NotRecording -> viewModel.startRecording()
                            is RecordingState.Recording -> {
                                viewModel.stopRecording()
                                navController.navigate(Routes.View.build(viewModel.routeVisitId))
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

    }
}
