package com.ashleyfae.activityreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)
        if (!prefs.isEnabled) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val hc = HealthConnectManager(context)
                if (hc.isAvailable() && hc.hasPermission()) {
                    val steps = hc.getStepsLastHour()
                    val remaining = prefs.stepThreshold - steps
                    if (remaining > 0) NotificationHelper(context).showReminder(remaining)
                } else {
                    NotificationHelper(context).showReminder()
                }
            } catch (e: Exception) {
                NotificationHelper(context).showReminder()
            } finally {
                AlarmScheduler(context).schedule()
                pendingResult.finish()
            }
        }
    }
}
