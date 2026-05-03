package com.ashleyfae.activityreminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    init {
        createChannel()
    }

    fun showReminder(remainingSteps: Long? = null) {
        val body = if (remainingSteps != null) {
            "You've been sitting a while — take $remainingSteps more steps to hit your goal."
        } else {
            "You've been sitting a while — get up and stretch."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to move!")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        vibrate()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Activity Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Hourly reminders to get up and move"
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun vibrate() {
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java).vibrate(effect)
        }
    }

    companion object {
        const val CHANNEL_ID = "activity_reminders"
        const val NOTIFICATION_ID = 2001
        private val VIBRATION_PATTERN = longArrayOf(0, 400, 150, 400, 150, 600)
    }
}
