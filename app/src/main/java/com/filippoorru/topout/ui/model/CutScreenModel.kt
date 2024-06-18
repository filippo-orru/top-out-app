package com.filippoorru.topout.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.filippoorru.topout.database.AttemptEntity
import com.filippoorru.topout.database.Database
import kotlinx.coroutines.launch

class CutScreenModel(
    routeVisitId: String,
    attemptId: String,
) : ViewModel() {
    fun cut(attempt: AttemptEntity, start: Long, end: Long) {
        fun cutFile(path: String, newPath: String, startTimestamp: Int, endTimestamp: Int) {
            val session: FFmpegSession = FFmpegKit.execute("-y -i $path -ss $startTimestamp -to $endTimestamp -c copy $newPath")
            when (session.returnCode.value) {
                ReturnCode.SUCCESS -> println("Cutting successful")
                ReturnCode.CANCEL -> println("Cutting cancelled")
                else -> println("Cutting failed")
            }
        }

        viewModelScope.launch {
            Database.i.attempts().save(
                attempt.copy(
                    partOfRouteVisitRecording = attempt.partOfRouteVisitRecording?.copy(
                        startMs = start,
                        endMs = end
                    )
                )
            )

        }
    }

    val routeVisit = Database.i.routeVisits().get(routeVisitId)
    val attempt = Database.i.attempts().get(attemptId)
}