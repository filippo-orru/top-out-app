package com.filippoorru.topout.model

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.lifecycle.ViewModel
import com.filippoorru.topout.services.ClimbingStateService
import com.filippoorru.topout.services.PoseDetectorService
import com.filippoorru.topout.services.PoseDetectorService.Companion.getSurroundingTrackingPoints
import com.filippoorru.topout.services.SegmentationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors


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
    private val climbingStateService = ClimbingStateService()//::updateClimbingState)

    private fun updatePoseState() {
        _poseState.value = poseDetectorService.landmarks?.let { person ->
            val feet = PoseDetectorService.getFeet(person)
            val feetWithTrackerPositions = feet
                .associateWith { foot ->
                    foot.getSurroundingTrackingPoints().map { (x, y) ->
                        TrackingPoint(x, y, isInMask = segmentationState.value?.containsPoint(x, y) == true)
                    }
                }

            val trackerPositions = feetWithTrackerPositions.values.flatten()
            PoseState(
                feet = feetWithTrackerPositions.map { (foot, trackerPositions) ->
                    TrackingPoint(foot.first, foot.second, isInMask = trackerPositions.isInMask)
                },
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
        } else if (pose.feetTrackingPoints.isInMask) {
            // Most feet tracking points have left the ground
            ClimbingState.Climbing
        } else {
            ClimbingState.Idle
        }
        _climbingState.value = climbingState
        climbingStateService.onNewClimbingState(climbingState, System.currentTimeMillis())
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

    fun close() {
        poseDetectorService.close()
        segmentationService.close()
    }

    override fun toString(): String {
        return "RecordViewModel(" +
                "poseState=${poseState.value}, " +
                "segmentationState=${segmentationState.value}, " +
                "climbingState=${climbingState.value}" +
                ")"
    }
}