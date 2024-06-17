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
        i = Room.databaseBuilder(context, AppDatabase::class.java, "app-database").build()
    }
}

@Database(entities = [RouteEntity::class, RouteVisitEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routes(): RoutesCollection

    abstract fun routeVisits(): RouteVisitsCollection
}

@Dao
interface RoutesCollection {
    @Insert
    fun save(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE id = :id")
    fun get(id: Int): Flow<RouteEntity?>

    @Query("SELECT * FROM routes")
    fun getAll(): Flow<List<RouteEntity>>
}

@Dao
interface RouteVisitsCollection {
    @Insert
    fun save(routeVisit: RouteVisitEntity)

    @Query("SELECT * FROM routeVisits WHERE id = :id")
    fun get(id: Int): Flow<RouteVisitEntity?>

    @Query("SELECT * FROM routeVisits")
    fun getAll(): Flow<List<RouteVisitEntity>>
}

