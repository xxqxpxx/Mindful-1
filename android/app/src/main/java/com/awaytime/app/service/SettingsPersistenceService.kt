package com.awaytime.app.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import com.google.gson.Gson

/**
 * Service to ensure app settings are properly persisted and recovered
 */
class SettingsPersistenceService(private val context: Context) {
    
    private val mainPrefs: SharedPreferences = 
        context.getSharedPreferences("awaytime_settings", Context.MODE_PRIVATE)
    private val backupPrefs: SharedPreferences = 
        context.getSharedPreferences("awaytime_settings_backup", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val SETTINGS_VERSION = "settings_version"
        private const val LAST_BACKUP_TIME = "last_backup_time"
        private const val DAILY_LIMIT_MINUTES = "daily_limit_minutes"
        private const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val PREMIUM_STATUS = "premium_status"
        private const val STREAK_COUNT = "streak_count"
        private const val LAST_STREAK_DATE = "last_streak_date"
        private const val SELECTED_APPS_JSON = "selected_apps_json"
        private const val APP_GROUP_SETTINGS = "app_group_settings"
        private const val USER_PREFERENCES = "user_preferences"
        private const val CURRENT_VERSION = 2
    }
    
    /**
     * Save settings with automatic backup
     */
    fun saveSettings(settings: AppSettings) {
        try {
            val settingsJson = gson.toJson(settings)
            val timestamp = System.currentTimeMillis()
            
            // Save to main preferences
            mainPrefs.edit()
                .putString(USER_PREFERENCES, settingsJson)
                .putInt(SETTINGS_VERSION, CURRENT_VERSION)
                .putLong(LAST_BACKUP_TIME, timestamp)
                .apply()
            
            // Save to backup preferences
            backupPrefs.edit()
                .putString(USER_PREFERENCES, settingsJson)
                .putInt(SETTINGS_VERSION, CURRENT_VERSION)
                .putLong(LAST_BACKUP_TIME, timestamp)
                .apply()
            
            println("✅ Settings saved with backup protection")
            
        } catch (e: Exception) {
            println("❌ Failed to save settings: ${e.message}")
        }
    }
    
    /**
     * Load settings with recovery fallback
     */
    fun loadSettings(): AppSettings {
        return try {
            // Try to load from main preferences first
            val mainSettingsJson = mainPrefs.getString(USER_PREFERENCES, null)
            
            if (mainSettingsJson != null) {
                val settings = gson.fromJson(mainSettingsJson, AppSettings::class.java)
                validateAndReturnSettings(settings, "main")
            } else {
                // Fallback to backup preferences
                val backupSettingsJson = backupPrefs.getString(USER_PREFERENCES, null)
                
                if (backupSettingsJson != null) {
                    val settings = gson.fromJson(backupSettingsJson, AppSettings::class.java)
                    println("⚠️ Main settings not found, loaded from backup")
                    validateAndReturnSettings(settings, "backup")
                } else {
                    // Return default settings if nothing found
                    println("⚠️ No settings found, using defaults")
                    getDefaultSettings()
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to load settings: ${e.message}")
            getDefaultSettings()
        }
    }
    
    /**
     * Validate settings and fix any corruption
     */
    private fun validateAndReturnSettings(settings: AppSettings, source: String): AppSettings {
        var validatedSettings = settings
        
        // Validate and fix any invalid values
        if (settings.dailyLimitMinutes < 0) {
            validatedSettings = validatedSettings.copy(dailyLimitMinutes = 120) // 2 hours default
        }
        
        if (settings.streakCount < 0) {
            validatedSettings = validatedSettings.copy(streakCount = 0)
        }
        
        // Ensure selected apps list is not null
        if (settings.selectedApps.isEmpty() && source == "main") {
            // Try to load from legacy storage
            val legacyApps = loadLegacySelectedApps()
            if (legacyApps.isNotEmpty()) {
                validatedSettings = validatedSettings.copy(selectedApps = legacyApps)
                println("✅ Migrated ${legacyApps.size} apps from legacy storage")
            }
        }
        
        // Save corrected settings if we made changes
        if (validatedSettings != settings) {
            saveSettings(validatedSettings)
        }
        
        println("✅ Settings loaded and validated from $source")
        return validatedSettings
    }
    
    /**
     * Load legacy selected apps from old storage format
     */
    private fun loadLegacySelectedApps(): List<String> {
        return try {
            val legacyPrefs = context.getSharedPreferences("awaytime_prefs", Context.MODE_PRIVATE)
            val appsSet = legacyPrefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
            appsSet.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get default settings
     */
    private fun getDefaultSettings(): AppSettings {
        return AppSettings(
            dailyLimitMinutes = 120, // 2 hours
            notificationsEnabled = true,
            isPremium = false,
            streakCount = 0,
            lastStreakDate = null,
            selectedApps = emptyList(),
            appGroupName = "My Apps"
        )
    }
    
    /**
     * Backup current settings manually
     */
    suspend fun createManualBackup() {
        withContext(Dispatchers.IO) {
            try {
                val currentSettings = loadSettings()
                
                // Create timestamped backup
                val timestamp = System.currentTimeMillis()
                val timestampedPrefs = context.getSharedPreferences(
                    "awaytime_settings_${timestamp}", 
                    Context.MODE_PRIVATE
                )
                
                timestampedPrefs.edit()
                    .putString(USER_PREFERENCES, gson.toJson(currentSettings))
                    .putLong("backup_timestamp", timestamp)
                    .apply()
                
                // Keep only last 5 backups to avoid storage bloat
                cleanupOldBackups()
                
                println("✅ Manual settings backup created")
                
            } catch (e: Exception) {
                println("❌ Manual backup failed: ${e.message}")
            }
        }
    }
    
    /**
     * Clean up old backup files
     */
    private fun cleanupOldBackups() {
        try {
            val prefsDir = context.getSharedPreferences("temp", Context.MODE_PRIVATE).let { prefs ->
                prefs.edit().putString("temp", "temp").apply()
                val prefsFile = context.getSharedPrefsFile("temp")
                prefsFile.parentFile
            }
            
            prefsDir?.listFiles()?.filter { file ->
                file.name.startsWith("awaytime_settings_") && 
                file.name.endsWith(".xml") &&
                file.name != "awaytime_settings_backup.xml"
            }?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { file ->
                try {
                    file.delete()
                    println("🗑️ Deleted old backup: ${file.name}")
                } catch (e: Exception) {
                    println("⚠️ Failed to delete old backup: ${file.name}")
                }
            }
        } catch (e: Exception) {
            println("⚠️ Cleanup of old backups failed: ${e.message}")
        }
    }
    
    /**
     * Check if settings are corrupted
     */
    fun checkSettingsIntegrity(): Boolean {
        return try {
            val settings = loadSettings()
            
            // Basic integrity checks
            settings.dailyLimitMinutes >= 0 && 
            settings.streakCount >= 0 &&
            settings.appGroupName.isNotBlank()
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Data class for app settings
     */
    data class AppSettings(
        val dailyLimitMinutes: Int = 120,
        val notificationsEnabled: Boolean = true,
        val isPremium: Boolean = false,
        val streakCount: Int = 0,
        val lastStreakDate: Long? = null,
        val selectedApps: List<String> = emptyList(),
        val appGroupName: String = "My Apps"
    )
}

/**
 * Extension function for Context to get shared preferences file
 */
private fun Context.getSharedPrefsFile(name: String): java.io.File {
    return java.io.File(applicationInfo.dataDir, "shared_prefs/$name.xml")
}