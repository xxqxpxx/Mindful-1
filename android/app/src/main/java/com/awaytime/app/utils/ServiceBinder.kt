package com.awaytime.app.utils

import android.app.Service
import android.os.Binder

/**
 * Generic service binder for AwayTime services.
 * Adapted from Mindful's ServiceBinder pattern.
 */
class ServiceBinder(private val service: Service) : Binder() {
    companion object {
        const val ACTION_START_AWAYTIME_SERVICE = "com.awaytime.app.action.START_SERVICE"
        const val ACTION_BIND_TO_AWAYTIME = "com.awaytime.app.action.BIND_SERVICE"
    }
    
    fun getService(): Service = service
}