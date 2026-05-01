# Activity Reminders

Android app that vibrates at :50 past each hour as a reminder to get up and move.

## Building & Running (Linux)

### 1. Launch Android Studio

```bash
/usr/local/android-studio/bin/studio.sh &
```

### 2. Open the project

- **File → Open** → navigate to this directory (`/path/to/activity-reminders`) → **OK**
- Android Studio will detect the Gradle project and begin syncing automatically
- Wait for the sync to complete — progress shows in the bottom status bar
- If prompted to generate a Gradle wrapper, accept it

### 3. Connect your phone

On your Android phone:

1. **Settings → About Phone** → tap **Build Number** 7 times until "You are now a developer!" appears
2. **Settings → Developer Options** → enable **USB Debugging**
3. Plug phone into computer via USB
4. On the phone, accept the **"Allow USB debugging?"** prompt (check "Always allow from this computer")

In Android Studio, the device should appear in the device selector dropdown (top toolbar, left of the Run button).

### 4. Run the app

Click the green **Run ▶** button (or **Shift+F10**).

Android Studio will build the APK, install it on your phone, and launch it automatically.

### 5. Grant permissions (first launch)

The app needs two permissions that require manual steps:

- **Notifications** — a system prompt will appear; tap Allow
- **Exact Alarms** — a yellow banner appears at the bottom of the app; tap "Open Settings" and toggle on *Alarms & Reminders* for the app

Once both are granted, flip the toggle on and reminders will fire at :50 past each hour within your configured window.

---

## Project structure

```
app/src/main/java/com/example/activityreminders/
├── MainActivity.kt        # Compose UI + permission handling
├── MainViewModel.kt       # State + scheduler coordination
├── AlarmScheduler.kt      # Schedules exact alarms via AlarmManager
├── AlarmReceiver.kt       # Fires on alarm, triggers notification, reschedules
├── BootReceiver.kt        # Re-arms alarm after phone reboot
├── NotificationHelper.kt  # Notification channel + vibration
├── PreferencesManager.kt  # Persists settings to SharedPreferences
└── ui/theme/              # Tailwind-inspired dark palette (slate/emerald)
```

## Settings

| Setting | Default | Description |
|---|---|---|
| Enabled | Off | Master toggle |
| Start hour | 8:00 AM | First reminder window hour |
| End hour | 4:00 PM | No reminders at or after this hour |

Reminders always fire at **:50 past the hour** within the active window (e.g. 8:50, 9:50 … 15:50).
