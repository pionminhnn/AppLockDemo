package com.nguyennhatminh614.applockdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var pinManager: PinManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        pinManager = PinManager(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupButtons()
    }
    
    private fun setupButtons() {
        // Add buttons to test PIN functionality
        val setupPinButton = findViewById<Button>(R.id.btnSetupPin)
        val authenticatePinButton = findViewById<Button>(R.id.btnAuthenticatePin)
        val clearPinButton = findViewById<Button>(R.id.btnClearPin)
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
        
        lockAppButton?.setOnClickListener {
            // Manually trigger app lock
            AppLockManager.getInstance().lockApp(this)
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
}