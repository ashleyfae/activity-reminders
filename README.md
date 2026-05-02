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

The app needs several permissions that require manual steps:

- **Notifications** — a system prompt will appear; tap Allow
- **Exact Alarms** — a yellow banner appears at the bottom of the app; tap "Open Settings" and toggle on *Alarms & Reminders* for the app
- **Health Connect** (optional) — if Health Connect is installed, a banner will offer to grant step-count access; tap "Grant" and allow *Steps* read access. See [Health Connect](#health-connect) below.

Once the first two are granted, flip the toggle on and reminders will fire at :50 past each hour within your configured window.

---

## Project structure

```
app/src/main/java/com/ashleyfae/activityreminders/
├── MainActivity.kt              # Compose UI + permission handling
├── MainViewModel.kt             # State + scheduler coordination
├── AlarmScheduler.kt            # Schedules exact alarms via AlarmManager
├── AlarmReceiver.kt             # Fires on alarm, checks steps, reschedules
├── BootReceiver.kt              # Re-arms alarm after phone reboot
├── NotificationHelper.kt        # Notification channel + vibration
├── PreferencesManager.kt        # Persists settings to SharedPreferences
├── HealthConnectManager.kt      # Health Connect step queries
├── PermissionsRationaleActivity.kt  # Required privacy screen for Health Connect
└── ui/theme/                    # Tailwind-inspired dark palette (slate/emerald)
```

## Settings

| Setting | Default | Description |
|---|---|---|
| Enabled | Off | Master toggle |
| Start hour | 8:00 AM | First reminder window hour |
| End hour | 4:00 PM | No reminders at or after this hour |
| Step threshold | 250 | Skip reminder if steps this hour are at or above this value |

Reminders always fire at **:50 past the hour** within the active window (e.g. 8:50, 9:50 … 15:50).

---

## Health Connect

If [Health Connect](https://health.google/health-connect-android/) is installed on the device, the app can read your step count and skip reminders when you've already been active enough.

**How it works**

At each :50 alarm, the app reads the step count since the top of the current hour (e.g. at 9:50 it reads steps from 9:00–9:50). If the count is at or above the configured threshold, the reminder is silently skipped. If Health Connect is unavailable, not granted, or the query fails, the reminder fires as normal.

**Privacy**

Step data is read at reminder time and while the app is open (to display the current hour's count in the UI). It is never stored, logged, or transmitted anywhere. This is shown to the user in the in-app privacy screen (`PermissionsRationaleActivity`), which Health Connect requires every app to provide.

**Permissions required**

- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` (declared in manifest; not requested at runtime)
