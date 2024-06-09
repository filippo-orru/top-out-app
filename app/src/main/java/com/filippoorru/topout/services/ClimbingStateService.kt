package com.filippoorru.topout.services

import com.filippoorru.topout.model.ClimbingState

class ClimbingStateService {
    private val climbingStateHistory = mutableListOf<ClimbingStateHistoryItem>()

    fun onNewClimbingState(climbingState: ClimbingState, timestamp: Long) {
        climbingStateHistory.add(ClimbingStateHistoryItem(climbingState, timestamp))
    }

    fun getClimbingStateHistory(
        startTimestamp: Long,
    ): List<ClimbingStateHistoryItem> {
        val minDuration = 1000L

        val computed = mutableListOf<ClimbingStateHistoryItem>()

        var first = ClimbingStateHistoryItem(ClimbingState.Idle, startTimestamp)
        var last: ClimbingStateHistoryItem? = null

        val history = climbingStateHistory + ClimbingStateHistoryItem(ClimbingState.Idle, Long.MAX_VALUE)

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

                    computed.add(
                        ClimbingStateHistoryItem(
                            climbingState = last.climbingState,
                            timestamp = timestamp
                        )
                    )
                }
                first = climb
                last = null
            } else {
                last = climb
            }
        }

        return computed.toList()
    }
}

class ClimbingStateHistoryItem(
    val climbingState: ClimbingState,
    val timestamp: Long,
)