package com.filippoorru.topout.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filippoorru.topout.database.Database
import kotlinx.coroutines.launch

class ViewAttemptModel(
    val routeVisitId: String,
    val attemptId: String,
) : ViewModel() {
    fun delete() {
        viewModelScope.launch {
            Database.i.attempts().delete(attemptId)
        }
    }

    val routeVisit = Database.i.routeVisits().get(routeVisitId)
    val attempt = Database.i.attempts().get(attemptId)
}