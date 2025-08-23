package com.awaytime.app.data.dao

import androidx.room.*
import com.awaytime.app.data.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 'user_settings' LIMIT 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 'user_settings' LIMIT 1")
    suspend fun getUserSettingsSync(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)

    @Update
    suspend fun updateUserSettings(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET isPremium = :isPremium WHERE id = 'user_settings'")
    suspend fun updatePremiumStatus(isPremium: Boolean)

    @Query("UPDATE user_settings SET streakCount = :streakCount, lastStreakDate = :lastStreakDate WHERE id = 'user_settings'")
    suspend fun updateStreak(streakCount: Int, lastStreakDate: Long?)

    @Query("UPDATE user_settings SET notificationsEnabled = :enabled WHERE id = 'user_settings'")
    suspend fun updateNotificationsEnabled(enabled: Boolean)

    // Create default settings if they don't exist
    suspend fun getOrCreateUserSettings(): UserSettingsEntity {
        return getUserSettingsSync() ?: run {
            val defaultSettings = UserSettingsEntity()
            insertUserSettings(defaultSettings)
            defaultSettings
        }
    }
}