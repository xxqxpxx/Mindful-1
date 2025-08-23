package com.awaytime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "usage_records")
data class UsageRecordEntity(
    @PrimaryKey val id: String,
    val date: Long, // Timestamp
    val usageMinutes: Int,
    val appGroupName: String,
    val limitExceeded: Boolean,
    val pickupCount: Int = 0
) {
    constructor(
        date: Date,
        usageMinutes: Int,
        appGroupName: String,
        limitExceeded: Boolean,
        pickupCount: Int = 0
    ) : this(
        id = java.util.UUID.randomUUID().toString(),
        date = date.time,
        usageMinutes = usageMinutes,
        appGroupName = appGroupName,
        limitExceeded = limitExceeded,
        pickupCount = pickupCount
    )

    fun getDateAsDate(): Date = Date(date)
}