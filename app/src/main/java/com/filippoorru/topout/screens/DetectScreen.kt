package com.filippoorru.topout.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.filippoorru.topout.Detector
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.optionals.getOrNull


class LastPose(
    val result: PoseLandmarkerResult
) {
    val timestamp: Long get() = result.timestampMs()
}

class LastSegmentation(
    val result: ImageSegmenterResult
) {
    val timestamp: Long get() = result.timestampMs()
}

@Composable
fun DetectScreen(navController: NavController) {
    suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val cameraProvider = ProcessCameraProvider.getInstance(this)
        cameraProvider.addListener({ continuation.resume(cameraProvider.get()) }, ContextCompat.getMainExecutor(this))
    }

    val context = LocalContext.current

    val lastPoseState: MutableState<LastPose?> = remember { mutableStateOf(null) }
    val lastSegmentationState: MutableState<LastSegmentation?> = remember { mutableStateOf(null) }

    val detector: Detector = remember {
        Detector.create(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            onPoseResult = { result -> lastPoseState.value = LastPose(result) },
            onSegmentationResult = { result -> /*lastSegmentationState.value = LastSegmentation(result)*/ },
            delegate = Delegate.CPU
        )
    }

    val imageCounter = remember { mutableIntStateOf(0) }

    val segmentationPoint = Offset(0.5f, 0.9f)

    fun onNewImage(imageProxy: ImageProxy) {
        val imageCount = imageCounter.intValue
        val lastSegmentation = lastSegmentationState.value

        val detectPose = imageCount % 2 == 0
        val detectSegmentation = lastSegmentation == null || System.currentTimeMillis() - lastSegmentation.timestamp > 1000

        if (detectPose || detectSegmentation) {
            val bitmap = BitmapImageBuilder(imageProxy.toBitmap()).build()
            if (detectPose) {
                detector.poseLandmarker.detectAsync(bitmap, imageProxy.imageInfo.timestamp)
            }
            if (detectSegmentation) {
                lastSegmentationState.value = LastSegmentation(
                    detector.segmentation.segment(
                        bitmap,
                        InteractiveSegmenter.RegionOfInterest.create(
                            NormalizedKeypoint.create(segmentationPoint.x, segmentationPoint.y),
                        ),
                    )
                )
            }
        }

        imageProxy.close()

        imageCounter.intValue = imageCount + 1
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
                    onNewImage(imageProxy)
                }
            }
    }

    fun close() {
        // TODO how to reopen the detector?
        //detector.close()
        imageAnalysis.clearAnalyzer()
    }

    // Execute when closing the screen
    DisposableEffect(Unit) {
        onDispose(::close)
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
        val lastPose = lastPoseState.value?.result
        if (lastPose == null) {
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
                color = Color.Black.copy(alpha = if (lastPose.landmarks().isNotEmpty()) 0.15f else 0.0f),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val height = size.height
                    val width = size.width

                    // Draw the pose
                    lastPose.landmarks().forEach { landmarks ->
                        landmarks.forEach { landmark ->
                            drawCircle(
                                color = Color.Red,
                                center = Offset((1 - landmark.y()) * size.width, landmark.x() * size.height),
                                radius = 8f
                            )
                        }
                    }

                    // Draw the segmentation image mask
                    val lastSegmentation = lastSegmentationState.value?.result?.categoryMask()?.getOrNull()
                    if (lastSegmentation != null) {
                        val byteBuffer = ByteBufferExtractor.extract(lastSegmentation)
                        val pixels = IntArray(byteBuffer.capacity())
                        for (i in pixels.indices) {
                            val byte = byteBuffer.get(i)
                            val color = if (byte.toBoolean()) android.graphics.Color.RED else android.graphics.Color.TRANSPARENT
                            pixels[i] = color
                        }

                        drawImage(
                            Bitmap.createBitmap(pixels, lastSegmentation.width, lastSegmentation.height, Bitmap.Config.ARGB_8888)
                                .asImageBitmap(),
                            alpha = 0.3f,
                            srcSize = IntSize(width.toInt(), height.toInt())
                        )
                    }

                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Landmarks: ${lastPose.landmarks().size}",
                        Modifier.align(Alignment.BottomCenter),
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun Byte.toBoolean(): Boolean = this.toInt() != 0