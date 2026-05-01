package com.ashleyfae.activityreminders

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("activity_reminders", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var startHour: Int
        get() = prefs.getInt(KEY_START_HOUR, 8)
        set(value) = prefs.edit { putInt(KEY_START_HOUR, value) }

    var endHour: Int
        get() = prefs.getInt(KEY_END_HOUR, 16)
        set(value) = prefs.edit { putInt(KEY_END_HOUR, value) }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_START_HOUR = "start_hour"
        private const val KEY_END_HOUR = "end_hour"
    }
}
