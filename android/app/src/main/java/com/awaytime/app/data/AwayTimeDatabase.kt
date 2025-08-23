package com.awaytime.app.data

import android.content.Context
import androidx.room.*
import com.awaytime.app.data.dao.AppGroupDao
import com.awaytime.app.data.dao.UsageRecordDao
import com.awaytime.app.data.dao.UserSettingsDao
import com.awaytime.app.data.entity.AppGroupEntity
import com.awaytime.app.data.entity.UsageRecordEntity
import com.awaytime.app.data.entity.UserSettingsEntity
import java.util.Date
import java.util.concurrent.Executors

@Database(
    entities = [
        UsageRecordEntity::class,
        AppGroupEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AwayTimeDatabase : RoomDatabase() {

    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun appGroupDao(): AppGroupDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AwayTimeDatabase? = null

        fun getDatabase(context: Context): AwayTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AwayTimeDatabase::class.java,
                    "awaytime_database"
                )
                    .addMigrations() // Add proper migrations instead of destructive migration
                    .addCallback(DatabaseCallback(context))
                    .setQueryCallback(QueryCallback { sqlQuery, bindArgs ->
                        // Log slow queries for debugging on background thread
                        if (sqlQuery.length > 50) { // Only log complex queries
                            println("DB Query: $sqlQuery")
                        }
                    }, Executors.newSingleThreadExecutor())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                println("✅ Database created successfully")
            }
            
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                println("✅ Database opened successfully")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}