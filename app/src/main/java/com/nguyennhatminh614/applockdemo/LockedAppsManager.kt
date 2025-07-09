package com.nguyennhatminh614.applockdemo

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager class to handle locked apps list
 */
class LockedAppsManager private constructor(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "locked_apps_prefs"
        private const val KEY_LOCKED_APPS = "locked_apps"
        
        @Volatile
        private var INSTANCE: LockedAppsManager? = null
        
        fun getInstance(context: Context): LockedAppsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LockedAppsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Add an app to the locked apps list
     */
    fun lockApp(packageName: String) {
        val lockedApps = getLockedApps().toMutableSet()
        lockedApps.add(packageName)
        saveLockedApps(lockedApps)
    }
    
    /**
     * Remove an app from the locked apps list
     */
    fun unlockApp(packageName: String) {
        val lockedApps = getLockedApps().toMutableSet()
        lockedApps.remove(packageName)
        saveLockedApps(lockedApps)
    }
    
    /**
     * Check if an app is locked
     */
    fun isAppLocked(packageName: String): Boolean {
        return getLockedApps().contains(packageName)
    }
    
    /**
     * Get all locked apps
     */
    fun getLockedApps(): Set<String> {
        return sharedPreferences.getStringSet(KEY_LOCKED_APPS, emptySet()) ?: emptySet()
    }
    
    /**
     * Save locked apps to SharedPreferences
     */
    private fun saveLockedApps(lockedApps: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_LOCKED_APPS, lockedApps)
            .apply()
    }
    
    /**
     * Clear all locked apps
     */
    fun clearAllLockedApps() {
        sharedPreferences.edit()
            .remove(KEY_LOCKED_APPS)
            .apply()
    }
}