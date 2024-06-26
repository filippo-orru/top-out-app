package com.filippoorru.topout.services

import com.filippoorru.topout.ui.model.ClimbingState
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

class ClimbingStateService {
    private val climbingStateHistory = mutableListOf<ClimbingStateHistoryItem>()

    fun onNewClimbingState(climbingState: ClimbingState, timestamp: Long) {
        val climbing = climbingState == ClimbingState.Climbing
        if (climbingStateHistory.isNotEmpty() && climbingStateHistory.last().climbing == climbing) {
            return
        }

        climbingStateHistory.add(
            ClimbingStateHistoryItem(
                climbing = climbing,
                timestamp
            )
        )
    }

    fun getAttempts(startTimestamp: Long, endTimestamp: Long): List<Attempt> {
        if (climbingStateHistory.isEmpty()) {
            return emptyList()
        }

        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter<List<ClimbingStateHistoryItem>>(List::class.java)

        println("climbingStateHistory")
        println(
            jsonAdapter.toJson(climbingStateHistory.toList())
        )

        val minDuration = 1000L

        val attempts = mutableListOf<Attempt>()

        var attemptStart: Long? = null

        val flicker: MutableList<ClimbingStateHistoryItem> = mutableListOf()

        for (now in climbingStateHistory) {
            val flickerLast = flicker.lastOrNull()
            if (flickerLast == null || now.timestamp - flickerLast.timestamp < minDuration) {
                flicker.add(now)
            } else {
                // Save start or end of attempt
                if (attemptStart == null && flickerLast.climbing) {
                    attemptStart = flicker.first().timestamp
                } else if (attemptStart != null && !flickerLast.climbing) {
                    attempts.add(
                        Attempt(
                            startMs = attemptStart - startTimestamp,
                            endMs = flickerLast.timestamp - startTimestamp
                        )
                    )
                }

                flicker.clear()
                flicker.add(now)
            }
        }

        if (attemptStart != null) {
            val end = if (flicker.last().climbing) {
                endTimestamp
            } else {
                flicker.last().timestamp
            }
            attempts.add(
                Attempt(
                    startMs = attemptStart - startTimestamp,
                    endMs = end - startTimestamp // End of recording
                )
            )
        }

        println("attempts")
        println(
            moshi.adapter<List<Attempt>>(List::class.java).toJson(attempts)
        )

        return attempts
    }
}

@JsonClass(generateAdapter = true)
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