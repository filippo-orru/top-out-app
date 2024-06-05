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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.filippoorru.topout.model.ClimbingState
import com.filippoorru.topout.model.RecordViewModel
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

    val lensFacing = remember { CameraSelector.LENS_FACING_BACK }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    val imageAnalyzers = remember { viewModel.getImageAnalyzers() }

    // Execute when closing the screen
    DisposableEffect(Unit) {
        onDispose {
            imageAnalyzers.forEach { it.clearAnalyzer() }
            viewModel.close()
            println("RecordScreen disposed")
        }
    }

    LaunchedEffect(lensFacing) {
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
            .requireLensFacing(lensFacing)
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
            *imageAnalyzers,
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212),
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
                                state.feet.forEach { (x, y) ->
                                    val center = Offset((1 - y.toFloat()) * size.width, x.toFloat() * size.height)
                                    // TODO function to transform landmark position into canvas position and back
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

                                state.feetTrackingPoints.forEach { point ->
                                    val center = Offset(((1 - point.y) * size.width).toFloat(), (point.x * size.height).toFloat())
                                    drawCircle(
                                        color = Color.White,
                                        center = center,
                                        radius = 7f
                                    )
                                    if (!point.isInMask) {
                                        drawCircle(
                                            color = Color.Red,
                                            center = center,
                                            radius = 5f
                                        )
                                    }
                                }
                            }
                        }

                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        viewModel.toString(),
                        Modifier.align(Alignment.BottomCenter),
                        color = Color.White
                    )
                }
            }
        }

        Box(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        "Idle",
                        color = Color.White
                    )
                }

                ClimbingState.Climbing -> {
                    Text(
                        "🔴 REC",
                        color = Color.White
                    )
                }

            }
        }
    }
}