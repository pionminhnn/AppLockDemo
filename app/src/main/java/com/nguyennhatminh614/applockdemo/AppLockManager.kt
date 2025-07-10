package com.nguyennhatminh614.applockdemo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Manager class to handle automatic app locking functionality
 * This class monitors app lifecycle and triggers PIN authentication when needed
 */
class AppLockManager private constructor() : Application.ActivityLifecycleCallbacks {
    
    private var isAppInBackground = false
    private var backgroundTime: Long = 0
    private val lockTimeoutMs = 30000L // 30 seconds
    
    companion object {
        @Volatile
        private var INSTANCE: AppLockManager? = null
        
        fun getInstance(): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLockManager().also { INSTANCE = it }
            }
        }
    }
    
    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Not needed for our use case
    }
    
    override fun onActivityStarted(activity: Activity) {
        // Not needed for our use case
    }
    
    override fun onActivityResumed(activity: Activity) {
        // App came to foreground
        if (isAppInBackground) {
            isAppInBackground = false
            
            // Check if app was in background for too long
            val currentTime = System.currentTimeMillis()
            if (currentTime - backgroundTime > lockTimeoutMs) {
                // App was in background for too long, require PIN authentication
                showPinAuthentication(activity)
            }
        }
    }
    
    override fun onActivityPaused(activity: Activity) {
        // Not needed for our use case
    }
    
    override fun onActivityStopped(activity: Activity) {
        // App went to background
        isAppInBackground = true
        backgroundTime = System.currentTimeMillis()
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Not needed for our use case
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        // Not needed for our use case
    }
    
    private fun showPinAuthentication(activity: Activity) {
        // Don't show PIN authentication if we're already on PIN-related activities
        if (activity is SetupPinActivity || activity is AuthenticatePinActivity) {
            return
        }
        
        val pinManager = PinManager(activity)
        
        // Only show authentication if PIN is set
        if (pinManager.isPinSet()) {
            val intent = Intent(activity, AuthenticatePinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            activity.startActivity(intent)
        }
    }
    
    /**
     * Manually trigger PIN authentication
     * Useful for testing or manual app lock
     */
    fun lockApp(context: Context, appPackageName: String? = null, appName: String? = null) {
        val pinManager = PinManager(context)
        
        if (pinManager.isPinSet()) {
            val intent = Intent(context, AuthenticatePinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            
            // Truyền thông tin app nếu có
            appPackageName?.let { intent.putExtra("app_package_name", it) }
            appName?.let { intent.putExtra("app_name", it) }
            
            context.startActivity(intent)
        } else {
            // No PIN set, redirect to setup
            val intent = Intent(context, SetupPinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
    
    /**
     * Reset the background timer
     * Useful when user successfully authenticates
     */
    fun resetBackgroundTimer() {
        isAppInBackground = false
        backgroundTime = 0
    }
}