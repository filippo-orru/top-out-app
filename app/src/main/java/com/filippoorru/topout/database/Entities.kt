package com.filippoorru.topout.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.filippoorru.topout.services.ClimbingStateHistoryItem

@Entity(
    tableName = "routes"
)
class RouteEntity(
    @PrimaryKey
    val id: String,
    val image: String,
//    var name: String,
//    var difficulty: String,
//    var notes: String,
)

@Entity(
    tableName = "routeVisits",
//    foreignKeys = [ForeignKey(
//        entity = Route::class,
//        parentColumns = arrayOf("id"),
//        childColumns = arrayOf("route"),
//        onDelete = ForeignKey.CASCADE
//    )]
)
class RouteVisitEntity(
    @PrimaryKey
    val id: String,

    @Embedded
    var recording: RouteVisitRecording?,

//    val routeId: String,
    val timestamp: Long,
//    var notes: String,
)

class RouteAndVisits(
    @Embedded
    val route: RouteEntity,
    @Relation(parentColumn = "id", entityColumn = "routeId")
    val visits: List<RouteVisitEntity>,
)

class RouteVisitRecording(
    val filePath: String,

    val climbingStateHistory: List<ClimbingStateHistoryItem>,
)

@Entity(
    tableName = "attempts",
//    foreignKeys = [ForeignKey(
//        entity = RouteVisitEntity::class,
//        parentColumns = arrayOf("id"),
//        childColumns = arrayOf("routeVisitId"),
//        onDelete = ForeignKey.CASCADE
//    )]
)
class AttemptEntity(
    @PrimaryKey
    val id: String,

    //@Relation(parentColumn = "id", entityColumn = "routeVisitId")
    var routeVisitId: String,

    @Embedded
    val recording: AttemptRecording,

//    var result: String, // top / fall / ?
)

class AttemptRecording(
    val filePath: String,
    val durationMs: Long,
)

// not sure how to use this
class Attempt(
    @Embedded
    val attempt: AttemptEntity,
    @Relation(parentColumn = "attemptId", entityColumn = "attemptId")
    val routeVisit: RouteVisitEntity,
)