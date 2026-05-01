package com.ashleyfae.activityreminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val scheduler = AlarmScheduler(application)

    private val _isEnabled = MutableStateFlow(prefs.isEnabled)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _startHour = MutableStateFlow(prefs.startHour)
    val startHour: StateFlow<Int> = _startHour

    private val _endHour = MutableStateFlow(prefs.endHour)
    val endHour: StateFlow<Int> = _endHour

    fun setEnabled(enabled: Boolean) {
        prefs.isEnabled = enabled
        _isEnabled.value = enabled
        if (enabled) scheduler.schedule() else scheduler.cancel()
    }

    fun setStartHour(hour: Int) {
        prefs.startHour = hour
        _startHour.value = hour
        if (prefs.isEnabled) scheduler.schedule()
    }

    fun setEndHour(hour: Int) {
        prefs.endHour = hour
        _endHour.value = hour
        if (prefs.isEnabled) scheduler.schedule()
    }

    fun nextReminderText(startHour: Int, endHour: Int): String {
        val now = Calendar.getInstance()
        val candidate = (now.clone() as Calendar).apply {
            set(Calendar.MINUTE, 50)
            set(Calendar.SECOND, 0)
            if (now.get(Calendar.MINUTE) >= 50) add(Calendar.HOUR_OF_DAY, 1)
        }
        repeat(25) {
            val h = candidate.get(Calendar.HOUR_OF_DAY)
            if (h in startHour until endHour) return "Next reminder at ${formatHour(h, minute = 50)}"
            candidate.add(Calendar.HOUR_OF_DAY, 1)
        }
        return "No reminders scheduled today"
    }
}

fun formatHour(hour: Int, minute: Int = 0): String {
    val m = minute.toString().padStart(2, '0')
    return when {
        hour == 0 -> "12:${m} AM"
        hour < 12 -> "${hour}:${m} AM"
        hour == 12 -> "12:${m} PM"
        else -> "${hour - 12}:${m} PM"
    }
}
