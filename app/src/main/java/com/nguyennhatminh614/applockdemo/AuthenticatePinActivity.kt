package com.nguyennhatminh614.applockdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.andrognito.pinlockview.PinLockView
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * Activity for PIN authentication
 * User enters PIN to unlock the app
 */
class AuthenticatePinActivity : AppCompatActivity() {
    
    private lateinit var pinLockView: PinLockView
    private lateinit var indicatorDots: IndicatorDots
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var forgotPinTextView: TextView
    
    private lateinit var pinManager: PinManager
    private lateinit var intruderDetectionManager: IntruderDetectionManager
    private lateinit var cameraManager: CameraManager
    
    private var attemptCount = 0
    private val maxAttempts = 3
    
    // Thông tin về app hiện tại (được truyền qua Intent)
    private var currentAppPackageName: String? = null
    private var currentAppName: String? = null
    
    companion object {
        private const val TAG = "AuthenticatePinActivity"
        private var currentInstance: AuthenticatePinActivity? = null
        
        /**
         * Thông báo cho activity hiện tại rằng launcher đã được detect
         */
        fun notifyLauncherDetected() {
            currentInstance?.let { activity ->
                Log.d(TAG, "Launcher detected, finishing AuthenticatePinActivity")
                activity.finishAndRemoveTask()
            }
        }
    }
    
    private val pinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) {
            if (pinManager.validatePin(pin)) {
                // PIN is correct
                Toast.makeText(this@AuthenticatePinActivity, getString(R.string.pin_verified_success), Toast.LENGTH_SHORT).show()
                
                // Reset the background timer
                AppLockManager.getInstance().resetBackgroundTimer()
                
                // Navigate to main activity
                finishAndRemoveTask()
            } else {
                // PIN is incorrect
                attemptCount++
                
                // Kiểm tra xem có cần chụp ảnh kẻ đột nhập không
                checkAndCaptureIntruderPhoto()
                
                if (attemptCount >= maxAttempts) {
                    // Max attempts reached
                    Toast.makeText(this@AuthenticatePinActivity, 
                        "Đã nhập sai PIN $maxAttempts lần. Vui lòng thử lại sau.", 
                        Toast.LENGTH_LONG).show()
                    
                    // You can implement additional security measures here
                    // like temporary lockout, biometric fallback, etc.
                    finishAndRemoveTask()
                } else {
                    val remainingAttempts = maxAttempts - attemptCount
                    showError("PIN không đúng. Còn lại $remainingAttempts lần thử.")
                    
                    pinLockView.resetPinLockView()
                    updateSubtitle()
                }
            }
        }
        
        override fun onEmpty() {
            // Handle empty PIN if needed
        }
        
        override fun onPinChange(pinLength: Int, intermediatePin: String) {
            // Handle PIN change if needed
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticate_pin)
        
        // Đặt instance hiện tại
        currentInstance = this
        
        initViews()
        setupPinLockView()
        
        pinManager = PinManager(this)
        intruderDetectionManager = IntruderDetectionManager(this)
        cameraManager = CameraManager(this)
        
        // Lấy thông tin app từ Intent
        currentAppPackageName = intent.getStringExtra("app_package_name")
        currentAppName = intent.getStringExtra("app_name")
        
        Log.d(TAG, "AuthenticatePinActivity created")
    }
    
    private fun initViews() {
        pinLockView = findViewById(R.id.pinLockView)
        indicatorDots = findViewById(R.id.indicatorDots)
        titleTextView = findViewById(R.id.tvTitle)
        subtitleTextView = findViewById(R.id.tvSubtitle)
        errorTextView = findViewById(R.id.tvError)
        forgotPinTextView = findViewById(R.id.tvForgotPin)
        
        titleTextView.text = getString(R.string.verify_pin_title)
        updateSubtitle()
        
        // Handle forgot PIN
        forgotPinTextView.setOnClickListener {
            handleForgotPin()
        }
    }
    
    private fun setupPinLockView() {
        pinLockView.attachIndicatorDots(indicatorDots)
        pinLockView.setPinLockListener(pinLockListener)
        pinLockView.setPinLength(4)
    }
    
    private fun updateSubtitle() {
        if (attemptCount == 0) {
            subtitleTextView.text = getString(R.string.verify_pin_subtitle)
            errorTextView.visibility = android.view.View.GONE
        } else {
            val remainingAttempts = maxAttempts - attemptCount
            subtitleTextView.text = getString(R.string.verify_pin_subtitle)
        }
    }
    
    private fun showError(message: String) {
        errorTextView.text = message
        errorTextView.visibility = android.view.View.VISIBLE
    }
    
    private fun handleForgotPin() {
        // Show dialog or navigate to reset PIN flow
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.forgot_pin_dialog_title))
            .setMessage(getString(R.string.forgot_pin_dialog_message))
            .setPositiveButton(getString(R.string.reset)) { _, _ ->
                // Clear current PIN and go to setup
                pinManager.clearPin()
                val intent = Intent(this, SetupPinActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onBackPressed() {
        // Prevent going back from authentication screen
        // You can customize this behavior based on your app's requirements
        //moveTaskToBack(true)
    }
    
    /**
     * Kiểm tra và chụp ảnh kẻ đột nhập nếu cần thiết
     */
    private fun checkAndCaptureIntruderPhoto() {
        // Kiểm tra xem tính năng phát hiện kẻ đột nhập có được bật không
        if (!intruderDetectionManager.isIntruderDetectionEnabled()) {
            return
        }
        
        // Kiểm tra xem đã đạt đến ngưỡng số lần thử sai chưa
        val threshold = intruderDetectionManager.getAttemptThreshold()
        if (attemptCount >= threshold) {
            captureIntruderPhoto()
        }
    }
    
    /**
     * Chụp ảnh kẻ đột nhập
     */
    private fun captureIntruderPhoto() {
        Log.d(TAG, "Attempting to capture intruder photo")
        
        cameraManager.captureIntruderPhoto { imagePath ->
            // Lưu bản ghi kẻ đột nhập
            val appPackageName = currentAppPackageName ?: packageName
            val appName = currentAppName ?: "Unknown App"
            
            val intruderRecord = IntruderRecord(
                imagePath = imagePath,
                appPackageName = appPackageName,
                appName = appName,
                attemptCount = attemptCount
            )
            
            // Sử dụng coroutine để lưu vào database
            lifecycleScope.launch {
                try {
                    intruderDetectionManager.saveIntruderRecord(intruderRecord)
                    runOnUiThread {
                        if (imagePath != null) {
                            Log.d(TAG, "Intruder photo captured and saved: $imagePath")
                        } else {
                            Log.d(TAG, "Intruder record saved without photo (camera not available)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving intruder record", e)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Xóa instance hiện tại
        if (currentInstance == this) {
            currentInstance = null
        }
        Log.d(TAG, "AuthenticatePinActivity destroyed")
    }
}