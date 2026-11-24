package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAtUtc DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAtUtc DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateStatus(userId: String, status: String)

    @Query("UPDATE users SET lastActiveAtUtc = :timestamp WHERE id = :userId")
    suspend fun updateLastActive(userId: String, timestamp: Long)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)
}

@Dao
interface ProductCatalogDao {
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE qrIdentifier = :qr LIMIT 1")
    suspend fun findByQr(qr: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)
}

@Dao
interface SecurityCodeDao {
    @Query("SELECT * FROM security_codes WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun findByCode(code: String): SecurityCodeEntity?

    @Query("SELECT COUNT(*) FROM security_codes")
    suspend fun countCodes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<SecurityCodeEntity>)

    @Query(
        "UPDATE security_codes SET verificationCount = verificationCount + 1, " +
            "firstVerifiedAtUtc = COALESCE(firstVerifiedAtUtc, :timestamp), " +
            "firstVerifiedByUserId = COALESCE(firstVerifiedByUserId, :userId) " +
            "WHERE UPPER(code) = UPPER(:code)"
    )
    suspend fun markVerified(code: String, timestamp: Long, userId: String)
}

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY timestampUtc DESC")
    fun observeScansForUser(userId: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY timestampUtc DESC LIMIT :limit")
    fun observeRecentScansForUser(userId: String, limit: Int): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :scanId AND userId = :userId LIMIT 1")
    suspend fun getScanById(scanId: String, userId: String): ScanEntity?

    @Query("SELECT * FROM scans ORDER BY timestampUtc DESC")
    suspend fun getAllScansForAdmin(): List<ScanEntity>

    @Query("SELECT * FROM scans ORDER BY timestampUtc DESC")
    fun observeAllScansForAdmin(): Flow<List<ScanEntity>>

    @Query("SELECT COUNT(*) FROM scans WHERE userId = :userId")
    suspend fun countScansForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :scanId AND userId = :userId")
    suspend fun deleteScan(scanId: String, userId: String)

    @Query("DELETE FROM scans WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface UserActivityDao {
    @Query("SELECT * FROM user_activity WHERE userId = :userId AND activityDateUtc = :dateUtc LIMIT 1")
    suspend fun getForDay(userId: String, dateUtc: String): UserActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: UserActivityEntity)

    @Query("SELECT * FROM user_activity")
    suspend fun getAllActivity(): List<UserActivityEntity>

    @Query("SELECT * FROM user_activity")
    fun observeAllActivity(): Flow<List<UserActivityEntity>>

    @Query("DELETE FROM user_activity WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestampUtc DESC")
    fun observeForUser(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface OfflineScanQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: OfflineScanQueueEntity)

    @Query("SELECT * FROM offline_scan_queue WHERE userId = :userId ORDER BY createdAtUtc ASC")
    suspend fun getPendingForUser(userId: String): List<OfflineScanQueueEntity>

    @Query("DELETE FROM offline_scan_queue WHERE id = :id")
    suspend fun deleteById(id: String)
}
