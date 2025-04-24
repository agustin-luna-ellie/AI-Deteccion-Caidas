package io.fallcare.util


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.provider.Settings.Secure
import android.util.Log
import androidx.core.app.NotificationCompat
import io.fallcare.BuildConfig
import io.fallcare.R
import io.fallcare.presentation.MainActivity
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import io.fallcare.util.Constants.PROB_FALL_KEY
import io.fallcare.util.Constants.SAMPLING_PERIOD_KEY
import io.fallcare.util.Constants.SEQUENCE_LENGTH_KEY
import io.fallcare.util.Constants.SETTINGS_KEY
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


inline val Context.androidID: String
    @SuppressLint("HardwareIds")
    get() = Secure.getString(this.contentResolver, Secure.ANDROID_ID)

inline val appTimeStamp: Long
    get() = System.currentTimeMillis()

fun verifyModel(assetManager: AssetManager): Boolean {
    return try {
        val inputStream = assetManager.open("modelo_convertido.tflite")
        val bytes = inputStream.readBytes()
        bytes.isNotEmpty() // Verificación básica
    } catch (e: Exception) {
        false
    }
}


@Throws(IOException::class)
fun Context.loadModelFileMapped(): ByteBuffer {
    val assetManager = assets
    val fileDescriptor = assetManager.openFd("modelo_convertido.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor).apply {
        channel.position(fileDescriptor.startOffset)
    }

    return inputStream.use { stream ->
        stream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        ).also { buffer ->
            buffer.order(ByteOrder.nativeOrder())
            logger(
                "ModelLoading",
                "Modelo mapeado correctamente. Tamaño: ${buffer.capacity()} bytes"
            )
        }
    }
}

fun getNotificationChannel(): NotificationChannel = NotificationChannel(
    "notification_channel",
    "Notifications",
    NotificationManager.IMPORTANCE_HIGH
).apply {
    description = "notification channel"
}


fun logger(tag: String = "", msg: String) = run {
    if (BuildConfig.DEBUG) Log.d("stack_Trace_$tag", msg)
}

fun Context.saveSettings(sequenceLength: Int, samplingPeriod: Int, probFall: Float) {
    val sharedPref = getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putInt(SEQUENCE_LENGTH_KEY, sequenceLength)
        putInt(SAMPLING_PERIOD_KEY, samplingPeriod)
        putFloat(PROB_FALL_KEY, probFall)
        apply()
    }
}

@SuppressLint("WearRecents")
fun Context.launchMainActivity() {

    val fullScreenPendingIntent = PendingIntent.getActivity(
        this,
        4636,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationChannel = getNotificationChannel()

    val notification: Notification = NotificationCompat.Builder(this, notificationChannel.id)
        .setContentTitle("Detección activa")
        .setContentText("Posible caída detectada")
        .setSmallIcon(R.drawable.ic_fall)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setAutoCancel(true)
        .setSilent(true)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .build()

    val mNotificationManager = getSystemService(NotificationManager::class.java)
    mNotificationManager.createNotificationChannel(notificationChannel)
    NotificationCompat.Builder(this, notificationChannel.id)

    runBlocking {
        launch {
            delay(1000L)
            mNotificationManager.notify(1, notification)
            delay(1000L)
            mNotificationManager.cancel(1)
        }
    }

}

