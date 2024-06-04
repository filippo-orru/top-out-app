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
import com.filippoorru.topout.utils.OneEuroFilter
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.optionals.getOrNull
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureTimeMillis


// TODO integrate filter for every person->landmark->coordinate. Or maybe just the 2 we need?
class LastPose(
    val result: PoseLandmarkerResult
) {
    val timestamp: Long get() = result.timestampMs()
}

class LastSegmentation(
    val result: ImageSegmenterResult
) {
    val timestamp: Long = System.currentTimeMillis()
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

    fun onPoseResult(result: PoseLandmarkerResult) {
        lastPoseState.value = LastPose(result)
    }

    val detector: Detector = remember {
        Detector.create(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            onPoseResult = ::onPoseResult,
        )
    }

    val climbingState = remember { mutableStateOf(ClimbingState.NotDetected) }
    fun updateClimbingState() {
        fun get(): ClimbingState {
            val pose = lastPoseState.value?.result
            val segmentation = lastSegmentationState.value?.result

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

        climbingState.value = get()
    }

    val poseFilter = remember { OneEuroFilter(0.1) }
    val poseDurations = remember { mutableListOf<Long>() }
    fun detectPose(imageProxy: ImageProxy) {
        val bitmap = BitmapImageBuilder(imageProxy.toBitmap()).build()

        val duration = measureTimeMillis {
            detector.poseLandmarker.detectAsync(bitmap, imageProxy.imageInfo.timestamp)
        }
        poseDurations.add(duration)
        if (poseDurations.size > 10) {
            poseDurations.removeAt(0)
        }
        updateClimbingState()

        imageProxy.close()
    }

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
    val segmentationDurations = remember { mutableListOf<Long>() }
    fun segmentImage(imageProxy: ImageProxy) {
        val lastSegmentation = lastSegmentationState.value

        // TODO only run when the image changes significantly?
        val timeSinceLastCompleted = System.currentTimeMillis() - (lastSegmentation?.timestamp ?: 0)
        if (lastSegmentation != null && timeSinceLastCompleted < 1000) {
            sleep(1000 - timeSinceLastCompleted)
        }

        val bitmap = BitmapImageBuilder(imageProxy.toBitmap()).build()
        val duration = measureTimeMillis {
            val result: ImageSegmenterResult? = detector.segmentation.segment(
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
            if (result != null) {
                lastSegmentationState.value = LastSegmentation(result)
            }
        }

        segmentationDurations.add(duration)
        if (segmentationDurations.size > 10) {
            segmentationDurations.removeAt(0)
        }

        updateClimbingState()

        imageProxy.close()
    }

    val lensFacing = remember { CameraSelector.LENS_FACING_BACK }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    val poseImageAnalyzer = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
                    .build()
            )
            .build()
            .apply { setAnalyzer(Executors.newSingleThreadExecutor(), ::detectPose) }
    }

    val segmentImageAnalyzer = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
                    .build()
            )
            .build()
            .apply { setAnalyzer(Executors.newSingleThreadExecutor(), ::segmentImage) }
    }

    // Execute when closing the screen
    DisposableEffect(Unit) {
        onDispose {
            // TODO how to reopen the detector?
            //detector.close()
            segmentImageAnalyzer.clearAnalyzer()
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
            poseImageAnalyzer,
            segmentImageAnalyzer,
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
                                "People: ${lastPose?.landmarks()?.size ?: 0}\n"
                                        + " Pose: ${poseDurations.average()}ms\n"
                                        + " Segmentation: ${segmentationDurations.average()}ms\n",
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