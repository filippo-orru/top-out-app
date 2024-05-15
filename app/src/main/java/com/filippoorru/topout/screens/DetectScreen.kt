package com.filippoorru.topout.screens

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.filippoorru.topout.Detector
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@Composable
fun DetectScreen(navController: NavController) {
    suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val cameraProvider = ProcessCameraProvider.getInstance(this)
        cameraProvider.addListener({ continuation.resume(cameraProvider.get()) }, ContextCompat.getMainExecutor(this))
    }

    val context = LocalContext.current

    val lastResult: MutableState<PoseLandmarkerResult?> = remember { mutableStateOf(null) }

    val detector: Detector = remember {
        Detector.create(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,

            onResult = { result ->
                // Handle the result
                lastResult.value = result
            },
            onError = { error ->
                // Handle the error
                println("error")
            },
            delegate = Delegate.CPU
        )!!
    }

    val lensFacing = remember { CameraSelector.LENS_FACING_BACK }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context)
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            //.setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            //.setImageQueueDepth(100)
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    // Handle the image
                    detector.poseLandmarker.detectAsync(
                        /* image = */ BitmapImageBuilder(imageProxy.toBitmap()).build(),
                        /* timestampMs = */ imageProxy.imageInfo.timestamp
                    )
                    imageProxy.close()
                }
            }
    }

    // Execute when closing the screen
    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            imageAnalysis.clearAnalyzer()
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        val preview = Preview.Builder().build()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.Builder().requireLensFacing(lensFacing).build(),
            preview,
            imageAnalysis
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
        val result = lastResult.value
        if (result == null) {
            Text(
                text = "No result",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                color = Color.Black.copy(alpha = if (result.landmarks().isNotEmpty()) 0.15f else 0.0f),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw the result
                    result.landmarks().forEach { landmarks ->
                        landmarks.forEach { landmark ->
                            drawCircle(
                                color = Color.Red,
                                center = Offset((1 - landmark.y()) * size.width, landmark.x() * size.height),
                                radius = 8f
                            )
                        }
                    }

                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Landmarks: ${result.landmarks().size}",
                        Modifier.align(Alignment.BottomCenter),
                        color = Color.White
                    )
                }
            }
        }
    }
}