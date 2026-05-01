package com.ashleyfae.activityreminders

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        val granted = client.permissionController.getGrantedPermissions()
        return READ_STEPS in granted
    }

    suspend fun getStepsLastHour(): Long {
        val now = Instant.now()
        val topOfHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).toInstant()
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(topOfHour, now)
            )
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun shouldAlert(threshold: Int): Boolean {
        if (!isAvailable() || !hasPermission()) return true
        return try {
            getStepsLastHour() < threshold
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
