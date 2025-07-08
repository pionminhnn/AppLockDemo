package com.nguyennhatminh614.applockdemo

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PermissionManager(private val activity: AppCompatActivity) {
    
    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
        const val REQUEST_CODE_USAGE_STATS_PERMISSION = 1002
    }
    
    /**
     * Kiểm tra quyền vẽ overlay
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
    }
    
    /**
     * Kiểm tra quyền Usage Stats
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = activity.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                activity.packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                activity.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * Yêu cầu quyền vẽ overlay
     */
    fun requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            showOverlayPermissionDialog()
        }
    }
    
    /**
     * Yêu cầu quyền Usage Stats
     */
    fun requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            showUsageStatsPermissionDialog()
        }
    }
    
    /**
     * Kiểm tra và yêu cầu tất cả quyền cần thiết
     */
    fun checkAndRequestAllPermissions(onAllPermissionsGranted: () -> Unit) {
        when {
            !hasOverlayPermission() -> {
                requestOverlayPermission()
            }
            !hasUsageStatsPermission() -> {
                requestUsageStatsPermission()
            }
            else -> {
                onAllPermissionsGranted()
            }
        }
    }
    
    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Cần quyền vẽ overlay")
            .setMessage("Ứng dụng cần quyền vẽ overlay để hiển thị màn hình khóa trên các ứng dụng khác. Vui lòng cấp quyền trong cài đặt.")
            .setPositiveButton("Đi đến cài đặt") { _, _ ->
                openOverlaySettings()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun showUsageStatsPermissionDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Cần quyền truy cập thống kê sử dụng")
            .setMessage("Ứng dụng cần quyền truy cập thống kê sử dụng để phát hiện ứng dụng đang chạy. Vui lòng cấp quyền trong cài đặt.")
            .setPositiveButton("Đi đến cài đặt") { _, _ ->
                openUsageStatsSettings()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    private fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        activity.startActivityForResult(intent, REQUEST_CODE_USAGE_STATS_PERMISSION)
    }
    
    /**
     * Xử lý kết quả từ Activity Settings
     */
    fun handleActivityResult(requestCode: Int, onPermissionResult: (Boolean) -> Unit) {
        when (requestCode) {
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                onPermissionResult(hasOverlayPermission())
            }
            REQUEST_CODE_USAGE_STATS_PERMISSION -> {
                onPermissionResult(hasUsageStatsPermission())
            }
        }
    }
}