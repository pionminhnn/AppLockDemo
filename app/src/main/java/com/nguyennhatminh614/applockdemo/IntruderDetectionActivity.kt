package com.nguyennhatminh614.applockdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity để cấu hình và xem lịch sử phát hiện kẻ đột nhập
 */
class IntruderDetectionActivity : AppCompatActivity() {
    
    private lateinit var switchIntruderDetection: Switch
    private lateinit var spinnerAttemptThreshold: Spinner
    private lateinit var recyclerViewIntruders: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvClearAll: TextView
    private lateinit var ivBack: ImageView
    
    private lateinit var intruderDetectionManager: IntruderDetectionManager
    private lateinit var cameraManager: CameraManager
    private lateinit var adapter: IntruderRecordAdapter
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intruder_detection)
        
        initViews()
        initManagers()
        setupViews()
        loadData()
    }
    
    private fun initViews() {
        switchIntruderDetection = findViewById(R.id.switchIntruderDetection)
        spinnerAttemptThreshold = findViewById(R.id.spinnerAttemptThreshold)
        recyclerViewIntruders = findViewById(R.id.recyclerViewIntruders)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvClearAll = findViewById(R.id.tvClearAll)
        ivBack = findViewById(R.id.ivBack)
    }
    
    private fun initManagers() {
        intruderDetectionManager = IntruderDetectionManager(this)
        cameraManager = CameraManager(this)
    }
    
    private fun setupViews() {
        // Setup back button
        ivBack.setOnClickListener {
            finish()
        }
        
        // Setup switch
        switchIntruderDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkCameraPermissionAndEnable()
            } else {
                intruderDetectionManager.setIntruderDetectionEnabled(false)
            }
        }
        
        // Setup spinner
        setupAttemptThresholdSpinner()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup clear all button
        tvClearAll.setOnClickListener {
            showClearAllDialog()
        }
    }
    
    private fun setupAttemptThresholdSpinner() {
        val attempts = arrayOf("1", "2", "3", "4", "5", "6")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, attempts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAttemptThreshold.adapter = adapter
        
        spinnerAttemptThreshold.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val threshold = position + 1 // position 0 = 1 attempt, position 1 = 2 attempts, etc.
                intruderDetectionManager.setAttemptThreshold(threshold)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupRecyclerView() {
        adapter = IntruderRecordAdapter(
            context = this,
            records = mutableListOf(),
            onDeleteClick = { record ->
                showDeleteDialog(record)
            }
        )
        
        recyclerViewIntruders.layoutManager = LinearLayoutManager(this)
        recyclerViewIntruders.adapter = adapter
    }
    
    private fun loadData() {
        // Load current settings
        switchIntruderDetection.isChecked = intruderDetectionManager.isIntruderDetectionEnabled()
        
        val currentThreshold = intruderDetectionManager.getAttemptThreshold()
        spinnerAttemptThreshold.setSelection(currentThreshold - 1) // Convert to 0-based index
        

    }

    override fun onResume() {
        super.onResume()
        // Load intruder records
        loadIntruderRecords()
    }

    private fun loadIntruderRecords() {
        lifecycleScope.launch {
            try {
                val records = intruderDetectionManager.getIntruderRecords()
                runOnUiThread {
                    adapter.updateRecords(records)
                    
                    // Show/hide empty state
                    if (records.isEmpty()) {
                        recyclerViewIntruders.visibility = View.GONE
                        tvEmptyState.visibility = View.VISIBLE
                    } else {
                        recyclerViewIntruders.visibility = View.VISIBLE
                        tvEmptyState.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@IntruderDetectionActivity, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun checkCameraPermissionAndEnable() {
        if (cameraManager.hasCameraPermission()) {
            intruderDetectionManager.setIntruderDetectionEnabled(true)
        } else {
            // Request camera permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    intruderDetectionManager.setIntruderDetectionEnabled(true)
                    Toast.makeText(this, "Quyền camera đã được cấp", Toast.LENGTH_SHORT).show()
                } else {
                    switchIntruderDetection.isChecked = false
                    Toast.makeText(this, "Cần quyền camera để sử dụng tính năng này", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showDeleteDialog(record: IntruderRecord) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bản ghi")
            .setMessage("Bạn có chắc chắn muốn xóa bản ghi này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteRecord(record)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun showClearAllDialog() {
        lifecycleScope.launch {
            try {
                val records = intruderDetectionManager.getIntruderRecords()
                runOnUiThread {
                    if (records.isEmpty()) {
                        Toast.makeText(this@IntruderDetectionActivity, "Không có bản ghi nào để xóa", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    
                    AlertDialog.Builder(this@IntruderDetectionActivity)
                        .setTitle("Xóa tất cả")
                        .setMessage("Bạn có chắc chắn muốn xóa tất cả bản ghi?")
                        .setPositiveButton("Xóa tất cả") { _, _ ->
                            clearAllRecords()
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@IntruderDetectionActivity, "Lỗi khi kiểm tra dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun deleteRecord(record: IntruderRecord) {
        lifecycleScope.launch {
            try {
                // Delete image file if exists
                record.imagePath?.let { imagePath ->
                    val file = File(imagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                
                // Delete from database
                intruderDetectionManager.deleteIntruderRecord(record.id)
                
                runOnUiThread {
                    // Update UI
                    adapter.removeRecord(record)
                    loadIntruderRecords() // Refresh to update empty state
                    
                    Toast.makeText(this@IntruderDetectionActivity, "Đã xóa bản ghi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@IntruderDetectionActivity, "Lỗi khi xóa bản ghi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun clearAllRecords() {
        lifecycleScope.launch {
            try {
                val records = intruderDetectionManager.getIntruderRecords()
                
                // Delete all image files
                records.forEach { record ->
                    record.imagePath?.let { imagePath ->
                        val file = File(imagePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
                
                // Clear from database
                intruderDetectionManager.clearIntruderRecords()
                
                runOnUiThread {
                    // Update UI
                    loadIntruderRecords()
                    
                    Toast.makeText(this@IntruderDetectionActivity, "Đã xóa tất cả bản ghi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@IntruderDetectionActivity, "Lỗi khi xóa dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}