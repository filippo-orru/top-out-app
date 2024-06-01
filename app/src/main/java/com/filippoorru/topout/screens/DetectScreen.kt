package com.filippoorru.topout.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

enum class ClimbingState {
    NotDetected,
    Idle,
    Climbing,
}

@Suppress("ConstPropertyName")
object Landmark {
    object Foot {
        const val LeftHeel = 29
        const val RightHeel = 30
    }
}

fun updateClimbingState(
    climbingState: ClimbingState,
    pose: PoseLandmarkerResult?,
    segmentation: ImageSegmenterResult?
): ClimbingState {
    if (pose == null || segmentation == null) {
        return ClimbingState.NotDetected
    }

    // If foot landmark is within the floor segmentation mask, then the user is on the floor

    val person = pose.landmarks().firstOrNull() ?: return ClimbingState.NotDetected

    val floorImage = segmentation.categoryMask().getOrNull() ?: return ClimbingState.NotDetected
    val floor = run {
        val byteBuffer = ByteBufferExtractor.extract(floorImage)
        val pixels = ByteArray(byteBuffer.capacity())
        byteBuffer.get(pixels)
        pixels
    }

    val footLandmarks = listOf(
        person[Landmark.Foot.LeftHeel],
        person[Landmark.Foot.RightHeel]
    )

    val zero = 0.toByte()
    return if (
        footLandmarks.any { floor[it.y().toInt() * floorImage.width + it.x().toInt()] != zero }
    ) {
        ClimbingState.Idle
    } else {
        ClimbingState.Climbing
    }
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

    val segmentationPoint = remember { mutableStateOf(Offset(0.5f, 0.9f)) }

    val climbingState = remember { mutableStateOf(ClimbingState.NotDetected) }

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
                            NormalizedKeypoint.create(
                                segmentationPoint.value.y * imageProxy.height,
                                imageProxy.width - segmentationPoint.value.x * imageProxy.width,
                            ),
                        ),
                    )
                )

                climbingState.value = updateClimbingState(
                    climbingState.value,
                    lastPoseState.value?.result,
                    lastSegmentationState.value?.result
                )
            }
        }

        imageProxy.close()

        imageCounter.intValue = imageCount + 1
    }

    val lensFacing = remember { CameraSelector.LENS_FACING_BACK }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
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
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.background
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
                val lastPose = lastPoseState.value?.result
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    segmentationPoint.value = Offset(it.x / size.width, it.y / size.height)
                                }
                            )
                        }
                ) {
                    val height = size.height
                    val width = size.width

                    lastPose?.landmarks()?.forEach { people ->
                        people.forEachIndexed { i, landmark ->
                            if (i == Landmark.Foot.LeftHeel || i == Landmark.Foot.RightHeel) {
                                drawCircle(
                                    color = Color.White,
                                    center = Offset((1 - landmark.y()) * size.width, landmark.x() * size.height),
                                    radius = 10f
                                )
                                drawCircle(
                                    color = Color.Red,
                                    center = Offset((1 - landmark.y()) * size.width, landmark.x() * size.height),
                                    radius = 6f
                                )
                            } else {
                                drawCircle(
                                    color = Color.Red,
                                    center = Offset((1 - landmark.y()) * size.width, landmark.x() * size.height),
                                    radius = 8f
                                )
                            }
                        }
                    }

                    // Draw the segmentation image mask
                    val lastSegmentation = lastSegmentationState.value?.result?.categoryMask()?.getOrNull()
                    if (lastSegmentation != null) {
                        val imgWidth = lastSegmentation.width
                        val imgHeight = lastSegmentation.height

                        val pixels = run {
                            val byteBuffer = ByteBufferExtractor.extract(lastSegmentation)
                            val pixels = IntArray(byteBuffer.capacity())
                            for (i in pixels.indices) {
                                // TODO can this be optimized?
                                val byte = byteBuffer.get(i)
                                val color: Int =
                                    if (byte.toBoolean()) android.graphics.Color.TRANSPARENT else android.graphics.Color.RED
                                pixels[i] = color
                            }
                            pixels
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
                    }

                    // Draw the segmentation point
                    drawCircle(
                        color = Color.White,
                        center = Offset(segmentationPoint.value.x * width, segmentationPoint.value.y * height),
                        radius = 7f
                    )
                    drawCircle(
                        color = Color.Blue,
                        center = Offset(segmentationPoint.value.x * width, segmentationPoint.value.y * height),
                        radius = 4f
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "${climbingState.value}\n" +
                                "Landmarks: ${lastPose?.landmarks()?.size ?: "null"}, Segmentation: ${segmentationPoint.value}",
                        Modifier.align(Alignment.BottomCenter),
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun Byte.toBoolean(): Boolean = this.toInt() != 0