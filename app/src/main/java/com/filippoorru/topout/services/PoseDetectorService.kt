package com.filippoorru.topout.services

import android.content.Context
import androidx.camera.core.ImageProxy
import com.filippoorru.topout.utils.measureTimeMillis
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.cos
import kotlin.math.sin

class PoseDetectorService(
    context: Context,
    val updateState: () -> Unit,
) {
    private val poseLandmarker: PoseLandmarker = run {
        // Set general pose landmarker options
        val baseOptions = BaseOptions.builder()
            .setDelegate(Delegate.GPU) // Use the specified hardware for running the model. Default to CPU
            .setModelAssetPath("pose_landmarker_full.task")
            .build()

        // Create an option builder with base options and specific
        // options only use for Pose Landmarker.
        val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setRunningMode(RunningMode.VIDEO)

        return@run PoseLandmarker.createFromOptions(context, optionsBuilder.build())
    }

    // TODO integrate filter for every person->landmark->coordinate. Or maybe just the 2 we need?
    class PoseInfo(
        val result: PoseLandmarkerResult,
    )

    var lastPose: PoseInfo? = null
    private val durations = mutableListOf<Long>()
    val averageDuration get() = durations.average().toLong()

    fun onDetectPose(imageProxy: ImageProxy) {
        val bitmap = BitmapImageBuilder(imageProxy.toBitmap()).build()

        val (result, duration) = measureTimeMillis {
            poseLandmarker.detectForVideo(bitmap, imageProxy.imageInfo.timestamp)
        }

        lastPose = PoseInfo(
            result
        )
        durations.add(duration)
        if (durations.size > 10) {
            durations.removeAt(0)
        }

        imageProxy.close()
        updateState()
    }

    fun close() {
        poseLandmarker.close()
    }

    companion object {
        fun getFeet(person: List<NormalizedLandmark>) = listOf(person[Landmark.Foot.LeftHeel], person[Landmark.Foot.RightHeel])

        fun NormalizedLandmark.getSurroundingTrackingPoints(): List<Pair<Double, Double>> {
            val distance = 0.055
            val aspect = 4 / 3.0 // TODO get from actual image aspect ratio
            return listOf(0.3, 0.45, 0.65, 0.7).map { angle ->
                val dx = cos(angle * Math.PI * 2) * distance * aspect
                val dy = sin(angle * Math.PI * 2) * distance
                val x = this.x() - dx
                val y = this.y() + dy
                x to y
            }
        }
    }
}

@Suppress("ConstPropertyName")
object Landmark {
    object Foot {
        const val LeftHeel = 29
        const val RightHeel = 30
    }
}