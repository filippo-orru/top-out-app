package com.filippoorru.topout

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class Detector private constructor(
    val poseLandmarker: PoseLandmarker,
) {

    fun close() {
        poseLandmarker.close()
    }

    companion object {
        fun create(
            context: Context, runningMode: RunningMode,
            onResult: (result: PoseLandmarkerResult) -> Unit,
            onError: (RuntimeException) -> Unit,
            delegate: Delegate
        ): Detector? {
            // Set general pose landmarker options
            val baseOptionsBuilder = BaseOptions.builder()

            // Use the specified hardware for running the model. Default to CPU

            baseOptionsBuilder.setDelegate(delegate)
            val modelName = "pose_landmarker_full.task"
            baseOptionsBuilder.setModelAssetPath(modelName)

            try {
                val baseOptions = baseOptionsBuilder.build()
                // Create an option builder with base options and specific
                // options only use for Pose Landmarker.
                val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setRunningMode(runningMode)

                // The ResultListener and ErrorListener only use for LIVE_STREAM mode.
                if (runningMode == RunningMode.LIVE_STREAM) {
                    optionsBuilder
                        .setResultListener { result, _ -> onResult(result) }
                        .setErrorListener { onError(it) }
                    // TODO do these these listeners need to be removed?
                }

                return Detector(
                    PoseLandmarker.createFromOptions(
                        context,
                        optionsBuilder.build()
                    )
                )
            } catch (e: IllegalStateException) {
                /*poseLandmarkerHelperListener?.onError(
                    "Pose Landmarker failed to initialize. See error logs for " +
                            "details"
                )*/
                println(
                    "MediaPipe failed to load the task with error: " + e
                        .message
                )
            } catch (e: RuntimeException) {
                // This occurs if the model being used does not support GPU
                /*poseLandmarkerHelperListener?.onError(
                    "Pose Landmarker failed to initialize. See error logs for " +
                            "details", GPU_ERROR
                )*/
                println("Image classifier failed to load model with error: " + e.message)
            }

            return null
        }
    }
}