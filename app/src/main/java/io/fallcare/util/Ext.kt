package io.fallcare.util


import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Secure
import android.util.Log
import io.fallcare.BuildConfig
import io.fallcare.presentation.MainActivity
import io.fallcare.core.FallDetectionService
import io.fallcare.util.Constants.FALL_DETECTED_DATA
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import io.fallcare.util.Constants.PROB_FALL_KEY
import io.fallcare.util.Constants.SAMPLING_PERIOD_KEY
import io.fallcare.util.Constants.SEQUENCE_LENGTH_KEY
import io.fallcare.util.Constants.SETTINGS_KEY


inline val Context.androidID: String
    @SuppressLint("HardwareIds")
    get() = Secure.getString(this.contentResolver, Secure.ANDROID_ID)

inline val appTimeStamp: Long
    get() = System.currentTimeMillis()

inline val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager


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

    return inputStream.use { inputStream ->
        inputStream.channel.map(
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


fun Context.createNotificationChannel() {
    val channelId = "notification_channel"
    val channelName = "Notifications"
    val channelDescription = "notification channel"
    val notificationChannel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = channelDescription
    }

    val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    notificationManager.createNotificationChannel(notificationChannel)
}


fun logger(tag: String = "", msg: String) = run {
    if (BuildConfig.DEBUG) Log.d("stack_Trace_$tag", msg)
}


@SuppressLint("WearRecents")
private fun launchActivityWithAlarm(context: Context, alarmManager: AlarmManager) {

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(
            FALL_DETECTED_DATA,
            true
        )
    }.also {
        logger(".", "ScheduleExactAlarm.create.Intent")
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    ).also {
        logger(".", "ScheduleExactAlarm.create.pendingIntent")
    }

    val triggerAtMillis = appTimeStamp + 6000 // X segundos después,
    logger(".", "ScheduleExactAlarm.create.triggerAtMillis: $triggerAtMillis")

    alarmManager.cancel(pendingIntent)
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
    )
}


fun FallDetectionService.launchActivity() {
    val checkScheduleAlarm = checkScheduleExactAlarmPermission(alarmManager)
    logger("ScheduleExactAlarm", "launchActivity.checkScheduleAlarm: $checkScheduleAlarm")
    if (checkScheduleAlarm)
        launchActivityWithAlarm(alarmManager = alarmManager, context = this)
    else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent().also { intent ->
                intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}


private fun checkScheduleExactAlarmPermission(alarmManager: AlarmManager): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        alarmManager.canScheduleExactAlarms()
    else
        true // Permitido en versiones anteriores

}


fun FallDetectionService.bringMainActivityToFront() {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val isMainActivityRunning = activityManager.runningAppProcesses
        ?.any {
            it.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == packageName
        } ?: false

    if (!isMainActivityRunning) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("FALL_DETECTED", true)
        }
        startActivity(intent)
        logger("FallDetectionService", "✔ bringMainActivityToFront.MainActivity relanzada")
    } else {
        logger("FallDetectionService", "ℹ bringMainActivityToFront.MainActivity ya está activa")
    }
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
