package com.filippoorru.topout

import com.filippoorru.topout.services.Attempt
import com.filippoorru.topout.services.ClimbingStateService
import com.filippoorru.topout.ui.model.ClimbingState
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

        val history = service.getAttempts(0L, 12000L)

        assertEquals(
            listOf(
                Attempt(5000L, 11600L),
            ),
            history
        )
    }
}