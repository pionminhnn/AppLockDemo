package com.nguyennhatminh614.applockdemo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nguyennhatminh614.applockdemo.AppDetectionService
import com.nguyennhatminh614.applockdemo.AppLockConfig

class ScreenReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ScreenReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned on")
                // Có thể thêm logic để kiểm tra app hiện tại khi màn hình bật
                checkCurrentAppIfServiceRunning(context)
            }
            
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen turned off")
                // Có thể thêm logic để xử lý khi màn hình tắt
            }
            
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "User present (unlocked)")
                // Kiểm tra app hiện tại khi user unlock màn hình
                checkCurrentAppIfServiceRunning(context)
            }
            
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed")
                checkCurrentAppIfServiceRunning(context)
            }

            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d(TAG, "Locked boot completed")
                checkCurrentAppIfServiceRunning(context)
            }
        }
    }

    /**
     * Gửi command để kiểm tra app hiện tại nếu service đang chạy
     */
    private fun checkCurrentAppIfServiceRunning(context: Context) {
        try {
            val appLockConfig = AppLockConfig(context)
            
            if (appLockConfig.isAppLockEnabled()) {
                Log.d(TAG, "Sending check_current_app command to service")

                val serviceIntent = Intent(context, AppDetectionService::class.java).apply {
                    action = AppDetectionService.ACTION_CHECK_CURRENT_APP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending check_current_app command", e)
        }
    }
}
