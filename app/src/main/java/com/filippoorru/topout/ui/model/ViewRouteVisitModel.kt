package com.filippoorru.topout.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filippoorru.topout.database.AttemptEntity
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.database.PartOfRouteVisitRecording
import kotlinx.coroutines.launch

class ViewRouteVisitModel(
    val routeVisitId: String,
) : ViewModel() {
    val routeVisits = Database.i.routeVisits().get(routeVisitId)
    val attempts = Database.i.attempts().getByRouteVisit(routeVisitId)

    fun createNewAttempt(index: Int): String {
        val id = "attempt-$routeVisitId-$index"
        viewModelScope.launch {
            Database.i.attempts().save(
                AttemptEntity(
                    id = id,
                    routeVisitId = routeVisitId,
                    partOfRouteVisitRecording = PartOfRouteVisitRecording(
                        startMs = 0,
                        endMs = 5_000,
                    ),
                )
            )
        }
        return id
    }
}