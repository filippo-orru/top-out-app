package com.filippoorru.topout.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.database.RouteVisitEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppViewModel : ViewModel() {
    val routeVisits: StateFlow<List<RouteVisitEntity>?> =
        Database.i.routeVisits().getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = null
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}