package com.filippoorru.topout.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.database.RouteVisitEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainScreenViewModel : ViewModel() {
    private val simpleDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    val attempts = Database.i.attempts().getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val routeVisits: StateFlow<List<VisitsOnDay>?> =
        Database.i.routeVisits().getAll()
            .map { visits ->
                visits
                    .sortedByDescending { it.timestamp }
                    .groupBy { simpleDateFormat.format(Date(it.timestamp)) }
                    .map { (date, visits) ->
                        VisitsOnDay(date, visits)
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = null
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class VisitsOnDay(
    val date: String,
    val visits: List<RouteVisitEntity>,
)