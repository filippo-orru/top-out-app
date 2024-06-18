package com.filippoorru.topout.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routeVisits")
class RouteVisitEntity(
    @PrimaryKey
    val id: String,

    @Embedded
    var recording: RouteVisitRecording,

//    val routeId: String,
    val timestamp: Long,
//    var notes: String,
)

class RouteVisitRecording(
    val filePath: String,
)

@Entity(tableName = "attempts")
class AttemptEntity(
    @PrimaryKey
    val id: String,

    //@Relation(parentColumn = "id", entityColumn = "routeVisitId")
    var routeVisitId: String,

//    @Embedded
//    val recording: AttemptRecording?,

    @Embedded
    val partOfRouteVisitRecording: PartOfRouteVisitRecording?,

//    var result: String, // top / fall / ?
)

class AttemptRecording(
    val filePath: String,
    val durationMs: Long,
)

class PartOfRouteVisitRecording(
    val startMs: Long,
    val endMs: Long,
)