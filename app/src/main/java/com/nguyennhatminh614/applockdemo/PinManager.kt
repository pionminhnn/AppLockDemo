package com.nguyennhatminh614.applockdemo

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to manage PIN storage and validation
 */
class PinManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "pin_prefs"
        private const val KEY_PIN = "user_pin"
        private const val KEY_PIN_SET = "is_pin_set"
    }
    
    /**
     * Save PIN to SharedPreferences
     */
    fun savePin(pin: String) {
        sharedPreferences.edit()
            .putString(KEY_PIN, pin)
            .putBoolean(KEY_PIN_SET, true)
            .apply()
    }
    
    /**
     * Check if PIN is already set
     */
    fun isPinSet(): Boolean {
        return sharedPreferences.getBoolean(KEY_PIN_SET, false)
    }
    
    /**
     * Validate entered PIN against saved PIN
     */
    fun validatePin(enteredPin: String): Boolean {
        val savedPin = sharedPreferences.getString(KEY_PIN, "")
        return savedPin == enteredPin
    }
    
    /**
     * Clear saved PIN (for reset functionality)
     */
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN)
            .putBoolean(KEY_PIN_SET, false)
            .apply()
    }
    
    /**
     * Get saved PIN (for debugging purposes - use carefully)
     */
    fun getSavedPin(): String? {
        return sharedPreferences.getString(KEY_PIN, null)
    }
}