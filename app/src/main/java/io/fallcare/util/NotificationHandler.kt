package io.fallcare.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.core.content.LocusIdCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import io.fallcare.R
import io.fallcare.presentation.MainActivity
import io.fallcare.util.Constants.NOTIFICATION_ID


fun Context.createForegroundNotification(): Notification {
    val channelId = "fall_detection_channel"

    val name = "Detección de Caídas"
    val descriptionText = "Notificación de detección de caídas en curso"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel(channelId, name, importance).apply {
        description = descriptionText
    }
    val notificationManager: NotificationManager =
        this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)

    val intent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificationBuilder = NotificationCompat.Builder(this, channelId)
        .setContentTitle("Detección activa")
        .setContentText("${getString(R.string.app_name)} está monitoreando posibles caídas.")
        .setSmallIcon(R.drawable.ic_fall)
        //.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setFullScreenIntent(pendingIntent, true)
        .setOngoing(true)
        .setSilent(true)

    val notification = notificationBuilder.build()

    val ongoingActivityStatus = Status.Builder()
        .addTemplate("Monitoreando...")
        .build()

    val ongoingActivity = OngoingActivity.Builder(this, NOTIFICATION_ID, notificationBuilder)
        .setStaticIcon(Icon.createWithResource(this, R.drawable.ic_fall))
        .setTouchIntent(pendingIntent)
        .setStatus(ongoingActivityStatus)
        .setAnimatedIcon(null)
        .setLocusId(LocusIdCompat("fall_detection"))
        .build()

    ongoingActivity.apply(this)

    return notification
}



