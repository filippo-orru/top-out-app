package com.filippoorru.topout.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

object Database {
    lateinit var i: AppDatabase

    fun init(context: Context) {
        i = Room.databaseBuilder(context, AppDatabase::class.java, "app-database")
            .build()
    }
}

@Database(
    entities = [
        RouteVisitEntity::class,
        AttemptEntity::class,
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeVisits(): RouteVisitsCollection

    abstract fun attempts(): AttemptsCollection
}

@Dao
interface RouteVisitsCollection {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(routeVisit: RouteVisitEntity)

    @Query("SELECT * FROM routeVisits WHERE id = :id")
    fun get(id: String): Flow<RouteVisitEntity?>

    @Query("SELECT * FROM routeVisits")
    fun getAll(): Flow<List<RouteVisitEntity>>

    @Query("DELETE FROM routeVisits WHERE id = :routeVisitId")
    suspend fun delete(routeVisitId: String)
}

@Dao
interface AttemptsCollection {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(attempt: AttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(attempts: List<AttemptEntity>)

    @Query("SELECT * FROM attempts WHERE id = :id")
    fun get(id: String): Flow<AttemptEntity?>

    @Query("SELECT * FROM attempts WHERE routeVisitId = :routeVisitId")
    fun getByRouteVisit(routeVisitId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts")
    fun getAll(): Flow<List<AttemptEntity>>

    @Query("DELETE FROM attempts WHERE id = :attemptId")
    suspend fun delete(attemptId: String)
}

