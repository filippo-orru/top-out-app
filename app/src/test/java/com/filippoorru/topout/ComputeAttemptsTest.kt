package com.filippoorru.topout

import com.filippoorru.topout.services.Attempt
import com.filippoorru.topout.services.ClimbingStateHistoryItem
import com.filippoorru.topout.services.ClimbingStateService
import com.filippoorru.topout.ui.model.ClimbingState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ComputeAttemptsTest {
    private fun getAttempts(
        vararg list: Pair<ClimbingState, Long>,
    ): List<Attempt> {
        val service = ClimbingStateService()

        list.forEach {
            service.onNewClimbingState(it.first, it.second)
        }

        return service.getAttempts(0L, list.last().second)
    }

    @Test
    fun simple() {
        val attempts = getAttempts(
            ClimbingState.Idle to 1000L,
            ClimbingState.Climbing to 5000L,
            ClimbingState.Idle to 10000L
        )

        assertEquals(
            listOf(Attempt(5000L, 10000L)),
            attempts
        )
    }

    @Test
    fun withFlicker() {
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

    inline fun <reified T> Moshi.parseList(jsonString: String): List<T>? {
        return adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java)).fromJson(jsonString)
    }

    @Test
    fun checkJson() {
        val raw = """[
  {
    "climbing": false,
    "timestamp": 1718799624875
  },
  {
    "climbing": false,
    "timestamp": 1718799624934
  },
  {
    "climbing": false,
    "timestamp": 1718799625007
  },
  {
    "climbing": false,
    "timestamp": 1718799625090
  },
  {
    "climbing": false,
    "timestamp": 1718799625133
  },
  {
    "climbing": false,
    "timestamp": 1718799625200
  },
  {
    "climbing": false,
    "timestamp": 1718799625310
  },
  {
    "climbing": false,
    "timestamp": 1718799625324
  },
  {
    "climbing": false,
    "timestamp": 1718799625411
  },
  {
    "climbing": false,
    "timestamp": 1718799625468
  },
  {
    "climbing": false,
    "timestamp": 1718799625530
  },
  {
    "climbing": false,
    "timestamp": 1718799625581
  },
  {
    "climbing": false,
    "timestamp": 1718799625597
  },
  {
    "climbing": false,
    "timestamp": 1718799625641
  },
  {
    "climbing": false,
    "timestamp": 1718799625686
  },
  {
    "climbing": false,
    "timestamp": 1718799625742
  },
  {
    "climbing": false,
    "timestamp": 1718799625815
  },
  {
    "climbing": false,
    "timestamp": 1718799625860
  },
  {
    "climbing": false,
    "timestamp": 1718799625925
  },
  {
    "climbing": false,
    "timestamp": 1718799626136
  },
  {
    "climbing": false,
    "timestamp": 1718799626152
  },
  {
    "climbing": false,
    "timestamp": 1718799626202
  },
  {
    "climbing": false,
    "timestamp": 1718799626313
  },
  {
    "climbing": false,
    "timestamp": 1718799626387
  },
  {
    "climbing": false,
    "timestamp": 1718799626450
  },
  {
    "climbing": false,
    "timestamp": 1718799626497
  },
  {
    "climbing": false,
    "timestamp": 1718799626555
  },
  {
    "climbing": false,
    "timestamp": 1718799626578
  },
  {
    "climbing": false,
    "timestamp": 1718799626622
  },
  {
    "climbing": false,
    "timestamp": 1718799626660
  },
  {
    "climbing": false,
    "timestamp": 1718799626711
  },
  {
    "climbing": false,
    "timestamp": 1718799626760
  },
  {
    "climbing": false,
    "timestamp": 1718799626861
  },
  {
    "climbing": false,
    "timestamp": 1718799626868
  },
  {
    "climbing": false,
    "timestamp": 1718799626936
  },
  {
    "climbing": false,
    "timestamp": 1718799627000
  },
  {
    "climbing": false,
    "timestamp": 1718799627105
  },
  {
    "climbing": false,
    "timestamp": 1718799627111
  },
  {
    "climbing": false,
    "timestamp": 1718799627187
  },
  {
    "climbing": false,
    "timestamp": 1718799627260
  },
  {
    "climbing": false,
    "timestamp": 1718799627350
  },
  {
    "climbing": false,
    "timestamp": 1718799627353
  },
  {
    "climbing": false,
    "timestamp": 1718799627440
  },
  {
    "climbing": false,
    "timestamp": 1718799627507
  },
  {
    "climbing": false,
    "timestamp": 1718799627600
  },
  {
    "climbing": false,
    "timestamp": 1718799627603
  },
  {
    "climbing": false,
    "timestamp": 1718799627651
  },
  {
    "climbing": false,
    "timestamp": 1718799627712
  },
  {
    "climbing": false,
    "timestamp": 1718799627770
  },
  {
    "climbing": false,
    "timestamp": 1718799627844
  },
  {
    "climbing": false,
    "timestamp": 1718799627852
  },
  {
    "climbing": false,
    "timestamp": 1718799627912
  },
  {
    "climbing": false,
    "timestamp": 1718799627964
  },
  {
    "climbing": false,
    "timestamp": 1718799628073
  },
  {
    "climbing": false,
    "timestamp": 1718799628088
  },
  {
    "climbing": false,
    "timestamp": 1718799628125
  },
  {
    "climbing": false,
    "timestamp": 1718799628184
  },
  {
    "climbing": false,
    "timestamp": 1718799628245
  },
  {
    "climbing": false,
    "timestamp": 1718799628323
  },
  {
    "climbing": false,
    "timestamp": 1718799628327
  },
  {
    "climbing": false,
    "timestamp": 1718799628377
  },
  {
    "climbing": true,
    "timestamp": 1718799628489
  },
  {
    "climbing": true,
    "timestamp": 1718799628570
  },
  {
    "climbing": true,
    "timestamp": 1718799628583
  },
  {
    "climbing": true,
    "timestamp": 1718799628641
  },
  {
    "climbing": true,
    "timestamp": 1718799628748
  },
  {
    "climbing": true,
    "timestamp": 1718799628866
  },
  {
    "climbing": true,
    "timestamp": 1718799628875
  },
  {
    "climbing": false,
    "timestamp": 1718799628937
  },
  {
    "climbing": false,
    "timestamp": 1718799629035
  },
  {
    "climbing": false,
    "timestamp": 1718799629101
  },
  {
    "climbing": false,
    "timestamp": 1718799629137
  },
  {
    "climbing": false,
    "timestamp": 1718799629162
  },
  {
    "climbing": false,
    "timestamp": 1718799629218
  },
  {
    "climbing": true,
    "timestamp": 1718799629267
  },
  {
    "climbing": true,
    "timestamp": 1718799629364
  },
  {
    "climbing": true,
    "timestamp": 1718799629371
  },
  {
    "climbing": true,
    "timestamp": 1718799629402
  },
  {
    "climbing": true,
    "timestamp": 1718799629467
  },
  {
    "climbing": true,
    "timestamp": 1718799629534
  },
  {
    "climbing": true,
    "timestamp": 1718799629617
  },
  {
    "climbing": true,
    "timestamp": 1718799629621
  },
  {
    "climbing": true,
    "timestamp": 1718799629682
  },
  {
    "climbing": true,
    "timestamp": 1718799629736
  },
  {
    "climbing": true,
    "timestamp": 1718799629786
  },
  {
    "climbing": true,
    "timestamp": 1718799629854
  },
  {
    "climbing": true,
    "timestamp": 1718799629884
  },
  {
    "climbing": true,
    "timestamp": 1718799629962
  },
  {
    "climbing": true,
    "timestamp": 1718799630022
  },
  {
    "climbing": false,
    "timestamp": 1718799631086
  }
]"""

        val moshi = Moshi.Builder().build()
        val history = moshi.parseList<ClimbingStateHistoryItem>(raw)!!

        val service = ClimbingStateService()

        history.forEach {
            val state = if (it.climbing) {
                ClimbingState.Climbing
            } else {
                ClimbingState.Idle
            }
            service.onNewClimbingState(state, it.timestamp)
        }

        val attempts = service.getAttempts(0L, 1718799631086L)

        println(attempts)
    }
}