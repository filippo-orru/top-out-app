package com.filippoorru.topout.model

import com.filippoorru.topout.utils.zero

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