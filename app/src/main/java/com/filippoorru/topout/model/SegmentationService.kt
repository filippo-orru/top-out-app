package com.filippoorru.topout.model

import android.content.Context
import androidx.camera.core.ImageProxy
import com.filippoorru.topout.utils.Segmenter
import com.filippoorru.topout.utils.measureTimeMillis
import com.filippoorru.topout.utils.zero
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import kotlin.jvm.optionals.getOrNull

class SegmentationService(
    context: Context,
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

    private fun rateLimit() {
        val timeSinceLastCompleted = System.currentTimeMillis() - (lastSegmentationCompleted ?: 0)
        if (timeSinceLastCompleted < 1000) {
            Thread.sleep(1000 - timeSinceLastCompleted)
        }
    }

    fun onSegmentImage(imageProxy: ImageProxy, segmentationPoints: List<Pair<Float, Float>>) {
        rateLimit()

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
    }

    fun pointIsInSegmentedArea(topRelative: Double, rightRelative: Double): Boolean {
        // No idea why I have to do these rotation gymnastics but whatever, it works.
        val segmentation = lastSegmentation ?: return false

        val x = topRelative * segmentation.width
        val y = (1 - rightRelative) * segmentation.height

        val i = y.toInt() * segmentation.width + x.toInt()
        return i > 0 && i < segmentation.mask.size && segmentation.mask[i] == zero
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