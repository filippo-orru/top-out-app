package com.filippoorru.topout.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

object Database {
    lateinit var i: AppDatabase

    fun init(context: Context) {
        i = Room.databaseBuilder(context, AppDatabase::class.java, "app-database")
            .fallbackToDestructiveMigration()
            .build()
    }
}

@Database(
    entities = [
        RouteEntity::class,
        RouteVisitEntity::class,
        AttemptEntity::class,
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routes(): RoutesCollection

    abstract fun routeVisits(): RouteVisitsCollection
}

@Dao
interface RoutesCollection {
    @Insert
    suspend fun save(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE id = :id")
    fun get(id: String): Flow<RouteEntity?>

    @Query("SELECT * FROM routes")
    fun getAll(): Flow<List<RouteEntity>>
}

@Dao
interface RouteVisitsCollection {
    @Insert
    suspend fun save(routeVisit: RouteVisitEntity)

    @Query("SELECT * FROM routeVisits WHERE id = :id")
    fun get(id: String): Flow<RouteVisitEntity?>

    @Query("SELECT * FROM routeVisits")
    fun getAll(): Flow<List<RouteVisitEntity>>
}

@Dao
interface AttemptsCollection {
    @Insert
    fun save(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE id = :id")
    fun get(id: String): Flow<AttemptEntity?>

    @Query("SELECT * FROM attempts")
    fun getAll(): Flow<List<AttemptEntity>>
}

