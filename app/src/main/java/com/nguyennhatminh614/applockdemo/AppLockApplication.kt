package com.nguyennhatminh614.applockdemo

import android.app.Application

/**
 * Custom Application class to initialize app-wide components
 */
class AppLockApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the AppLockManager
        AppLockManager.getInstance().initialize(this)
    }
}