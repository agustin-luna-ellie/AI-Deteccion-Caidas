package io.fallcare.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.fallcare.R
import io.fallcare.presentation.MainActivity


fun Context.createForegroundNotification(): Notification {

    val notificationChannel = getNotificationChannel()
    val notificationManager: NotificationManager =
        this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(notificationChannel)

    val intent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificationBuilder = NotificationCompat.Builder(this, notificationChannel.id)
        .setContentTitle("Detección activa")
        .setContentText("${getString(R.string.app_name)} está monitoreando posibles caídas.")
        .setSmallIcon(R.drawable.ic_fall)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setFullScreenIntent(pendingIntent, true)
        .setOngoing(true)
        .setSilent(true)

    return notificationBuilder.build()

    /*
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

    */

}



