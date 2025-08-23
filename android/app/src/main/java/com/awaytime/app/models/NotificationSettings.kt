package com.awaytime.app.models

/**
 * Settings for notification management and batching.
 * Adapted from Mindful's notification system.
 */
data class NotificationSettings(
    /**
     * Apps whose notifications should be batched/dismissed during focus time
     */
    val batchedApps: Set<String> = emptySet(),
    
    /**
     * Whether to store notifications from non-batched apps for history
     */
    val storeNonBatchedToo: Boolean = false,
    
    /**
     * Whether notification batching is enabled
     */
    val batchingEnabled: Boolean = false,
    
    /**
     * Whether to show notification summaries
     */
    val showSummaries: Boolean = true,
    
    /**
     * How often to deliver batched notifications (in minutes)
     */
    val batchDeliveryIntervalMinutes: Int = 60,
    
    /**
     * Whether to automatically batch notifications during focus sessions
     */
    val autoBatchDuringFocus: Boolean = true
)