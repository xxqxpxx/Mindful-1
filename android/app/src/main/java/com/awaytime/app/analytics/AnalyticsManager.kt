package com.awaytime.app.analytics

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

// MARK: - Analytics Events

enum class AnalyticsEvent(val eventName: String) {
    // App lifecycle
    APP_LAUNCHED("app_launched"),
    APP_BACKGROUNDED("app_backgrounded"),
    APP_FOREGROUNDED("app_foregrounded"),
    
    // Onboarding
    ONBOARDING_STARTED("onboarding_started"),
    ONBOARDING_COMPLETED("onboarding_completed"),
    
    // Permissions
    PERMISSION_GRANTED("permission_granted"),
    PERMISSION_DENIED("permission_denied"),
    
    // App selection
    APPS_SELECTED("apps_selected"),
    
    // Limits
    LIMIT_SET("limit_set"),
    LIMIT_CHANGED("limit_changed"),
    
    // Usage tracking
    USAGE_TRACKED("usage_tracked"),
    
    // Goals
    GOAL_ACHIEVED("goal_achieved"),
    GOAL_FAILED("goal_failed"),
    
    // Premium features
    PREMIUM_PURCHASED("premium_purchased"),
    PREMIUM_FEATURE_USED("premium_feature_used"),
    TRIAL_EXPIRED("trial_expired"),
    
    // Errors
    ERROR_OCCURRED("error_occurred"),
    
    // Gamification
    ACHIEVEMENT_UNLOCKED("achievement_unlocked"),
    LEVEL_UP("level_up"),
    STREAK_MILESTONE("streak_milestone")
}

// MARK: - Constants

object AnalyticsConstants {
    const val EVENT_APP_LAUNCH = "app_launch"
    const val EVENT_SCREEN_VIEW = "screen_view"
    const val EVENT_BUTTON_CLICK = "button_click"
    const val EVENT_USER_INTERACTION = "user_interaction"
    
    // Parameter keys
    const val PARAM_SCREEN_NAME = "screen_name"
    const val PARAM_BUTTON_NAME = "button_name"
    const val PARAM_ACTION = "action"
    const val PARAM_VALUE = "value"
    const val CATEGORY = "category"
}

// MARK: - Data Classes

@Serializable
data class AnalyticsEventData(
    val event: String,
    val parameters: Map<String, String>,
    val timestamp: Long
)

// MARK: - Analytics Manager

class AnalyticsManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AnalyticsManager"
        private const val PREFS_NAME = "awaytime_analytics"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val KEY_LOCAL_EVENTS = "local_events"
        private const val KEY_USER_PROPERTIES = "user_properties"
        private const val KEY_LAUNCH_COUNT = "launch_count"
        private const val KEY_LAST_VERSION = "last_version"
        private const val MAX_LOCAL_EVENTS = 1000

        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private var sessionStartTime: Long? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    private var isAnalyticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()

    init {
        setupAnalytics()
    }

    // MARK: - Setup

    private fun setupAnalytics() {
        // Initialize analytics with privacy-first approach
        setUserProperty("app_version", getAppVersion())
        setUserProperty("os_version", Build.VERSION.RELEASE)
        setUserProperty("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")

        Log.i(TAG, "Analytics initialized with privacy-first configuration")
    }

    // MARK: - Event Tracking

    fun track(event: AnalyticsEvent, parameters: Map<String, Any> = emptyMap()) {
        if (!isAnalyticsEnabled) return

        val eventParameters = parameters.toMutableMap().apply {
            put("app_version", getAppVersion())
            put("timestamp", System.currentTimeMillis().toString())
        }

        // Convert all values to strings for serialization
        val stringParameters = eventParameters.mapValues { it.value.toString() }

        Log.i(TAG, "Analytics Event: ${event.eventName} with parameters: $stringParameters")

        // Always log locally first (safe operation)
        coroutineScope.launch {
            try {
                logEventLocally(event.eventName, stringParameters)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to log event locally", e)
            }
        }

        // Send to Firebase Analytics with emergency mode protection
        com.awaytime.app.service.EmergencyMode.safeExecuteWithEmergencyCheck(
            operation = {
                coroutineScope.launch {
                    try {
                        // Use crash prevention wrapper for Firebase calls
                        com.awaytime.app.service.CrashPrevention.safeExecute(
                            operation = {
                                firebaseAnalytics.logEvent(event.eventName) {
                                    eventParameters.forEach { (key, value) ->
                                        when (value) {
                                            is String -> param(key, value)
                                            is Int -> param(key, value.toLong())
                                            is Long -> param(key, value)
                                            is Double -> param(key, value)
                                            is Boolean -> param(key, if (value) 1L else 0L)
                                            else -> param(key, value.toString())
                                        }
                                    }
                                }
                            },
                            timeoutMs = 3000L,
                            operationName = "firebase_analytics_log_event"
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to log analytics event to Firebase", e)
                        com.awaytime.app.service.EmergencyMode.recordCrash()
                    }
                }
            },
            fallback = Unit,
            operationName = "track_analytics_event",
            requiresFirebase = true
        )
    }

    // MARK: - User Properties

    fun setUserProperty(name: String, value: String?) {
        if (!isAnalyticsEnabled) return

        val userProperties = getUserProperties().toMutableMap()
        if (value != null) {
            userProperties[name] = value
        } else {
            userProperties.remove(name)
        }

        prefs.edit().putString(KEY_USER_PROPERTIES, json.encodeToString(userProperties)).apply()
        Log.i(TAG, "User property set: $name = $value")
    }

    fun setUserType(type: String) {
        setUserProperty("user_type", type)
    }

    private fun getUserProperties(): Map<String, String> {
        val propertiesJson = prefs.getString(KEY_USER_PROPERTIES, "{}")
        return try {
            json.decodeFromString<Map<String, String>>(propertiesJson ?: "{}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode user properties", e)
            emptyMap()
        }
    }

    // MARK: - Session Tracking

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        track(AnalyticsEvent.APP_LAUNCHED)
    }

    fun endSession() {
        sessionStartTime?.let { startTime ->
            val sessionDuration = System.currentTimeMillis() - startTime
            track(
                AnalyticsEvent.APP_BACKGROUNDED, mapOf(
                    "session_duration" to sessionDuration
                )
            )
        }
        sessionStartTime = null
    }

    fun resumeSession() {
        track(AnalyticsEvent.APP_FOREGROUNDED)
    }

    // MARK: - Convenience Methods

    fun trackOnboardingStep(step: String, completed: Boolean) {
        track(
            if (completed) AnalyticsEvent.ONBOARDING_COMPLETED else AnalyticsEvent.ONBOARDING_STARTED,
            mapOf(
                "step" to step,
                "success" to completed
            )
        )
    }

    fun trackPermissionRequest(permission: String, granted: Boolean) {
        track(
            if (granted) AnalyticsEvent.PERMISSION_GRANTED else AnalyticsEvent.PERMISSION_DENIED,
            mapOf(
                "permission" to permission,
                "success" to granted
            )
        )
    }

    fun trackAppSelection(count: Int) {
        track(
            AnalyticsEvent.APPS_SELECTED, mapOf(
                "count" to count
            )
        )
    }

    fun trackLimitSet(minutes: Int, isFirstTime: Boolean) {
        track(
            if (isFirstTime) AnalyticsEvent.LIMIT_SET else AnalyticsEvent.LIMIT_CHANGED,
            mapOf(
                "value" to minutes,
                "is_first_time" to isFirstTime
            )
        )
    }

    fun trackUsage(minutes: Int, percentage: Double) {
        track(
            AnalyticsEvent.USAGE_TRACKED, mapOf(
                "duration" to minutes,
                "percentage" to percentage
            )
        )
    }

    fun trackGoalResult(achieved: Boolean, streakCount: Int) {
        track(
            if (achieved) AnalyticsEvent.GOAL_ACHIEVED else AnalyticsEvent.GOAL_FAILED,
            mapOf(
                "success" to achieved,
                "streak_count" to streakCount
            )
        )
    }

    fun trackPremiumEvent(event: AnalyticsEvent, feature: String? = null) {
        val parameters = mutableMapOf<String, Any>()
        feature?.let { parameters["feature_name"] = it }
        track(event, parameters)
    }

    fun trackError(error: Throwable, context: String) {
        track(
            AnalyticsEvent.ERROR_OCCURRED, mapOf(
                "error_message" to (error.message ?: "Unknown error"),
                "context" to context,
                "error_type" to error.javaClass.simpleName
            )
        )
    }

    // MARK: - Privacy Controls

    fun enableAnalytics() {
        isAnalyticsEnabled = true
        Log.i(TAG, "Analytics enabled")
    }

    fun disableAnalytics() {
        isAnalyticsEnabled = false
        Log.i(TAG, "Analytics disabled")
    }

    fun isAnalyticsEnabledByUser(): Boolean = isAnalyticsEnabled

    // MARK: - Data Export and Deletion

    fun exportAnalyticsData(): Map<String, Any> {
        return mapOf(
            "user_properties" to getUserProperties(),
            "analytics_enabled" to isAnalyticsEnabled,
            "last_session" to (sessionStartTime ?: 0)
        )
    }

    fun deleteAnalyticsData() {
        prefs.edit()
            .remove(KEY_USER_PROPERTIES)
            .remove(KEY_LOCAL_EVENTS)
            .apply()
        sessionStartTime = null
        Log.i(TAG, "Analytics data deleted")
    }

    // MARK: - Local Logging

    private suspend fun logEventLocally(event: String, parameters: Map<String, String>) {
        try {
            val eventData = AnalyticsEventData(
                event = event,
                parameters = parameters,
                timestamp = System.currentTimeMillis()
            )

            val localEvents = getLocalEvents().toMutableList()
            localEvents.add(eventData)

            // Keep only last MAX_LOCAL_EVENTS to prevent storage bloat
            if (localEvents.size > MAX_LOCAL_EVENTS) {
                localEvents.removeAt(0)
            }

            val eventsJson = json.encodeToString(localEvents)
            prefs.edit().putString(KEY_LOCAL_EVENTS, eventsJson).apply()

        } catch (e: Exception) {
            Log.w(TAG, "Failed to log event locally", e)
        }
    }

    private fun getLocalEvents(): List<AnalyticsEventData> {
        val eventsJson = prefs.getString(KEY_LOCAL_EVENTS, "[]")
        return try {
            json.decodeFromString<List<AnalyticsEventData>>(eventsJson ?: "[]")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode local events", e)
            emptyList()
        }
    }

    // MARK: - Launch Metrics

    fun trackLaunchMetrics() {
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount).apply()

        track(
            AnalyticsEvent.APP_LAUNCHED, mapOf(
                "launch_count" to launchCount,
                "is_first_launch" to (launchCount == 1)
            )
        )

        // Track app version for update analytics
        val currentVersion = getAppVersion()
        val lastVersion = prefs.getString(KEY_LAST_VERSION, null)

        if (lastVersion != currentVersion) {
            track(
                AnalyticsEvent.APP_LAUNCHED, mapOf(
                    "version_updated" to (lastVersion != null),
                    "previous_version" to (lastVersion ?: "none"),
                    "current_version" to currentVersion
                )
            )
            prefs.edit().putString(KEY_LAST_VERSION, currentVersion).apply()
        }
    }

    // MARK: - Utility Methods

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

// MARK: - Compose Integration

@Composable
fun TrackAnalyticsEffect(
    event: AnalyticsEvent,
    parameters: Map<String, Any> = emptyMap()
) {
    val context = LocalContext.current
    val analyticsManager = remember { AnalyticsManager.getInstance(context) }

    DisposableEffect(event) {
        analyticsManager.track(event, parameters)
        onDispose { }
    }
}

// MARK: - Extension Functions

fun Context.getAnalyticsManager(): AnalyticsManager {
    return AnalyticsManager.getInstance(this)
}