package com.filippoorru.topout.utils

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter

class Segmenter(
    context: Context,
) {
    private val segmenter: InteractiveSegmenter = run {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("interactive_segmentation_model.tflite")
            .setDelegate(Delegate.CPU)
            .build()

        val options =
            InteractiveSegmenter.InteractiveSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setOutputCategoryMask(true)
                .setOutputConfidenceMasks(false)
                //.setResultListener { result, _ -> onSegmentationResult(result) }
                //.setErrorListener(this::returnSegmenterError)
                .build()

        return@run InteractiveSegmenter.createFromOptions(context, options)
    }

    fun segmentImage(
        imageProxy: ImageProxy,
        segmentationPoints: List<Pair<Float, Float>>,
    ): ImageSegmenterResult? {
        val bitmap = BitmapImageBuilder(imageProxy.toBitmap()).build()
        val result = segmenter.segment(
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
        imageProxy.close()

        return result
    }

    fun close() {
        segmenter.close()
    }
}
