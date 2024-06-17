package com.filippoorru.topout.services

import android.content.Context
import androidx.camera.core.ImageProxy
import com.filippoorru.topout.utils.Segmenter
import com.filippoorru.topout.utils.measureTimeMillis
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import kotlin.jvm.optionals.getOrNull

class SegmentationService(
    context: Context,
    val updateState: () -> Unit,
) {
    private val segmenter = Segmenter(context)


    class SegmentationInfo(
        val result: ImageSegmenterResult,
        val mask: ByteArray,
        val width: Int,
        val height: Int,
    )

    var lastSegmentation: SegmentationInfo? = null
    private var lastSegmentationCompleted: Long? = null
    private val durations = mutableListOf<Long>()
    val averageDuration get() = durations.average().toLong()

    private var disposed = false

    fun onSegmentImage(imageProxy: ImageProxy, segmentationPoints: List<Pair<Float, Float>>) {
        if (disposed) {
            return
        }

        if (lastSegmentationCompleted != null && System.currentTimeMillis() - lastSegmentationCompleted!! > 1000) {
            return
        }

        val (result, duration) = measureTimeMillis {
            return@measureTimeMillis segmenter.segmentImage(imageProxy, segmentationPoints)
        }
        result
            ?.let { getSegmentationInfo(it) }
            ?.let { lastSegmentation = it } // Only update if successful

        lastSegmentationCompleted = System.currentTimeMillis()

        durations.add(duration)
        if (durations.size > 10) {
            durations.removeAt(0)
        }
        updateState()
    }

    fun dispose() {
        segmenter.close()
        disposed = true
    }

    companion object {
        private fun getSegmentationInfo(result: ImageSegmenterResult): SegmentationInfo? {
            val image = result.categoryMask()?.getOrNull() ?: return null
            val mask = run {
                val byteBuffer = ByteBufferExtractor.extract(image)
                val pixels = ByteArray(byteBuffer.capacity())
                byteBuffer.get(pixels)
                pixels
            }
            return SegmentationInfo(result, mask, image.width, image.height)
        }
    }
}