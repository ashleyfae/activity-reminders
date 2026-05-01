package com.ashleyfae.activityreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)
        if (!prefs.isEnabled) return

        NotificationHelper(context).showReminder()
        AlarmScheduler(context).schedule()
    }
}
