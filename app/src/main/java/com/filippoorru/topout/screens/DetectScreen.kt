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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.filippoorru.topout.Detector
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin


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

const val zero = 0.toByte()

fun trackerPositionIsInMask(
    topRelative: Double,
    rightRelative: Double,
    mask: ByteArray,
    width: Int,
    height: Int,
): Boolean {
    // No idea why I have to do these rotation gymnastics but whatever, it works.
    val x = topRelative * width
    val y = (1 - rightRelative) * height

    val i = y.toInt() * width + x.toInt()
    return i > 0 && i < mask.size && mask[i] == zero
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

    val trackerPositions = getTrackerPositions(person)

    return if (
        trackerPositions.count { (x, y) ->
            trackerPositionIsInMask(x, y, floor, floorImage.width, floorImage.height)
        } < trackerPositions.size / 2
    ) {
        ClimbingState.Climbing
    } else {
        ClimbingState.Idle
    }
}

private fun getTrackerPositions(person: List<NormalizedLandmark>): List<Pair<Double, Double>> {
    return listOf(
        person[Landmark.Foot.LeftHeel],
        person[Landmark.Foot.RightHeel]
    ).flatMap { landmark ->
        val distance = 0.055
        val aspect = 4 / 3.0
        listOf(0.3, 0.45, 0.65, 0.7).map { angle ->
            val dx = cos(angle * Math.PI * 2) * distance * aspect
            val dy = sin(angle * Math.PI * 2) * distance
            val x = landmark.x() - dx
            val y = landmark.y() + dy
            x to y
        }
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

    val segmentationPoints = remember {
        listOf(
            Offset(0.09f, 0.89f),
            Offset(0.2f, 0.95f),
            Offset(0.3f, 0.89f),
            Offset(0.4f, 0.95f),

            Offset(0.6f, 0.95f),
            Offset(0.7f, 0.89f),
            Offset(0.8f, 0.95f),
            Offset(0.91f, 0.89f),
        )
    }

    val climbingState = remember { mutableStateOf(ClimbingState.NotDetected) }

    fun onNewImage(imageProxy: ImageProxy) {
        val imageCount = imageCounter.intValue
        val lastSegmentation = lastSegmentationState.value

        // TODO only update when the image changes significantly
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
                            segmentationPoints.map { (x, y) ->
                                NormalizedKeypoint.create(
                                    y * imageProxy.width,
                                    imageProxy.height - x * imageProxy.height,
                                )
                            },
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
                        val lastPose = lastPoseState.value?.result

                        val lastSegmentation = lastSegmentationState.value?.result?.categoryMask()?.getOrNull()

                        if (lastPose != null && lastSegmentation != null) {
                            val imgWidth = lastSegmentation.width
                            val imgHeight = lastSegmentation.height

                            val byteArray = ByteBufferExtractor.extract(lastSegmentation).let { byteBuffer ->
                                val byteArray = ByteArray(byteBuffer.capacity())
                                byteBuffer.get(byteArray)
                                byteArray
                            }

                            val pixels = IntArray(byteArray.size)
                            for (i in pixels.indices) {
                                // TODO can this be optimized?
                                val byte = byteArray[i]
                                val color: Int =
                                    if (byte.toBoolean()) android.graphics.Color.TRANSPARENT else android.graphics.Color.RED
                                pixels[i] = color
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val height = size.height
                                val width = size.width

                                lastPose.landmarks().forEach { person ->
                                    listOf(
                                        person[Landmark.Foot.LeftHeel],
                                        person[Landmark.Foot.RightHeel]
                                    ).forEach { landmark ->
                                        val center = Offset((1 - landmark.y()) * size.width, landmark.x() * size.height)
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

                                    val trackerPositions = getTrackerPositions(person)
                                    trackerPositions.forEach { (x, y) ->
                                        val isOnGround = trackerPositionIsInMask(x, y, byteArray, imgWidth, imgHeight)
                                        val center = Offset(((1 - y) * size.width).toFloat(), (x * size.height).toFloat())
                                        drawCircle(
                                            color = Color.White,
                                            center = center,
                                            radius = 7f
                                        )
                                        if (!isOnGround) {
                                            drawCircle(
                                                color = Color.Red,
                                                center = center,
                                                radius = 5f
                                            )
                                        }
                                    }
                                }

                                // Draw the segmentation image mask


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
                                segmentationPoints.forEach { segmentationPoint ->
                                    drawCircle(
                                        color = Color.White,
                                        center = Offset(segmentationPoint.x * width, segmentationPoint.y * height),
                                        radius = 7f
                                    )
                                    drawCircle(
                                        color = Color.Blue,
                                        center = Offset(segmentationPoint.x * width, segmentationPoint.y * height),
                                        radius = 5f
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "People: ${lastPose?.landmarks()?.size ?: 0}",
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
                    when (climbingState.value) {
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
    }
}

fun Byte.toBoolean(): Boolean = this.toInt() != 0