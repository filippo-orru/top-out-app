package com.filippoorru.topout.services

import com.filippoorru.topout.ui.model.ClimbingState
import com.squareup.moshi.JsonClass

class ClimbingStateService {
    private val climbingStateHistory = mutableListOf<ClimbingStateHistoryItem>()

    fun onNewClimbingState(climbingState: ClimbingState, timestamp: Long) {
        climbingStateHistory.add(
            ClimbingStateHistoryItem(
                climbing = climbingState == ClimbingState.Climbing,
                timestamp
            )
        )
    }

    fun getAttempts(startTimestamp: Long, endTimestamp: Long): List<Attempt> {
        val minDuration = 1000L

        val attempts = mutableListOf<Attempt>()

        var first = ClimbingStateHistoryItem(climbing = false, startTimestamp)
        var last: ClimbingStateHistoryItem? = null

        val history = climbingStateHistory +
                // Make sure to process the last attempt if the history ends with a climbing state
                ClimbingStateHistoryItem(climbing = false, endTimestamp)

        for (climb in history) {
            if (
                last != null &&
                climb.timestamp - last.timestamp > minDuration &&
                climb.climbing != first.climbing
            ) {
                if (first.climbing) {
                    val timestamp = if (first.timestamp == startTimestamp) {
                        first.timestamp
                    } else {
                        // Average, but closer to the first timestamp
                        listOf(first.timestamp, first.timestamp, last.timestamp).average().toLong()
                    }

                    attempts.add(
                        Attempt(
                            startMs = timestamp - startTimestamp,
                            endMs = climb.timestamp - startTimestamp
                        )
                    )
                }

                first = climb
                last = null
            } else {
                last = climb
            }
        }

        return attempts.toList()
    }
}

data class Attempt(
    // Times are relative to the start of the route visit recording
    // Might need to be clamped to the actual recording duration
    val startMs: Long,
    val endMs: Long,
)

@JsonClass(generateAdapter = true)
class ClimbingStateHistoryItem(
    val climbing: Boolean,
    val timestamp: Long,
)