package com.example.data.repository

import android.util.Patterns
import com.example.data.local.NotificationDao
import com.example.data.local.PreferencesManager
import com.example.data.local.ScanDao
import com.example.data.local.UserActivityDao
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.local.PasswordHasher
import com.example.data.model.AccountStatus
import com.example.data.model.ActivityType
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class AuthRepository(
    private val userDao: UserDao,
    private val scanDao: ScanDao,
    private val userActivityDao: UserActivityDao,
    private val notificationDao: NotificationDao,
    private val preferencesManager: PreferencesManager,
    private val activityTrackingRepository: ActivityTrackingRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserProfile?> = preferencesManager.activeUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(null)
        } else {
            userDao.observeById(uid).map { entity -> entity?.toProfile() }
        }
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Result<UserProfile> {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters long."))
        }
        if (password != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }
        if (userDao.getByEmail(cleanEmail) != null) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }

        val now = System.currentTimeMillis()
        val salt = PasswordHasher.generateSaltHex()
        val hash = PasswordHasher.hashPassword(password, salt)
        val userId = "usr_${UUID.randomUUID()}"

        // Standard consumer role is strictly enforced; users can never self-assign ADMIN.
        val entity = UserEntity(
            id = userId,
            fullName = cleanName,
            email = cleanEmail,
            passwordHash = hash,
            passwordSalt = salt,
            role = UserRole.CONSUMER.name,
            status = AccountStatus.ACTIVE.name,
            emailVerified = true,
            createdAtUtc = now,
            lastActiveAtUtc = now
        )
        userDao.insert(entity)
        preferencesManager.setActiveUserId(userId)
        activityTrackingRepository.recordQualifyingActivity(userId, ActivityType.SIGN_IN)
        return Result.success(entity.toProfile())
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your password."))
        }

        val user = userDao.getByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found for $cleanEmail."))

        if (!PasswordHasher.verifyPassword(password, user.passwordSalt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        if (user.status == AccountStatus.SUSPENDED.name) {
            return Result.failure(IllegalStateException("This account has been suspended by an administrator."))
        }

        preferencesManager.setActiveUserId(user.id)
        activityTrackingRepository.recordQualifyingActivity(user.id, ActivityType.SIGN_IN)
        return Result.success(user.toProfile())
    }

    suspend fun resetPassword(email: String, newPassword: String, confirmPassword: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("New password must be at least 8 characters."))
        }
        if (newPassword != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }

        val user = userDao.getByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No registered account matches $cleanEmail."))

        val newSalt = PasswordHasher.generateSaltHex()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        userDao.update(user.copy(passwordHash = newHash, passwordSalt = newSalt))
        return Result.success(Unit)
    }

    suspend fun updateProfile(userId: String, fullName: String): Result<Unit> {
        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Full name must be at least 2 characters."))
        }
        val existing = userDao.getById(userId)
            ?: return Result.failure(IllegalStateException("User not found."))
        userDao.update(existing.copy(fullName = cleanName))
        activityTrackingRepository.recordQualifyingActivity(userId, ActivityType.CORE_FEATURE)
        return Result.success(Unit)
    }

    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("New password must be at least 8 characters."))
        }
        val existing = userDao.getById(userId)
            ?: return Result.failure(IllegalStateException("User not found."))
        if (!PasswordHasher.verifyPassword(currentPassword, existing.passwordSalt, existing.passwordHash)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        val newSalt = PasswordHasher.generateSaltHex()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        userDao.update(existing.copy(passwordHash = newHash, passwordSalt = newSalt))
        return Result.success(Unit)
    }

    suspend fun deleteAccount(userId: String): Result<Unit> {
        val user = userDao.getById(userId)
            ?: return Result.failure(IllegalStateException("Account not found."))
        if (user.role == UserRole.ADMIN.name) {
            return Result.failure(IllegalStateException("Root administrator account cannot be deleted from consumer settings."))
        }
        scanDao.deleteAllForUser(userId)
        userActivityDao.deleteForUser(userId)
        notificationDao.deleteForUser(userId)
        userDao.deleteById(userId)
        preferencesManager.setActiveUserId(null)
        return Result.success(Unit)
    }

    suspend fun logout() {
        preferencesManager.setActiveUserId(null)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun UserEntity.toProfile(): UserProfile {
        return UserProfile(
            id = id,
            fullName = fullName,
            email = email,
            role = UserRole.entries.firstOrNull { it.name == role } ?: UserRole.CONSUMER,
            status = AccountStatus.entries.firstOrNull { it.name == status } ?: AccountStatus.ACTIVE,
            emailVerified = emailVerified,
            createdAtUtc = createdAtUtc,
            lastActiveAtUtc = lastActiveAtUtc
        )
    }
}
