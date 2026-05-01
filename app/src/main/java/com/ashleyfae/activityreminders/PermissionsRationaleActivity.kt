package com.ashleyfae.activityreminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashleyfae.activityreminders.ui.theme.*

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActivityRemindersTheme {
                RationaleScreen()
            }
        }
    }
}

@Composable
fun RationaleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            "Health Data Usage",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RationaleItem(
                    title = "What we read",
                    body = "Step count from the past hour, via Health Connect."
                )
                RationaleItem(
                    title = "Why we read it",
                    body = "To decide whether to send a movement reminder. If you've taken 250 or more steps in the past hour, the reminder is skipped."
                )
                RationaleItem(
                    title = "What we don't do",
                    body = "Your step data is never stored, shared, or transmitted anywhere. It is read at reminder time and immediately discarded."
                )
            }
        }
    }
}

@Composable
fun RationaleItem(title: String, body: String) {
    Column {
        Text(title, fontSize = 14.sp, color = Emerald500)
        Spacer(Modifier.height(2.dp))
        Text(body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
    }
}
