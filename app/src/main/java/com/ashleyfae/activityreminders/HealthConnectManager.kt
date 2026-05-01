package com.ashleyfae.activityreminders

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        val granted = client.permissionController.getGrantedPermissions()
        return READ_STEPS in granted
    }

    suspend fun shouldAlert(threshold: Int): Boolean {
        if (!isAvailable() || !hasPermission()) return true
        return try {
            val now = Instant.now()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(now.minusSeconds(3600), now)
                )
            )
            (response[StepsRecord.COUNT_TOTAL] ?: 0L) < threshold
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
        val READ_STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        val PERMISSIONS = setOf(READ_STEPS)
    }
}
