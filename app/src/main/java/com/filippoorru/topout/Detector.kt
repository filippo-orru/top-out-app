package com.filippoorru.topout

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class Detector private constructor(
    val poseLandmarker: PoseLandmarker,
    val segmentation: InteractiveSegmenter
) {

    fun close() {
        poseLandmarker.close()
        segmentation.close()
    }

    companion object {
        fun create(
            context: Context, runningMode: RunningMode,
            onPoseResult: (result: PoseLandmarkerResult) -> Unit,
            onSegmentationResult: (result: ImageSegmenterResult) -> Unit,
            delegate: Delegate
        ): Detector {
            fun setUpPoseLandmarker(): PoseLandmarker {
                // Set general pose landmarker options
                val baseOptions = BaseOptions.builder()
                    .setDelegate(delegate) // Use the specified hardware for running the model. Default to CPU
                    .setModelAssetPath("pose_landmarker_full.task")
                    .build()

                // Create an option builder with base options and specific
                // options only use for Pose Landmarker.
                val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setRunningMode(runningMode)

                // The ResultListener and ErrorListener are only used in LIVE_STREAM mode.
                // TODO can these these listeners be removed?
                if (runningMode == RunningMode.LIVE_STREAM) {
                    optionsBuilder
                        .setResultListener { result, _ -> onPoseResult(result) }
                }
                return PoseLandmarker.createFromOptions(context, optionsBuilder.build())
            }

            fun setUpSegmentation(): InteractiveSegmenter {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("interactive_segmentation_model.tflite")
                    .setDelegate(delegate)
                    .build()

                val optionsBuilder =
                    InteractiveSegmenter.InteractiveSegmenterOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setOutputCategoryMask(true)
                        .setOutputConfidenceMasks(false)
                //.setResultListener { result, _ -> onSegmentationResult(result) }
                //.setErrorListener(this::returnSegmenterError)

                val options = optionsBuilder.build()
                return InteractiveSegmenter.createFromOptions(context, options)

            }

            return Detector(
                setUpPoseLandmarker(),
                setUpSegmentation()
            )
        }
    }
}