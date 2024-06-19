package com.filippoorru.topout.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
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

@Entity(
    tableName = "attempts",
    foreignKeys = [ForeignKey(
        entity = RouteVisitEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("routeVisitId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class AttemptEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(index = true)
    var routeVisitId: String,

//    @Embedded
//    val recording: AttemptRecording?,

    @Embedded
    val partOfRouteVisitRecording: PartOfRouteVisitRecording,

//    var result: String, // top / fall / ?
)

class AttemptRecording(
    val filePath: String,
    val durationMs: Long,
)

data class PartOfRouteVisitRecording(
    val startMs: Long,
    val endMs: Long,
)