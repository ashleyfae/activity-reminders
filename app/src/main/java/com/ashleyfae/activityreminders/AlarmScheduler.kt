package com.ashleyfae.activityreminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule() {
        val prefs = PreferencesManager(context)
        if (!prefs.isEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return

        val nextMs = calculateNextAlarmMs(prefs.startHour, prefs.endHour) ?: return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMs,
            buildPendingIntent()
        )
    }

    fun cancel() {
        alarmManager.cancel(buildPendingIntent())
    }

    private fun calculateNextAlarmMs(startHour: Int, endHour: Int): Long? {
        val now = Calendar.getInstance()
        val candidate = (now.clone() as Calendar).apply {
            set(Calendar.MINUTE, 50)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (now.get(Calendar.MINUTE) >= 50) add(Calendar.HOUR_OF_DAY, 1)
        }

        repeat(25) {
            if (candidate.get(Calendar.HOUR_OF_DAY) in startHour until endHour) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.HOUR_OF_DAY, 1)
        }
        return null
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
