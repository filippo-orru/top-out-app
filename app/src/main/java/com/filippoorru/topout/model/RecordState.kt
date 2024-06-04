package com.filippoorru.topout.model

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.filippoorru.topout.PoseDetectorService
import com.filippoorru.topout.PoseDetectorService.Companion.getSurroundingTrackingPoints
import java.util.concurrent.Executors

data class RecordState(
    val climbingState: ClimbingState,
    val segmentationState: SegmentationState? = null,
    val poseState: PoseState? = null,
) {
    enum class ClimbingState {
        NotDetected,
        Idle,
        Climbing,
    }

    class SegmentationState(
        val mask: ByteArray,
        val width: Int,
        val height: Int,
        val averageDuration: Long,
    )

    class PoseState(
        val feet: List<Pair<Float, Float>>,
        val feetTrackingPoints: List<TrackingPoint>,
        val averageDuration: Long,
    )

    class TrackingPoint(
        val x: Double,
        val y: Double,
        val isInMask: Boolean,
    )

    companion object {
        fun initial() = RecordState(
            climbingState = ClimbingState.NotDetected
        )
    }
}

class RecordViewModel(
    context: Context,
) : ViewModel() {
    private val _state = mutableStateOf(RecordState.initial())
    val state: State<RecordState> = _state

    private val segmentationService = SegmentationService(context)
    private val poseDetectorService = PoseDetectorService(context)

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

    private fun updateState() {
        // If foot landmark is within the floor segmentation mask, then the user is on the floor

        val pose = poseDetectorService.lastPose ?: return
        val person = pose.result.landmarks().firstOrNull()

        if (person == null) {
            _state.value = _state.value.copy(
                climbingState = RecordState.ClimbingState.NotDetected
            )
        } else {
            val feet = PoseDetectorService.getFeet(person)
            val trackerPositions = feet
                .flatMap { it.getSurroundingTrackingPoints() }
                .map { (x, y) -> RecordState.TrackingPoint(x, y, segmentationService.pointIsInSegmentedArea(x, y)) }

            val climbingState: RecordState.ClimbingState = if (trackerPositions.count { it.isInMask } < trackerPositions.size / 2) {
                RecordState.ClimbingState.Climbing
            } else {
                RecordState.ClimbingState.Idle
            }

            _state.value = _state.value.copy(
                climbingState = climbingState,
                segmentationState = segmentationService.lastSegmentation?.let {
                    RecordState.SegmentationState(it.mask, it.width, it.height, segmentationService.averageDuration)
                },
                poseState = RecordState.PoseState(
                    feet.map { it.x() to it.y() },
                    trackerPositions,
                    poseDetectorService.averageDuration
                )
            )
        }
    }

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
}