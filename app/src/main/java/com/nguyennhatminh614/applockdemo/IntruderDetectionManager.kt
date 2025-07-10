package com.nguyennhatminh614.applockdemo

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Manager để quản lý cài đặt và dữ liệu phát hiện kẻ đột nhập
 */
class IntruderDetectionManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val intruderRecordDao = database.intruderRecordDao()
    
    companion object {
        private const val PREFS_NAME = "intruder_detection_prefs"
        private const val KEY_ENABLED = "intruder_detection_enabled"
        private const val KEY_ATTEMPT_THRESHOLD = "attempt_threshold"
        
        private const val DEFAULT_ATTEMPT_THRESHOLD = 3
        private const val MAX_RECORDS = 100 // Giới hạn số lượng bản ghi
    }
    
    /**
     * Bật/tắt tính năng phát hiện kẻ đột nhập
     */
    fun setIntruderDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Kiểm tra xem tính năng phát hiện kẻ đột nhập có được bật không
     */
    fun isIntruderDetectionEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENABLED, false)
    }
    
    /**
     * Đặt số lần thử sai PIN trước khi chụp ảnh
     */
    fun setAttemptThreshold(threshold: Int) {
        sharedPreferences.edit()
            .putInt(KEY_ATTEMPT_THRESHOLD, threshold)
            .apply()
    }
    
    /**
     * Lấy số lần thử sai PIN trước khi chụp ảnh
     */
    fun getAttemptThreshold(): Int {
        return sharedPreferences.getInt(KEY_ATTEMPT_THRESHOLD, DEFAULT_ATTEMPT_THRESHOLD)
    }
    
    /**
     * Lưu bản ghi kẻ đột nhập (suspend function)
     */
    suspend fun saveIntruderRecord(record: IntruderRecord) {
        intruderRecordDao.insertIntruderRecord(record)
        
        // Giới hạn số lượng bản ghi (chỉ giữ MAX_RECORDS bản ghi gần nhất)
        intruderRecordDao.deleteOldRecords(MAX_RECORDS)
    }
    
    /**
     * Lấy danh sách bản ghi kẻ đột nhập dưới dạng Flow (reactive)
     */
    fun getIntruderRecordsFlow(): Flow<List<IntruderRecord>> {
        return intruderRecordDao.getAllIntruderRecords()
    }
    
    /**
     * Lấy danh sách bản ghi kẻ đột nhập (suspend function)
     */
    suspend fun getIntruderRecords(): List<IntruderRecord> {
        return intruderRecordDao.getAllIntruderRecordsList()
    }
    
    /**
     * Xóa tất cả bản ghi kẻ đột nhập (suspend function)
     */
    suspend fun clearIntruderRecords() {
        intruderRecordDao.deleteAllIntruderRecords()
    }
    
    /**
     * Xóa một bản ghi cụ thể (suspend function)
     */
    suspend fun deleteIntruderRecord(recordId: Long) {
        intruderRecordDao.deleteIntruderRecordById(recordId)
    }
    
    /**
     * Lấy số lượng bản ghi kẻ đột nhập (suspend function)
     */
    suspend fun getIntruderRecordsCount(): Int {
        return intruderRecordDao.getIntruderRecordsCount()
    }
    
    /**
     * Lấy bản ghi kẻ đột nhập theo app package name (suspend function)
     */
    suspend fun getIntruderRecordsByApp(packageName: String): List<IntruderRecord> {
        return intruderRecordDao.getIntruderRecordsByApp(packageName)
    }
}