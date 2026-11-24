package com.example.data.repository

import com.example.data.local.UserActivityDao
import com.example.data.local.UserActivityEntity
import com.example.data.local.UserDao
import com.example.data.model.ActivityType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ActivityTrackingRepository(
    private val userActivityDao: UserActivityDao,
    private val userDao: UserDao
) {
    suspend fun recordQualifyingActivity(userId: String, type: ActivityType) {
        if (userId.isBlank()) return
        val nowUtc = System.currentTimeMillis()
        val dateKeyUtc = utcDateString(nowUtc)
        val existing = userActivityDao.getForDay(userId, dateKeyUtc)
        val updated = UserActivityEntity(
            userId = userId,
            activityDateUtc = dateKeyUtc,
            activityType = type.name,
            lastActiveTimestampUtc = nowUtc,
            eventCount = (existing?.eventCount ?: 0) + 1
        )
        userActivityDao.upsert(updated)
        userDao.updateLastActive(userId, nowUtc)
    }

    companion object {
        fun utcDateString(timestampMillis: Long): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            return fmt.format(Date(timestampMillis))
        }
    }
}
