package com.filippoorru.topout

import com.filippoorru.topout.model.ClimbingState
import com.filippoorru.topout.services.ClimbingStateService
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ClimbingStateServiceTest {
    @Test
    fun testClimbingStateHistory() {
        val service = ClimbingStateService()

        // Initial idle
        service.onNewClimbingState(ClimbingState.Idle, 1000L)
        service.onNewClimbingState(ClimbingState.Climbing, 1500L)
        service.onNewClimbingState(ClimbingState.Idle, 2200L)

        // Start climbing
        service.onNewClimbingState(ClimbingState.Climbing, 5000L)
        service.onNewClimbingState(ClimbingState.Idle, 5500L)
        service.onNewClimbingState(ClimbingState.Climbing, 6200L)

        // This should be ignored
        service.onNewClimbingState(ClimbingState.Idle, 8500L)
        service.onNewClimbingState(ClimbingState.Climbing, 9000L)

        // Stop climbing
        service.onNewClimbingState(ClimbingState.Idle, 11000L)
        service.onNewClimbingState(ClimbingState.Climbing, 11100L)
        service.onNewClimbingState(ClimbingState.Idle, 11600L)

        val history = service.getClimbingStateHistory(0L)

        assertEquals(3, history.size)

        assertEquals(ClimbingState.Idle, history[0].climbingState)
        assertEquals(0L, history[0].timestamp)

        assertEquals(ClimbingState.Climbing, history[1].climbingState)
        assertEquals(5400L, history[1].timestamp)

        assertEquals(ClimbingState.Idle, history[2].climbingState)
        assertEquals(11200L, history[2].timestamp)
    }
}