package com.filippoorru.topout.ui.model

import androidx.lifecycle.ViewModel
import com.filippoorru.topout.database.Database

class CutScreenModel(
    val routeVisitId: String,
    val attemptId: String,
) : ViewModel() {
    val routeVisit = Database.i.routeVisits().get(routeVisitId)
    val attempt = Database.i.attempts().get(attemptId)
}