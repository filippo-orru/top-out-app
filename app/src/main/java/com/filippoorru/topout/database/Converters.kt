package com.filippoorru.topout.database

import androidx.room.TypeConverter
import com.filippoorru.topout.services.ClimbingStateHistoryItem
import com.squareup.moshi.Moshi

class Converters {
    private val moshi = Moshi.Builder().build()
    private val jsonAdapter = moshi.adapter<List<ClimbingStateHistoryItem>>(List::class.java)

    @TypeConverter
    fun listClimbingHistoryToJsonString(list: List<ClimbingStateHistoryItem>?): String? {
        return jsonAdapter.toJson(list)
    }

    @TypeConverter
    fun listClimbingHistoryFromJsonString(jsonString: String?): List<ClimbingStateHistoryItem>? {
        return jsonString?.let { jsonAdapter.fromJson(jsonString) }
    }
}