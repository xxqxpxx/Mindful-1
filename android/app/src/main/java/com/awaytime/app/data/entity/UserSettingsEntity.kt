package com.awaytime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: String = "user_settings", // Single row table
    val isPremium: Boolean = false,
    val streakCount: Int = 0,
    val lastStreakDate: Long? = null,
    val notificationsEnabled: Boolean = true,
    val createdDate: Long = Date().time
) {
    fun getLastStreakDateAsDate(): Date? = lastStreakDate?.let { Date(it) }

    fun getCreatedDateAsDate(): Date = Date(createdDate)
}