package com.nguyennhatminh614.applockdemo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.andrognito.pinlockview.PinLockView

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
    
    private var attemptCount = 0
    private val maxAttempts = 3
    
    private val pinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) {
            if (pinManager.validatePin(pin)) {
                // PIN is correct
                Toast.makeText(this@AuthenticatePinActivity, getString(R.string.pin_verified_success), Toast.LENGTH_SHORT).show()
                
                // Reset the background timer
                AppLockManager.getInstance().resetBackgroundTimer()
                
                // Navigate to main activity
                val intent = Intent(this@AuthenticatePinActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // PIN is incorrect
                attemptCount++
                
                if (attemptCount >= maxAttempts) {
                    // Max attempts reached
                    Toast.makeText(this@AuthenticatePinActivity, 
                        "Đã nhập sai PIN $maxAttempts lần. Vui lòng thử lại sau.", 
                        Toast.LENGTH_LONG).show()
                    
                    // You can implement additional security measures here
                    // like temporary lockout, biometric fallback, etc.
                    finish()
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
        
        initViews()
        setupPinLockView()
        
        pinManager = PinManager(this)
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
        moveTaskToBack(true)
    }
}