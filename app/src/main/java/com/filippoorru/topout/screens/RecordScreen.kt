package com.filippoorru.topout.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.filippoorru.topout.model.RecordState
import com.filippoorru.topout.model.RecordViewModel
import com.filippoorru.topout.utils.zero
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


// TODO integrate filter for every person->landmark->coordinate. Or maybe just the 2 we need?


@Composable
fun RecordScreen(navController: NavController) {
    suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val cameraProvider = ProcessCameraProvider.getInstance(this)
        cameraProvider.addListener({ continuation.resume(cameraProvider.get()) }, ContextCompat.getMainExecutor(this))
    }

    val context = LocalContext.current

    val viewModel = remember {
        RecordViewModel(context)
    }

    val state = viewModel.state.value

    val lensFacing = remember { CameraSelector.LENS_FACING_BACK }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    val imageAnalyzers = remember { viewModel.getImageAnalyzers() }

    // Execute when closing the screen
    DisposableEffect(Unit) {
        onDispose {
            // TODO how to reopen the detector?
            //detector.close()
            imageAnalyzers.forEach { it.clearAnalyzer() }
        }
    }


    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        val preview = Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .build()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
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
                        val lastSegmentation = state.segmentationState

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val height = size.height
                            val width = size.width

                            if (lastSegmentation != null) {
                                val byteArray = lastSegmentation.mask
                                val imgWidth = lastSegmentation.width
                                val imgHeight = lastSegmentation.height

                                val pixels = IntArray(byteArray.size)
                                for (i in pixels.indices) {
                                    // TODO can this be optimized?
                                    val byte = byteArray[i]
                                    val color: Int =
                                        if (byte != zero) android.graphics.Color.TRANSPARENT else android.graphics.Color.RED
                                    pixels[i] = color
                                }

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

                            if (state.poseState != null) {
                                state.poseState.feet.forEach { (x, y) ->
                                    val center = Offset((1 - y) * size.width, x * size.height)
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

                                state.poseState.feetTrackingPoints.forEach { point ->
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
                        state.toString(),
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
            when (state.climbingState) {
                RecordState.ClimbingState.NotDetected -> {
                    Text(
                        "...",
                        color = Color.White
                    )
                }

                RecordState.ClimbingState.Idle -> {
                    Text(
                        "Idle",
                        color = Color.White
                    )
                }

                RecordState.ClimbingState.Climbing -> {
                    Text(
                        "🔴 REC",
                        color = Color.White
                    )
                }

            }
        }
    }
}