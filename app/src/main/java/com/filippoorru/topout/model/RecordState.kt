package com.filippoorru.topout.model

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.lifecycle.ViewModel
import com.filippoorru.topout.PoseDetectorService
import com.filippoorru.topout.PoseDetectorService.Companion.getSurroundingTrackingPoints
import com.filippoorru.topout.utils.zero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors


enum class ClimbingState {
    NotDetected,
    Idle,
    Climbing,
}

data class PoseState(
    val feet: List<Pair<Float, Float>>,
    val feetTrackingPoints: List<TrackingPoint>,
    val averageDuration: Long,
)

data class TrackingPoint(
    val x: Double,
    val y: Double,
    val isInMask: Boolean,
)

class SegmentationState(
    val mask: ByteArray,
    val width: Int,
    val height: Int,
    val averageDuration: Long,
) {
    fun containsPoint(topRelative: Double, rightRelative: Double): Boolean {
        // No idea why I have to do these rotation gymnastics but whatever, it works.
        val x = topRelative * width
        val y = (1 - rightRelative) * height

        val i = y.toInt() * width + x.toInt()
        return i > 0 && i < mask.size && mask[i] == zero
    }

    override fun toString(): String {
        return "SegmentationState(mask.size=${mask.size}, width=$width, height=$height, averageDuration=$averageDuration)"
    }
}

class RecordViewModel(
    context: Context,
) : ViewModel() {
    private val _poseState = MutableStateFlow<PoseState?>(null)
    val poseState = _poseState.asStateFlow()

    private val _segmentationState = MutableStateFlow<SegmentationState?>(null)
    val segmentationState = _segmentationState.asStateFlow()

    private val _climbingState = MutableStateFlow(ClimbingState.NotDetected)
    val climbingState = _climbingState.asStateFlow()

    private val poseDetectorService = PoseDetectorService(context, ::updatePoseState)
    private val segmentationService = SegmentationService(context, ::updateSegmentationState)

    private fun updatePoseState() {
        _poseState.value = poseDetectorService.lastPose?.result?.landmarks()?.firstOrNull()?.let { person ->
            val feet = PoseDetectorService.getFeet(person)
            val trackerPositions = feet
                .flatMap { it.getSurroundingTrackingPoints() }
                .map { (x, y) -> TrackingPoint(x, y, isInMask = segmentationState.value?.containsPoint(x, y) == true) }

            PoseState(
                feet = feet.map { it.x() to it.y() },
                feetTrackingPoints = trackerPositions,
                averageDuration = poseDetectorService.averageDuration,
            )
        }

        updateClimbingState()
    }

    private fun updateSegmentationState() {
        _segmentationState.value = segmentationService.lastSegmentation?.let {
            SegmentationState(it.mask, it.width, it.height, segmentationService.averageDuration)
        }
        updateClimbingState()
    }

    private fun updateClimbingState() {
        val pose = poseState.value
        val climbingState: ClimbingState = if (pose == null) {
            ClimbingState.NotDetected
        } else if (pose.feetTrackingPoints.count { it.isInMask } < pose.feetTrackingPoints.size / 2) {
            // Most feet tracking points have left the ground
            ClimbingState.Climbing
        } else {
            ClimbingState.Idle
        }

        _climbingState.value = climbingState
    }

    val segmentationPoints = listOf(
        0.09f to 0.89f,
        0.2f to 0.95f,
        0.3f to 0.89f,
        0.4f to 0.95f,

        0.6f to 0.95f,
        0.7f to 0.89f,
        0.8f to 0.95f,
        0.91f to 0.89f,
    )

    fun getImageAnalyzers(): Array<ImageAnalysis> {
        return arrayOf(
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
                        .build()
                )
                .build()
                .apply {
                    setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        segmentationService.onSegmentImage(imageProxy, segmentationPoints)
                    }
                },

            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
                        .build()
                )
                .build()
                .apply {
                    setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        poseDetectorService.onDetectPose(imageProxy)
                    }
                }
        )
    }

    override fun toString(): String {
        return "RecordViewModel(" +
                "poseState=${poseState.value}, " +
                "segmentationState=${segmentationState.value}, " +
                "climbingState=${climbingState.value}" +
                ")"
    }
}