/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.awaytime.app.R
import com.awaytime.app.core.AppConstants
import com.awaytime.app.helpers.SharedPrefsHelper
import com.awaytime.app.utils.NotificationHelper
import com.awaytime.app.utils.ServiceBinder
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketException
import java.nio.channels.DatagramChannel
import java.util.concurrent.atomic.AtomicReference

/**
 * A VPN service that manages internet access by blocking specified apps.
 * This creates a local VPN that only routes blocked apps through it (providing no internet).
 */
class AwayTimeVpnService : VpnService() {
    companion object {
        private const val TAG = "AwayTime.VpnService"
        const val ACTION_UPDATE_BLOCKED_APPS = "com.awaytime.app.action.UPDATE_BLOCKED_APPS"
        
        @Volatile
        private var instance: AwayTimeVpnService? = null
        
        @Synchronized
        fun getInstance(): AwayTimeVpnService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }

    private val serviceBinder = ServiceBinder(this@AwayTimeVpnService)
    private val atomicVpnThread = AtomicReference<Thread?>(null)
    private var blockedApps: Set<String> = HashSet(0)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        synchronized(this) {
            instance = this
        }
        Log.d(TAG, "VPN service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceBinder.ACTION_START_AWAYTIME_SERVICE -> {
                startForegroundService()
                return START_STICKY
            }
            ACTION_UPDATE_BLOCKED_APPS -> {
                val apps = intent?.getStringArrayExtra("blocked_apps")?.toSet() ?: emptySet()
                updateBlockedApps(apps)
                return START_STICKY
            }
            else -> {
                stopAndDisposeService()
                return START_NOT_STICKY
            }
        }
    }

    private fun startForegroundService() {
        if (isServiceRunning) return
        try {
            startForeground(
                AppConstants.VPN_SERVICE_NOTIFICATION_ID,
                NotificationHelper.buildForegroundServiceNotification(
                    this,
                    "AwayTime VPN",
                    getString(R.string.vpn_service_running_message),
                    R.drawable.ic_notification
                )
            )
            isServiceRunning = true
            connectVpn()
            Log.d(TAG, "VPN foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN foreground service", e)
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
            stopAndDisposeService()
        }
    }

    /**
     * Restarts the VPN connection by disconnecting and then reconnecting.
     */
    private fun reconnectVpn() {
        disconnectVpn()
        connectVpn()
        Log.d(TAG, "VPN reconnected successfully")
    }

    /**
     * Establishes a VPN connection based on blocked apps.
     * If there are no blocked apps, the service will stop itself.
     */
    private fun connectVpn() {
        // Check if no blocked apps then STOP service
        if (blockedApps.isEmpty()) {
            Log.w(TAG, "Tried to connect VPN without any blocked apps, stopping service")
            stopAndDisposeService()
            return
        }

        val newThread = Thread(vpnThread, TAG)
        setVpnThread(newThread)
        newThread.start()
    }

    /**
     * Disconnects the VPN connection if established.
     */
    private fun disconnectVpn() {
        try {
            vpnInterface?.close()
            setVpnThread(null)
            Log.d(TAG, "VPN disconnected successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to disconnect VPN", e)
        }
    }

    /**
     * Stops the foreground service and disconnects the VPN.
     */
    private fun stopAndDisposeService() {
        disconnectVpn()
        isServiceRunning = false
        stopSelf()
    }

    /**
     * Returns a Runnable that configures and establishes the VPN connection.
     */
    private val vpnThread: Runnable
        get() = Runnable {
            try {
                DatagramChannel.open().use { tunnel ->
                    check(this@AwayTimeVpnService.protect(tunnel.socket())) { 
                        "Cannot protect the VPN socket tunnel" 
                    }
                    val serverAddress: SocketAddress = InetSocketAddress("localhost", 0)
                    tunnel.connect(serverAddress)
                    tunnel.configureBlocking(false)

                    val builder = this@AwayTimeVpnService.Builder()
                    builder.addAddress("192.168.0.0", 24)
                    builder.addRoute("0.0.0.0", 0)

                    // Add blocked apps to VPN - they will have no internet access
                    for (packageName in blockedApps) {
                        try {
                            builder.addAllowedApplication(packageName)
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.w(TAG, "Cannot find app with package $packageName")
                        }
                    }
                    
                    synchronized(this@AwayTimeVpnService) {
                        vpnInterface = builder.establish()
                        Log.d(TAG, "VPN connected successfully - blocking ${blockedApps.size} apps")
                    }
                }
            } catch (e: SocketException) {
                Log.e(TAG, "Cannot use socket for VPN", e)
                SharedPrefsHelper.insertCrashLogToPrefs(this@AwayTimeVpnService, e)
                stopAndDisposeService()
            } catch (e: IOException) {
                Log.e(TAG, "VPN connection failed", e)
                SharedPrefsHelper.insertCrashLogToPrefs(this@AwayTimeVpnService, e)
                stopAndDisposeService()
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "VPN connection failed - invalid arguments", e)
                SharedPrefsHelper.insertCrashLogToPrefs(this@AwayTimeVpnService, e)
                stopAndDisposeService()
            } catch (e: Exception) {
                Log.e(TAG, "VPN connection failed - unexpected error", e)
                SharedPrefsHelper.insertCrashLogToPrefs(this@AwayTimeVpnService, e)
                stopAndDisposeService()
            }
        }

    /**
     * Sets the current VPN thread, interrupting the previous thread if necessary.
     */
    private fun setVpnThread(thread: Thread?) {
        val oldThread = atomicVpnThread.getAndSet(thread)
        oldThread?.interrupt()
    }

    /**
     * Updates the list of blocked apps and restarts the VPN service if needed.
     */
    fun updateBlockedApps(newBlockedApps: Set<String>) {
        blockedApps = newBlockedApps
        Log.d(TAG, "Internet blocked apps updated: ${blockedApps.size} apps")
        
        if (blockedApps.isEmpty()) {
            stopAndDisposeService()
        } else {
            reconnectVpn()
        }
    }

    /**
     * Gets the current list of blocked apps
     */
    fun getBlockedApps(): Set<String> = blockedApps.toSet()

    override fun onDestroy() {
        synchronized(this) {
            instance = null
        }
        
        disconnectVpn()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "VPN service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == ServiceBinder.ACTION_BIND_TO_AWAYTIME) {
            serviceBinder
        } else null
    }


}