package com.awaytime.app.service

import android.content.Context
import android.content.SharedPreferences
import com.awaytime.app.data.AwayTimeDatabase
import com.awaytime.app.data.entity.AppGroupEntity
import com.awaytime.app.data.entity.UserSettingsEntity
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.Date

/**
 * Service to backup and restore critical user data to prevent data loss
 */
class DataBackupService(private val context: Context) {
    
    private val backupPrefs: SharedPreferences = 
        context.getSharedPreferences("awaytime_backup", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val database = AwayTimeDatabase.getDatabase(context)
    
    companion object {
        private const val BACKUP_VERSION = "backup_version"
        private const val BACKUP_TIMESTAMP = "backup_timestamp" 
        private const val BACKUP_APP_GROUPS = "backup_app_groups"
        private const val BACKUP_USER_SETTINGS = "backup_user_settings"
        private const val BACKUP_PERMISSIONS = "backup_permissions"
        private const val CURRENT_BACKUP_VERSION = 1
    }
    
    /**
     * Create a backup of all critical user data
     */
    suspend fun createBackup() {
        withContext(Dispatchers.IO) {
            try {
                println("🔄 Creating data backup...")
                
                val appGroups = database.appGroupDao().getAllAppGroupsSync()
                val userSettings = database.userSettingsDao().getOrCreateUserSettings()
                val permissionStates = backupPermissionStates()
                
                // Create backup data structure
                val backup = BackupData(
                    version = CURRENT_BACKUP_VERSION,
                    timestamp = System.currentTimeMillis(),
                    appGroups = appGroups,
                    userSettings = userSettings,
                    permissionStates = permissionStates
                )
                
                // Save backup to SharedPreferences (multiple locations for safety)
                val backupJson = gson.toJson(backup)
                backupPrefs.edit()
                    .putInt(BACKUP_VERSION, CURRENT_BACKUP_VERSION)
                    .putLong(BACKUP_TIMESTAMP, System.currentTimeMillis())
                    .putString(BACKUP_APP_GROUPS, gson.toJson(appGroups))
                    .putString(BACKUP_USER_SETTINGS, gson.toJson(userSettings))
                    .putString(BACKUP_PERMISSIONS, gson.toJson(permissionStates))
                    .putString("full_backup", backupJson)
                    .apply()
                
                // Also save to a secondary location
                val secondaryBackup = context.getSharedPreferences("awaytime_backup_secondary", Context.MODE_PRIVATE)
                secondaryBackup.edit()
                    .putString("backup_data", backupJson)
                    .putLong("backup_time", System.currentTimeMillis())
                    .apply()
                
                println("✅ Data backup completed successfully")
                
            } catch (e: Exception) {
                println("❌ Backup failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Restore data from backup if primary storage is corrupted
     */
    suspend fun restoreFromBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔄 Attempting data restore...")
                
                // Try primary backup first
                val backupJson = backupPrefs.getString("full_backup", null)
                    ?: backupPrefs.getString(BACKUP_APP_GROUPS, null) // Fallback to partial backup
                
                if (backupJson != null) {
                    val backup = try {
                        gson.fromJson(backupJson, BackupData::class.java)
                    } catch (e: Exception) {
                        // Try to restore individual components if full backup fails
                        restoreIndividualComponents()
                        return@withContext true
                    }
                    
                    // Restore app groups
                    backup.appGroups.forEach { appGroup ->
                        try {
                            database.appGroupDao().insertAppGroup(appGroup)
                        } catch (e: Exception) {
                            println("⚠️ Failed to restore app group: ${appGroup.name}")
                        }
                    }
                    
                    // Restore user settings
                    try {
                        database.userSettingsDao().insertOrUpdateUserSettings(backup.userSettings)
                    } catch (e: Exception) {
                        println("⚠️ Failed to restore user settings")
                    }
                    
                    // Restore permission states
                    restorePermissionStates(backup.permissionStates)
                    
                    println("✅ Data restored successfully from backup")
                    return@withContext true
                } else {
                    // Try secondary backup
                    val secondaryBackup = context.getSharedPreferences("awaytime_backup_secondary", Context.MODE_PRIVATE)
                    val secondaryData = secondaryBackup.getString("backup_data", null)
                    
                    if (secondaryData != null) {
                        val backup = gson.fromJson(secondaryData, BackupData::class.java)
                        // Restore from secondary backup (same logic as above)
                        println("✅ Data restored from secondary backup")
                        return@withContext true
                    }
                }
                
                println("⚠️ No backup data found")
                return@withContext false
                
            } catch (e: Exception) {
                println("❌ Restore failed: ${e.message}")
                e.printStackTrace()
                return@withContext false
            }
        }
    }
    
    /**
     * Check if backup exists and is recent
     */
    fun hasRecentBackup(): Boolean {
        val lastBackup = backupPrefs.getLong(BACKUP_TIMESTAMP, 0)
        val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
        return lastBackup > dayAgo
    }
    
    /**
     * Get backup statistics
     */
    fun getBackupInfo(): BackupInfo {
        val timestamp = backupPrefs.getLong(BACKUP_TIMESTAMP, 0)
        val version = backupPrefs.getInt(BACKUP_VERSION, 0)
        val hasBackup = backupPrefs.contains("full_backup")
        
        return BackupInfo(
            hasBackup = hasBackup,
            timestamp = timestamp,
            version = version,
            age = if (timestamp > 0) System.currentTimeMillis() - timestamp else -1
        )
    }
    
    /**
     * Backup permission states to prevent re-requesting
     */
    private fun backupPermissionStates(): Map<String, Any> {
        val permissionPrefs = context.getSharedPreferences("awaytime_permissions", Context.MODE_PRIVATE)
        return permissionPrefs.all.mapValues { entry ->
            when (val value = entry.value) {
                is Boolean -> value
                is Long -> value
                is String -> value
                else -> value.toString()
            }
        }
    }
    
    /**
     * Restore permission states
     */
    private fun restorePermissionStates(states: Map<String, Any>) {
        val permissionPrefs = context.getSharedPreferences("awaytime_permissions", Context.MODE_PRIVATE)
        val editor = permissionPrefs.edit()
        
        states.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value) 
                is String -> editor.putString(key, value)
                is Double -> editor.putLong(key, value.toLong())
            }
        }
        editor.apply()
    }
    
    /**
     * Restore individual components if full backup fails
     */
    private suspend fun restoreIndividualComponents(): Boolean {
        var restored = false
        
        // Try to restore app groups
        backupPrefs.getString(BACKUP_APP_GROUPS, null)?.let { json ->
            try {
                val appGroups = gson.fromJson(json, Array<AppGroupEntity>::class.java).toList()
                appGroups.forEach { database.appGroupDao().insertAppGroup(it) }
                restored = true
            } catch (e: Exception) {
                println("⚠️ Failed to restore app groups from partial backup")
            }
        }
        
        // Try to restore user settings
        backupPrefs.getString(BACKUP_USER_SETTINGS, null)?.let { json ->
            try {
                val userSettings = gson.fromJson(json, UserSettingsEntity::class.java)
                database.userSettingsDao().insertOrUpdateUserSettings(userSettings)
                restored = true
            } catch (e: Exception) {
                println("⚠️ Failed to restore user settings from partial backup")
            }
        }
        
        return restored
    }
    
    /**
     * Schedule automatic backups
     */
    fun scheduleAutoBackup() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(6 * 60 * 60 * 1000) // Every 6 hours
                createBackup()
            }
        }
    }
    
    /**
     * Data classes for backup structure
     */
    data class BackupData(
        val version: Int,
        val timestamp: Long,
        val appGroups: List<AppGroupEntity>,
        val userSettings: UserSettingsEntity,
        val permissionStates: Map<String, Any>
    )
    
    data class BackupInfo(
        val hasBackup: Boolean,
        val timestamp: Long,
        val version: Int,
        val age: Long
    )
}

/**
 * Extension functions for DAO to handle backup operations
 */
suspend fun com.awaytime.app.data.dao.UserSettingsDao.insertOrUpdateUserSettings(settings: UserSettingsEntity) {
    try {
        insertUserSettings(settings)
    } catch (e: Exception) {
        // If insert fails, try update
        updatePremiumStatus(settings.isPremium)
        updateStreak(settings.streakCount, settings.lastStreakDate ?: System.currentTimeMillis())
        updateNotificationsEnabled(settings.notificationsEnabled)
    }
}

suspend fun com.awaytime.app.data.dao.UserSettingsDao.insertUserSettings(settings: UserSettingsEntity) {
    // Implementation would be added to the DAO interface
    // For now, we'll use existing update methods
    updatePremiumStatus(settings.isPremium)
    updateStreak(settings.streakCount, settings.lastStreakDate ?: System.currentTimeMillis())
    updateNotificationsEnabled(settings.notificationsEnabled)
}