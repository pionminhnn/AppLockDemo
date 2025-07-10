package com.nguyennhatminh614.applockdemo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database cho ứng dụng
 */
@Database(
    entities = [IntruderRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun intruderRecordDao(): IntruderRecordDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Lấy instance của database (Singleton pattern)
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_lock_database"
                )
                .fallbackToDestructiveMigration() // Trong production, nên implement migration properly
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}