package com.filippoorru.topout.utils

import android.content.Context
import android.provider.MediaStore

class VideoFile(
    val id: Long,
    val name: String
)

fun getAllVideos(context: Context): List<VideoFile>? {
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME
    )

    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    return context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        null,//projection,
        null,
        null,
        null//sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

        generateSequence {
            if (cursor.moveToNext()) {
                VideoFile(cursor.getLong(idColumn), cursor.getString(nameColumn))
            } else {
                null
            }
        }.toList()
    }
}