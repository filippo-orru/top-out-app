package com.filippoorru.topout.ui.model

import android.content.Context
import android.os.Environment
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filippoorru.topout.database.AttemptEntity
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.database.PartOfRouteVisitRecording
import com.filippoorru.topout.database.RouteVisitEntity
import com.filippoorru.topout.database.RouteVisitRecording
import com.filippoorru.topout.services.ClimbingStateService
import com.filippoorru.topout.services.PoseDetectorService
import com.filippoorru.topout.services.PoseDetectorService.Companion.getSurroundingTrackingPoints
import com.filippoorru.topout.services.SegmentationService
import com.filippoorru.topout.utils.zero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors


class RecordViewModel(
    context: Context,
) : ViewModel() {
    // State
    private val _poseState = MutableStateFlow<PoseState?>(null)
    val poseState = _poseState.asStateFlow()

    private val _segmentationState = MutableStateFlow<SegmentationState?>(null)
    val segmentationState = _segmentationState.asStateFlow()

    private val _climbingState = MutableStateFlow(ClimbingState.NotDetected)
    val climbingState = _climbingState.asStateFlow()

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.NotRecording)
    val recordingState = _recordingState.asStateFlow()


    // Detector services
    private val poseDetectorService = PoseDetectorService(context, ::updatePoseState)
    private val segmentationService = SegmentationService(context, ::updateSegmentationState)
    private val climbingStateService = ClimbingStateService()//::updateClimbingState)


    val routeVisitId = "topout-" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

    // Recording
    private val recordingFileName = "$routeVisitId.mp4"
    private val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD))
        .build()
    private var recordingStartTimestamp = 0L
    private val outputOptions = run {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
        moviesDir.mkdirs()

        FileOutputOptions
            .Builder(moviesDir.resolve(recordingFileName))
            .build()
    }
    private val pendingRecording = recorder.prepareRecording(context, outputOptions)
    private var recording: Recording? = null


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
            // Most feet tracking points are on the ground
            ClimbingState.Idle
        } else {
            ClimbingState.Climbing
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

    private val imageAnalyzers = arrayOf(
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
            },
    )

    val useCases: Array<UseCase> = arrayOf(
        VideoCapture.withOutput(recorder),
        *imageAnalyzers
    )

    fun startRecording() {
        recordingStartTimestamp = System.currentTimeMillis()
        recording = pendingRecording.start({}, {})
        _recordingState.value = RecordingState.Recording(recordingStartTimestamp)
        saveRouteVisit()
    }

    fun stopRecording() {
        imageAnalyzers.forEach { it.clearAnalyzer() }

        poseDetectorService.dispose()
        segmentationService.dispose()

        recording?.stop() // File is automatically saved to MediaStore
        _recordingState.value = RecordingState.NotRecording

        saveRouteVisit()
    }

    private fun saveRouteVisit() {
        viewModelScope.launch {
            val attempts = climbingStateService.getAttempts(recordingStartTimestamp, System.currentTimeMillis())
            //println(attempts)
            Database.i.routeVisits().save(
                RouteVisitEntity(
                    id = routeVisitId,
                    recording = RouteVisitRecording(
                        outputOptions.file.absolutePath,
                    ),
                    timestamp = recordingStartTimestamp,
                )
            )

            Database.i.attempts().saveAll(
                attempts.mapIndexed { index, attempt ->
                    AttemptEntity(
                        id = "attempt-$routeVisitId-$index",
                        routeVisitId = routeVisitId,
                        partOfRouteVisitRecording = PartOfRouteVisitRecording(
                            startMs = attempt.startMs,
                            endMs = attempt.endMs,
                        ),
                    )
                }
            )
        }
    }

    override fun toString(): String {
        return """
            RecordViewModel(
                poseState=${poseState.value},
                segmentationState=${segmentationState.value},
                climbingState=${climbingState.value}
            )
        """.trimIndent()
    }
}

enum class ClimbingState {
    NotDetected,
    Idle,
    Climbing,
}

data class PoseState(
    val feet: List<TrackingPoint>,
    val feetTrackingPoints: List<TrackingPoint>,
    val averageDuration: Long,
)

data class TrackingPoint(
    val x: Double,
    val y: Double,
    val isInMask: Boolean,
)

val List<TrackingPoint>.isInMask get() = count { it.isInMask } > size / 2

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

sealed class RecordingState {
    data class Recording(val startTimestamp: Long) : RecordingState()
    data object NotRecording : RecordingState()
}