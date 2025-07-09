package com.nguyennhatminh614.applockdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), AppDetectionService.AppChangeListener {
    
    private lateinit var pinManager: PinManager
    private lateinit var permissionManager: PermissionManager
    private var isServiceStarted = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        pinManager = PinManager(this)
        permissionManager = PermissionManager(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupButtons()
        checkPermissionsAndStartService()
    }
    
    private fun setupButtons() {
        // Add buttons to test PIN functionality
        val setupPinButton = findViewById<Button>(R.id.btnSetupPin)
        val authenticatePinButton = findViewById<Button>(R.id.btnAuthenticatePin)
        val clearPinButton = findViewById<Button>(R.id.btnClearPin)
        val selectAppsButton = findViewById<Button>(R.id.btnSelectApps)
        val lockAppButton = findViewById<Button>(R.id.btnLockApp)
        
        setupPinButton?.setOnClickListener {
            val intent = Intent(this, SetupPinActivity::class.java)
            startActivity(intent)
        }
        
        authenticatePinButton?.setOnClickListener {
            if (pinManager.isPinSet()) {
                val intent = Intent(this, AuthenticatePinActivity::class.java)
                startActivity(intent)
            } else {
                // No PIN set, go to setup
                val intent = Intent(this, SetupPinActivity::class.java)
                startActivity(intent)
            }
        }
        
        clearPinButton?.setOnClickListener {
            pinManager.clearPin()
            android.widget.Toast.makeText(this, "PIN đã được xóa", android.widget.Toast.LENGTH_SHORT).show()
            updatePinStatus()
        }
        
        selectAppsButton?.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            startActivity(intent)
        }
        
        lockAppButton?.setOnClickListener {
            // Manually trigger app lock
            AppLockManager.getInstance().lockApp(this)
        }
        
        // Thêm button để kiểm tra quyền
        val checkPermissionsButton = findViewById<Button>(R.id.btnCheckPermissions)
        checkPermissionsButton?.setOnClickListener {
            checkPermissionsAndStartService()
        }
        
        // Thêm button để test service
        val testServiceButton = findViewById<Button>(R.id.btnTestService)
        testServiceButton?.setOnClickListener {
            testServiceDetection()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if PIN is required when app comes to foreground
        // This is where you would implement app lock logic
        // For demo purposes, we'll just show the current PIN status
        updatePinStatus()
    }
    
    private fun updatePinStatus() {
        val statusText = findViewById<android.widget.TextView>(R.id.tvPinStatus)
        statusText?.text = if (pinManager.isPinSet()) {
            "PIN đã được thiết lập"
        } else {
            "Chưa thiết lập PIN"
        }
    }
    
    private fun checkPermissionsAndStartService() {
        permissionManager.checkAndRequestAllPermissions {
            startAppDetectionService()
        }
    }
    
    private fun startAppDetectionService() {
        if (!isServiceStarted) {
            val serviceIntent = Intent(this, AppDetectionService::class.java)
            startService(serviceIntent)
            isServiceStarted = true
            
            // Đăng ký listener để nhận thông báo khi ứng dụng thay đổi
            AppDetectionService.setAppChangeListener(this)
            
            Toast.makeText(this, "Service phát hiện ứng dụng đã được khởi động", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "AppDetectionService started")
        }
    }
    
    private fun testServiceDetection() {
        if (!permissionManager.hasUsageStatsPermission()) {
            Toast.makeText(this, "Chưa có quyền Usage Stats! Vui lòng cấp quyền trước.", Toast.LENGTH_LONG).show()
            return
        }
        
        Toast.makeText(this, "Đang test service detection... Hãy chuyển sang app khác và quay lại", Toast.LENGTH_LONG).show()
        Log.d("MainActivity", "Testing service detection...")
        
        // Trigger manual check nếu service đã chạy
        if (isServiceStarted) {
            Log.d("MainActivity", "Service is running, manual detection should work")
        } else {
            Toast.makeText(this, "Service chưa được khởi động! Hãy khởi động service trước.", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        permissionManager.handleActivityResult(requestCode) { hasPermission ->
            if (hasPermission) {
                Toast.makeText(this, "Quyền đã được cấp", Toast.LENGTH_SHORT).show()
                checkPermissionsAndStartService()
            } else {
                Toast.makeText(this, "Cần cấp quyền để ứng dụng hoạt động", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Implement AppChangeListener
    override fun onAppChanged(packageName: String, appName: String) {
        runOnUiThread {
            Log.d("MainActivity", "App changed to: $appName ($packageName)")
            Toast.makeText(this, "Ứng dụng hiện tại: $appName", Toast.LENGTH_SHORT).show()
            
            // Ở đây bạn có thể thêm logic để kiểm tra xem ứng dụng có cần khóa không
            // Ví dụ: nếu ứng dụng trong danh sách cần khóa thì hiển thị màn hình PIN
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký listener
        AppDetectionService.setAppChangeListener(null)
    }
}