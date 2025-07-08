package com.nguyennhatminh614.applockdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.andrognito.pinlockview.PinLockView

/**
 * Activity for setting up a new PIN
 * User needs to enter PIN twice for confirmation
 */
class SetupPinActivity : AppCompatActivity() {
    
    private lateinit var pinLockView: PinLockView
    private lateinit var indicatorDots: IndicatorDots
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var skipButton: Button
    
    private lateinit var pinManager: PinManager
    
    private var firstPin: String = ""
    private var isConfirmingPin: Boolean = false
    
    private val pinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) {
            if (!isConfirmingPin) {
                // First PIN entry
                firstPin = pin
                isConfirmingPin = true
                updateUI()
                pinLockView.resetPinLockView()
            } else {
                // Confirming PIN
                if (pin == firstPin) {
                    // PINs match, save and proceed
                    pinManager.savePin(pin)
                    Toast.makeText(this@SetupPinActivity, getString(R.string.pin_setup_success), Toast.LENGTH_SHORT).show()
                    
                    // Navigate to main activity or authentication
                    val intent = Intent(this@SetupPinActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // PINs don't match, reset
                    Toast.makeText(this@SetupPinActivity, getString(R.string.pin_mismatch), Toast.LENGTH_SHORT).show()
                    resetPinSetup()
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
        setContentView(R.layout.activity_setup_pin)
        
        initViews()
        setupPinLockView()
        
        pinManager = PinManager(this)
    }
    
    private fun initViews() {
        pinLockView = findViewById(R.id.pinLockView)
        indicatorDots = findViewById(R.id.indicatorDots)
        titleTextView = findViewById(R.id.tvTitle)
        subtitleTextView = findViewById(R.id.tvSubtitle)
        skipButton = findViewById(R.id.btnSkip)
        
        updateUI()
        
        // Handle skip button
        skipButton.setOnClickListener {
            // Navigate to main activity without setting PIN
            val intent = Intent(this@SetupPinActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    private fun setupPinLockView() {
        pinLockView.attachIndicatorDots(indicatorDots)
        pinLockView.setPinLockListener(pinLockListener)
        pinLockView.setPinLength(4)
    }
    
    private fun updateUI() {
        if (!isConfirmingPin) {
            titleTextView.text = getString(R.string.setup_pin_title)
            subtitleTextView.text = getString(R.string.setup_pin_subtitle)
        } else {
            titleTextView.text = getString(R.string.confirm_pin_title)
            subtitleTextView.text = getString(R.string.confirm_pin_subtitle)
        }
    }
    
    private fun resetPinSetup() {
        firstPin = ""
        isConfirmingPin = false
        updateUI()
        pinLockView.resetPinLockView()
    }
    
    override fun onBackPressed() {
        if (isConfirmingPin) {
            // Allow going back to first PIN entry
            resetPinSetup()
        } else {
            // Exit the setup
            super.onBackPressed()
        }
    }
}