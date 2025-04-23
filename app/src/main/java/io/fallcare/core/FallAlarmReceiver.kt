package io.fallcare.core

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.fallcare.R
import io.fallcare.presentation.MainActivity
import io.fallcare.util.logger

class FallAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("FALL_DETECTED", true)
        }

        try {
            context.startActivity(launchIntent)
            logger(".", "FallAlarmReceiver.onReceive.startActivity")
        } catch (e: Exception) {
            logger(".", "FallAlarmReceiver.onReceive.showFallbackNotification")
            // Fallback: mostrar notificación si no se puede abrir la actividad
            showFallbackNotification(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showFallbackNotification(context: Context) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "fall_detection_channel")
            .setSmallIcon(R.drawable.splash_icon)
            .setContentTitle("Posible caída detectada")
            .setContentText("Toca para abrir la aplicación")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(2001, notification)
        }
    }

}
