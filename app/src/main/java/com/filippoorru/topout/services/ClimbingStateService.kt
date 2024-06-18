package com.filippoorru.topout.services

import com.filippoorru.topout.ui.model.ClimbingState
import com.squareup.moshi.JsonClass

class ClimbingStateService {
    private val climbingStateHistory = mutableListOf<ClimbingStateHistoryItem>()

    fun onNewClimbingState(climbingState: ClimbingState, timestamp: Long) {
        climbingStateHistory.add(ClimbingStateHistoryItem(climbingState, timestamp))
    }

    fun getAttempts(startTimestamp: Long): List<Attempt> {
        val minDuration = 1000L

        val attempts = mutableListOf<Attempt>()

        var first = ClimbingStateHistoryItem(ClimbingState.Idle, startTimestamp)
        var last: ClimbingStateHistoryItem? = null

        val history = climbingStateHistory +
                ClimbingStateHistoryItem(
                    ClimbingState.Idle,
                    Long.MAX_VALUE
                ) // Make sure to process the last attempt if the history ends with a climbing state

        for (climb in history) {
            if (last == null) {
                last = climb
            } else if (climb.timestamp - last.timestamp > minDuration) {
                if (last.climbingState == first.climbingState) {
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

class Attempt(
    // Times are relative to the start of the route visit recording
    // Might need to be clamped to the actual recording duration
    val startMs: Long,
    val endMs: Long,
)

@JsonClass(generateAdapter = true)
class ClimbingStateHistoryItem(
    val climbingState: ClimbingState,
    val timestamp: Long,
)