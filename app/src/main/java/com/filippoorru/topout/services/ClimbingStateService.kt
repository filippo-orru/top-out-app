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

        var attemptStart: Long? = null

        var flickerStart: ClimbingStateHistoryItem? = null
        var flickerLast: ClimbingStateHistoryItem? = null

        for (now in climbingStateHistory) {
            if (flickerStart == null) {
                flickerStart = now
            } else if (
                flickerLast != null &&
                now.timestamp - flickerLast.timestamp > minDuration
            ) {
                // Save start or end of attempt
                if (attemptStart == null && flickerLast.climbing) {
                    attemptStart = flickerStart.timestamp
                } else if (attemptStart != null && !flickerLast.climbing) {
                    attempts.add(
                        Attempt(
                            startMs = attemptStart - startTimestamp,
                            endMs = flickerLast.timestamp - startTimestamp
                        )
                    )
                }

                flickerStart = now
                flickerLast = null
            } else {
                // Flickering
                flickerLast = now
            }
        }

        if (attemptStart != null && flickerLast?.climbing == false) {
            attempts.add(
                Attempt(
                    startMs = attemptStart - startTimestamp,
                    endMs = flickerLast.timestamp - startTimestamp
                )
            )
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