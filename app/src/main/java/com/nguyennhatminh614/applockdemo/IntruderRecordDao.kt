package com.nguyennhatminh614.applockdemo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) cho IntruderRecord
 */
@Dao
interface IntruderRecordDao {
    
    /**
     * Lấy tất cả bản ghi kẻ đột nhập, sắp xếp theo thời gian mới nhất trước
     */
    @Query("SELECT * FROM intruder_records ORDER BY timestamp DESC")
    fun getAllIntruderRecords(): Flow<List<IntruderRecord>>
    
    /**
     * Lấy tất cả bản ghi kẻ đột nhập dưới dạng List (không phải Flow)
     */
    @Query("SELECT * FROM intruder_records ORDER BY timestamp DESC")
    suspend fun getAllIntruderRecordsList(): List<IntruderRecord>
    
    /**
     * Lấy bản ghi kẻ đột nhập theo ID
     */
    @Query("SELECT * FROM intruder_records WHERE id = :id")
    suspend fun getIntruderRecordById(id: Long): IntruderRecord?
    
    /**
     * Lấy bản ghi kẻ đột nhập theo package name của app
     */
    @Query("SELECT * FROM intruder_records WHERE appPackageName = :packageName ORDER BY timestamp DESC")
    suspend fun getIntruderRecordsByApp(packageName: String): List<IntruderRecord>
    
    /**
     * Lấy số lượng bản ghi kẻ đột nhập
     */
    @Query("SELECT COUNT(*) FROM intruder_records")
    suspend fun getIntruderRecordsCount(): Int
    
    /**
     * Thêm bản ghi kẻ đột nhập mới
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderRecord(record: IntruderRecord)
    
    /**
     * Thêm nhiều bản ghi kẻ đột nhập
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderRecords(records: List<IntruderRecord>)
    
    /**
     * Cập nhật bản ghi kẻ đột nhập
     */
    @Update
    suspend fun updateIntruderRecord(record: IntruderRecord)
    
    /**
     * Xóa bản ghi kẻ đột nhập
     */
    @Delete
    suspend fun deleteIntruderRecord(record: IntruderRecord)
    
    /**
     * Xóa bản ghi kẻ đột nhập theo ID
     */
    @Query("DELETE FROM intruder_records WHERE id = :id")
    suspend fun deleteIntruderRecordById(id: Long)
    
    /**
     * Xóa tất cả bản ghi kẻ đột nhập
     */
    @Query("DELETE FROM intruder_records")
    suspend fun deleteAllIntruderRecords()
    
    /**
     * Xóa các bản ghi cũ, chỉ giữ lại N bản ghi mới nhất
     */
    @Query("DELETE FROM intruder_records WHERE id NOT IN (SELECT id FROM intruder_records ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun deleteOldRecords(limit: Int)
    
    /**
     * Lấy bản ghi kẻ đột nhập trong khoảng thời gian
     */
    @Query("SELECT * FROM intruder_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getIntruderRecordsByTimeRange(startTime: Long, endTime: Long): List<IntruderRecord>
}