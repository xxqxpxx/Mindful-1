package com.awaytime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "app_groups")
data class AppGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean,
    val createdDate: Long,
    val selectedAppsJson: String // JSON string of selected app package names
) {
    constructor(
        name: String,
        dailyLimitMinutes: Int,
        isActive: Boolean = true,
        selectedAppsJson: String
    ) : this(
        id = java.util.UUID.randomUUID().toString(),
        name = name,
        dailyLimitMinutes = dailyLimitMinutes,
        isActive = isActive,
        createdDate = Date().time,
        selectedAppsJson = selectedAppsJson
    )

    val displayName: String
        get() = name.ifEmpty { "My Apps" }

    fun getCreatedDateAsDate(): Date = Date(createdDate)

    fun getSelectedApps(): List<String> {
        return try {
            com.google.gson.Gson().fromJson(selectedAppsJson, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}