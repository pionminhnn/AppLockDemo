package com.nguyennhatminh614.applockdemo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity để lưu thông tin về kẻ đột nhập trong Room Database
 */
@Entity(tableName = "intruder_records")
data class IntruderRecord(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    val imagePath: String? = null, // Đường dẫn đến ảnh chụp được
    val appPackageName: String, // Package name của app bị đột nhập
    val appName: String, // Tên hiển thị của app bị đột nhập
    val timestamp: Long = System.currentTimeMillis(), // Thời gian xảy ra đột nhập (timestamp)
    val attemptCount: Int = 1 // Số lần thử sai PIN
) {
    /**
     * Chuyển đổi timestamp thành Date object
     */
    fun getTimestampAsDate(): Date = Date(timestamp)
}