package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        SecurityCodeEntity::class,
        ScanEntity::class,
        UserActivityEntity::class,
        NotificationEntity::class,
        OfflineScanQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productCatalogDao(): ProductCatalogDao
    abstract fun securityCodeDao(): SecurityCodeDao
    abstract fun scanDao(): ScanDao
    abstract fun userActivityDao(): UserActivityDao
    abstract fun notificationDao(): NotificationDao
    abstract fun offlineScanQueueDao(): OfflineScanQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "truecheck_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
