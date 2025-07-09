package com.nguyennhatminh614.applockdemo

import android.graphics.drawable.Drawable

/**
 * Data class to represent an installed application
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    var isLocked: Boolean = false
)