package com.example.data.repository

import com.example.data.local.ScanDao
import com.example.data.local.UserActivityDao
import com.example.data.local.UserDao
import com.example.data.model.AccountStatus
import com.example.data.model.AdminDashboardSnapshot
import com.example.data.model.DailyMetricPoint
import com.example.data.model.ProductCategory
import com.example.data.model.ScanMethod
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus
import java.util.Calendar
import java.util.TimeZone

class AdminAnalyticsRepository(
    private val userDao: UserDao,
    private val scanDao: ScanDao,
    private val userActivityDao: UserActivityDao
) {
    suspend fun loadSnapshot(actingUserId: String, daysWindow: Int = 7): Result<AdminDashboardSnapshot> {
        val actingUser = userDao.getById(actingUserId)
            ?: return Result.failure(SecurityException("Unauthenticated administrator access denied."))
        if (actingUser.role != UserRole.ADMIN.name) {
            return Result.failure(SecurityException("Access denied: Administrator privileges required."))
        }

        val now = System.currentTimeMillis()
        val todayUtc = ActivityTrackingRepository.utcDateString(now)
        val dayMs = 86_400_000L
        val startTodayUtc = utcStartOfDayMillis(now)
        val start7Days = startTodayUtc - 6 * dayMs
        val start30Days = startTodayUtc - 29 * dayMs

        val allUsers = userDao.getAllUsers()
        val allScans = scanDao.getAllScansForAdmin()
        val allActivity = userActivityDao.getAllActivity()

        val scanCountByUser = allScans.groupingBy { it.userId }.eachCount()

        val dauToday = allActivity
            .filter { it.activityDateUtc == todayUtc }
            .map { it.userId }
            .distinct()
            .size

        val recent30DateSet = (0 until 30).map { offset ->
            ActivityTrackingRepository.utcDateString(startTodayUtc - offset * dayMs)
        }.toSet()

        val mauCount = allActivity
            .filter { it.activityDateUtc in recent30DateSet }
            .map { it.userId }
            .distinct()
            .size

        val newUsersToday = allUsers.count { it.createdAtUtc >= startTodayUtc }
        val newUsers7Days = allUsers.count { it.createdAtUtc >= start7Days }
        val newUsers30Days = allUsers.count { it.createdAtUtc >= start30Days }

        val scansToday = allScans.count { it.timestampUtc >= startTodayUtc }
        val scans7Days = allScans.count { it.timestampUtc >= start7Days }
        val scans30Days = allScans.count { it.timestampUtc >= start30Days }

        val realCount = allScans.count { it.status == VerificationStatus.REAL.name }
        val suspiciousCount = allScans.count { it.status == VerificationStatus.SUSPICIOUS.name }
        val fakeCount = allScans.count { it.status == VerificationStatus.FAKE.name }
        val unverifiedCount = allScans.count { it.status == VerificationStatus.UNVERIFIED.name }

        val topMethod = allScans
            .groupingBy { it.method }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.let { raw -> ScanMethod.entries.firstOrNull { it.name == raw }?.label ?: raw }
            ?: "No scans yet"

        val topCategory = allScans
            .groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.let { raw -> ProductCategory.entries.firstOrNull { it.name == raw }?.displayName ?: raw }
            ?: "No scans yet"

        val windowDays = (daysWindow.coerceIn(7, 30) - 1 downTo 0).map { offset ->
            val dayStart = startTodayUtc - offset * dayMs
            val dayEnd = dayStart + dayMs
            val label = ActivityTrackingRepository.utcDateString(dayStart)
            Triple(label, dayStart, dayEnd)
        }

        val dailyRegistrationsSeries = windowDays.map { (label, s, e) ->
            DailyMetricPoint(label.substring(5), allUsers.count { it.createdAtUtc in s until e })
        }

        val dailyActiveSeries = windowDays.map { (label, _, _) ->
            DailyMetricPoint(
                label.substring(5),
                allActivity.filter { it.activityDateUtc == label }.map { it.userId }.distinct().size
            )
        }

        val dailyScansSeries = windowDays.map { (label, s, e) ->
            DailyMetricPoint(label.substring(5), allScans.count { it.timestampUtc in s until e })
        }

        val weeklyGrowthSeries = (3 downTo 0).map { weekOffset ->
            val wEnd = startTodayUtc + dayMs - weekOffset * 7 * dayMs
            val count = allUsers.count { it.createdAtUtc < wEnd }
            DailyMetricPoint("W-${weekOffset}", count)
        }

        val monthlyGrowthSeries = (2 downTo 0).map { monthOffset ->
            val mEnd = startTodayUtc + dayMs - monthOffset * 30 * dayMs
            val count = allUsers.count { it.createdAtUtc < mEnd }
            DailyMetricPoint("M-${monthOffset}", count)
        }

        val userProfiles = allUsers.map { entity ->
            // Explicitly strip passwordHash and passwordSalt — never expose credentials.
            UserProfile(
                id = entity.id,
                fullName = entity.fullName,
                email = entity.email,
                role = UserRole.entries.firstOrNull { it.name == entity.role } ?: UserRole.CONSUMER,
                status = AccountStatus.entries.firstOrNull { it.name == entity.status } ?: AccountStatus.ACTIVE,
                emailVerified = entity.emailVerified,
                createdAtUtc = entity.createdAtUtc,
                lastActiveAtUtc = entity.lastActiveAtUtc,
                scanCount = scanCountByUser[entity.id] ?: 0
            )
        }

        return Result.success(
            AdminDashboardSnapshot(
                totalUsers = allUsers.size,
                dailyActiveUsers = dauToday,
                monthlyActiveUsers = mauCount,
                newUsersToday = newUsersToday,
                newUsers7Days = newUsers7Days,
                newUsers30Days = newUsers30Days,
                totalScans = allScans.size,
                scansToday = scansToday,
                scans7Days = scans7Days,
                scans30Days = scans30Days,
                realCount = realCount,
                suspiciousCount = suspiciousCount,
                fakeCount = fakeCount,
                unverifiedCount = unverifiedCount,
                topScanMethod = topMethod,
                topCategory = topCategory,
                dailyRegistrationsSeries = dailyRegistrationsSeries,
                dailyActiveUsersSeries = dailyActiveSeries,
                dailyScansSeries = dailyScansSeries,
                weeklyGrowthSeries = weeklyGrowthSeries,
                monthlyGrowthSeries = monthlyGrowthSeries,
                users = userProfiles
            )
        )
    }

    suspend fun setUserStatus(
        actingUserId: String,
        targetUserId: String,
        newStatus: AccountStatus
    ): Result<Unit> {
        val actingUser = userDao.getById(actingUserId)
            ?: return Result.failure(SecurityException("Unauthenticated administrator access denied."))
        if (actingUser.role != UserRole.ADMIN.name) {
            return Result.failure(SecurityException("Access denied: Administrator privileges required."))
        }
        if (actingUserId == targetUserId) {
            return Result.failure(IllegalArgumentException("Administrators cannot suspend their own active account."))
        }
        userDao.updateStatus(targetUserId, newStatus.name)
        return Result.success(Unit)
    }

    private fun utcStartOfDayMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
