package com.ashleyfae.activityreminders

import android.Manifest
import java.util.Calendar
import kotlinx.coroutines.delay
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ashleyfae.activityreminders.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* recomposition handles state refresh via onResume check */ }

    private val requestHcPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { /* recomposition handles state refresh via onResume check */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActivityRemindersTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestNotifPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestExactAlarmPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    },
                    onRequestHcPermission = {
                        requestHcPermissions.launch(HealthConnectManager.PERMISSIONS)
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestNotifPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestHcPermission: () -> Unit,
) {
    val context = LocalContext.current
    val isEnabled by viewModel.isEnabled.collectAsState()
    val startHour by viewModel.startHour.collectAsState()
    val endHour by viewModel.endHour.collectAsState()
    val stepThreshold by viewModel.stepThreshold.collectAsState()
    val stepsLastHour by viewModel.stepsLastHour.collectAsState()
    val stepsLastUpdated by viewModel.stepsLastUpdated.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val hc = remember { HealthConnectManager(context) }

    var hasNotifPermission by remember { mutableStateOf(checkNotifPermission(context)) }
    var hasExactAlarmPermission by remember { mutableStateOf(checkExactAlarmPermission(context)) }
    var isHcAvailable by remember { mutableStateOf(hc.isAvailable()) }
    var hasHcPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasHcPermission = hc.hasPermission()
    }

    // Recompute next reminder text and step count at each minute boundary
    var currentMinute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    LaunchedEffect(Unit) {
        viewModel.refreshSteps()
        while (true) {
            val now = Calendar.getInstance()
            val msUntilNextMinute = (60 - now.get(Calendar.SECOND)) * 1000L - now.get(Calendar.MILLISECOND)
            delay(msUntilNextMinute.coerceAtLeast(100L))
            currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
            viewModel.refreshSteps()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = checkNotifPermission(context)
                hasExactAlarmPermission = checkExactAlarmPermission(context)
                isHcAvailable = hc.isAvailable()
                scope.launch { hasHcPermission = hc.hasPermission() }
                viewModel.refreshSteps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        // Header
        Text(
            text = "Activity Reminders",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Emerald500 else Slate600)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = if (isEnabled) "Active" else "Paused",
                fontSize = 13.sp,
                color = if (isEnabled) Emerald500 else Slate600
            )
        }

        Spacer(Modifier.height(32.dp))

        // Main toggle card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Movement Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Vibrates at :50 past each hour",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Emerald500,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate700,
                        uncheckedBorderColor = Slate700
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Time window card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    "ACTIVE HOURS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeButton(
                        label = "Start",
                        hour = startHour,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartPicker = true }
                    )
                    TimeButton(
                        label = "End",
                        hour = endHour,
                        modifier = Modifier.weight(1f),
                        onClick = { showEndPicker = true }
                    )
                }
            }
        }

        // Step threshold card — only shown when Health Connect is available
        if (isHcAvailable) {
            Spacer(Modifier.height(12.dp))
            StepThresholdCard(
                threshold = stepThreshold,
                currentSteps = stepsLastHour,
                stepsLastUpdated = stepsLastUpdated,
                onThresholdChange = { viewModel.setStepThreshold(it) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // Next reminder hint — keyed on currentMinute so it recomputes at :00 of each minute
        if (isEnabled) {
            val nextReminderText = remember(startHour, endHour, currentMinute) {
                viewModel.nextReminderText(startHour, endHour)
            }
            Text(text = nextReminderText, fontSize = 13.sp, color = Slate400)
        }

        Spacer(Modifier.weight(1f))

        // Permission warnings (float to bottom)
        if (!hasExactAlarmPermission) {
            PermissionBanner(
                message = "Exact alarm permission needed for on-time reminders",
                actionLabel = "Open Settings",
                onClick = onRequestExactAlarmPermission
            )
            Spacer(Modifier.height(8.dp))
        }
        if (!hasNotifPermission) {
            PermissionBanner(
                message = "Notification permission needed to show reminders",
                actionLabel = "Allow",
                onClick = onRequestNotifPermission
            )
            Spacer(Modifier.height(8.dp))
        }
        if (isHcAvailable && !hasHcPermission) {
            PermissionBanner(
                message = "Grant Health Connect access to enable step-based filtering",
                actionLabel = "Grant",
                onClick = onRequestHcPermission
            )
        }
    }

    if (showStartPicker) {
        HourPickerDialog(
            initialHour = startHour,
            onDismiss = { showStartPicker = false },
            onConfirm = { hour ->
                viewModel.setStartHour(hour)
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        HourPickerDialog(
            initialHour = endHour,
            onDismiss = { showEndPicker = false },
            onConfirm = { hour ->
                viewModel.setEndHour(hour)
                showEndPicker = false
            }
        )
    }
}

@Composable
fun StepThresholdCard(threshold: Int, currentSteps: Long?, stepsLastUpdated: String?, onThresholdChange: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                "STEP THRESHOLD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepperButton(label = "−") {
                    onThresholdChange((threshold - 50).coerceAtLeast(50))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$threshold",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "steps",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StepperButton(label = "+") {
                    onThresholdChange((threshold + 50).coerceAtMost(2000))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Alert only if steps in the past hour are below this",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentSteps != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("This hour  ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currentSteps steps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentSteps < threshold) Slate400 else Emerald500
                            )
                        }
                        if (stepsLastUpdated != null) {
                            Text(
                                "Updated $stepsLastUpdated",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (currentSteps >= threshold) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Emerald500.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Goal Met",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald500
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        Text(label, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TimeButton(label: String, hour: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = formatHour(hour),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun PermissionBanner(message: String, actionLabel: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate800,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                fontSize = 13.sp,
                color = Amber400,
                modifier = Modifier.weight(1f),
                lineHeight = 18.sp
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, color = Emerald500, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun HourPickerDialog(
    initialHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val dialog = android.app.TimePickerDialog(
            context,
            { _, hour, _ -> onConfirm(hour) },
            initialHour, 0, false
        )
        dialog.setOnCancelListener { onDismiss() }
        dialog.show()
        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}

private fun checkNotifPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun checkExactAlarmPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    } else true
}
