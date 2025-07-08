package com.nguyennhatminh614.applockdemo

import android.content.Context
import android.content.SharedPreferences

class AppLockConfig(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("app_lock_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_LOCKED_APPS = "locked_apps"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        
        // Một số ứng dụng mặc định có thể cần khóa (ví dụ)
        val DEFAULT_LOCKED_APPS = setOf(
            "com.facebook.katana", // Facebook
            "com.instagram.android", // Instagram
            "com.whatsapp", // WhatsApp
            "com.google.android.gm", // Gmail
            "com.android.chrome" // Chrome
        )
    }
    
    /**
     * Lấy danh sách các ứng dụng cần khóa
     */
    fun getLockedApps(): Set<String> {
        return sharedPreferences.getStringSet(KEY_LOCKED_APPS, DEFAULT_LOCKED_APPS) ?: emptySet()
    }
    
    /**
     * Thêm ứng dụng vào danh sách khóa
     */
    fun addLockedApp(packageName: String) {
        val currentApps = getLockedApps().toMutableSet()
        currentApps.add(packageName)
        saveLockedApps(currentApps)
    }
    
    /**
     * Xóa ứng dụng khỏi danh sách khóa
     */
    fun removeLockedApp(packageName: String) {
        val currentApps = getLockedApps().toMutableSet()
        currentApps.remove(packageName)
        saveLockedApps(currentApps)
    }
    
    /**
     * Kiểm tra xem ứng dụng có cần khóa không
     */
    fun isAppLocked(packageName: String): Boolean {
        return getLockedApps().contains(packageName)
    }
    
    /**
     * Lưu danh sách ứng dụng khóa
     */
    private fun saveLockedApps(apps: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_LOCKED_APPS, apps)
            .apply()
    }
    
    /**
     * Bật/tắt tính năng khóa ứng dụng
     */
    fun setAppLockEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Kiểm tra xem tính năng khóa ứng dụng có được bật không
     */
    fun isAppLockEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, true)
    }
    
    /**
     * Xóa tất cả cấu hình
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}