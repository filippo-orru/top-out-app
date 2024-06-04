package com.filippoorru.topout.utils

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

inline fun <T> measureTimeMillis(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    return result to System.currentTimeMillis() - start
}

const val zero = 0.toByte()

suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProvider = ProcessCameraProvider.getInstance(context)
    cameraProvider.addListener({ continuation.resume(cameraProvider.get()) }, ContextCompat.getMainExecutor(context))
}

fun <T> List<T>.emptyToNull(): List<T>? = ifEmpty { null }