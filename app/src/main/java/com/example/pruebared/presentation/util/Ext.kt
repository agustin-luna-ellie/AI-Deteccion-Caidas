package com.example.pruebared.presentation.util

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


@Throws(IOException::class)
fun Context.loadModelFileMapped(): ByteBuffer {
    val assetManager = assets
    val fileDescriptor = assetManager.openFd("modelo_convertido.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor).apply {
        channel.position(fileDescriptor.startOffset)
    }

    return inputStream.use { inputStream ->
        inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        ).also { buffer ->
            buffer.order(ByteOrder.nativeOrder())
            Log.d(
                "ModelLoading",
                "Modelo mapeado correctamente. Tamaño: ${buffer.capacity()} bytes"
            )
        }
    }
}

fun verifyModel(assetManager: AssetManager): Boolean {
    return try {
        val inputStream = assetManager.open("modelo_convertido.tflite")
        val bytes = inputStream.readBytes()
        bytes.isNotEmpty() // Verificación básica
    } catch (e: Exception) {
        false
    }
}