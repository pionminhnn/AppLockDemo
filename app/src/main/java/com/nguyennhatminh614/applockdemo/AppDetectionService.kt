package com.nguyennhatminh614.applockdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nguyennhatminh614.applockdemo.receiver.ScreenReceiver

class AppDetectionService : Service() {

    private var screenReceiver: ScreenReceiver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var currentApp: String? = null
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var appLockConfig: AppLockConfig
    private lateinit var lockedAppsManager: LockedAppsManager
    private var systemLauncherPackages: Set<String> = emptySet()

    // Interface để lắng nghe sự thay đổi ứng dụng
    interface AppChangeListener {
        fun onAppChanged(packageName: String, appName: String)
    }

    companion object {
        private const val TAG = "AppDetectionService"
        private const val CHECK_INTERVAL = 500L // 0.5 giây

        val ACTION_CHECK_CURRENT_APP = "ACTION_CHECK_CURRENT_APP"

        // Notification constants
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_detection_channel"
        private const val CHANNEL_NAME = "App Detection Service"

        private var appChangeListener: AppChangeListener? = null
        
        fun setAppChangeListener(listener: AppChangeListener?) {
            appChangeListener = listener
        }
    }
    
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkCurrentApp()
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
    }

    private fun setupScreenReceiver() {
        // Register screen receiver
        screenReceiver = ScreenReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        }
        registerReceiver(screenReceiver, intentFilter)
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        appLockConfig = AppLockConfig(this)
        lockedAppsManager = LockedAppsManager.getInstance(this)
        Log.d(TAG, "AppDetectionService created")
        
        // Tạo notification channel
        createNotificationChannel()
        
        // Kiểm tra quyền Usage Stats
        checkUsageStatsPermission()

        //Detect receiver boot
        setupScreenReceiver()
        
        // Lấy danh sách launcher hệ thống
        systemLauncherPackages = getSystemLauncherPackages()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kênh thông báo cho dịch vụ phát hiện ứng dụng"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Intent để mở MainActivity khi tap vào notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Lock đang hoạt động")
            .setContentText("Đang theo dõi các ứng dụng được khóa")
            .setSmallIcon(R.drawable.ic_lock) // Bạn cần thêm icon này vào drawable
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun checkUsageStatsPermission() {
        try {
            val currentTime = System.currentTimeMillis()
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 24 * 60 * 60 * 1000,
                currentTime
            )
            
            if (usageStats.isEmpty()) {
                Log.w(TAG, "Usage stats is empty - permission might not be granted")
            } else {
                Log.d(TAG, "Usage stats permission is working - found ${usageStats.size} apps")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppDetectionService started")
        
        // Khởi động service ở foreground
        val notification = createNotification()

        startForeground(NOTIFICATION_ID, notification)
        
        // Kiểm tra command từ intent
        //val command = intent?.getStringExtra("command")
        when (intent?.action) {
            ACTION_CHECK_CURRENT_APP -> {
                Log.d(TAG, "Received command to check current app")
                // Delay một chút để đảm bảo hệ thống đã ổn định sau boot
                handler.postDelayed({
                    checkCurrentApp(isForceShow = true)
                }, 2000)
            }
        }
        
        startDetection()
        return START_STICKY // Service sẽ được khởi động lại nếu bị kill
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopDetection()
        screenReceiver?.let { unregisterReceiver(it) }
        Log.d(TAG, "AppDetectionService destroyed")
    }
    
    private fun startDetection() {
        if (!isRunning) {
            isRunning = true
            
            // Chạy ngay lập tức để test
            checkCurrentApp()
            
            // Sau đó chạy theo chu kỳ
            handler.post(checkRunnable)
            Log.d(TAG, "App detection started")
        }
    }
    
    private fun stopDetection() {
        isRunning = false
        handler.removeCallbacks(checkRunnable)
        Log.d(TAG, "App detection stopped")
    }
    
    private fun checkCurrentApp(isForceShow: Boolean = false) {
        try {
            val currentTime = System.currentTimeMillis()
            val startTime = currentTime - 60000 // Tăng lên 60 giây để có nhiều dữ liệu hơn
            
            Log.d(TAG, "Checking usage events from ${startTime} to ${currentTime}")
            
            val usageEvents = usageStatsManager.queryEvents(startTime, currentTime)
            
            var lastEventPackage: String? = null
            var lastEventTime: Long = 0
            var eventCount = 0
            val event = UsageEvents.Event()
            
            // Tìm sự kiện MOVE_TO_FOREGROUND gần nhất
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                eventCount++
                
                Log.v(TAG, "Event: ${event.packageName}, type: ${event.eventType}, time: ${event.timeStamp}")
                
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (event.timeStamp > lastEventTime) {
                        lastEventTime = event.timeStamp
                        lastEventPackage = event.packageName
                        Log.d(TAG, "Found MOVE_TO_FOREGROUND event: ${event.packageName} at ${event.timeStamp}")
                    }
                }
            }
            
            Log.d(TAG, "Total events found: $eventCount, Latest foreground package: $lastEventPackage")
            
            // Nếu không tìm thấy event nào, thử phương pháp khác
            if (lastEventPackage == null) {
                lastEventPackage = getCurrentAppUsingUsageStats()
            }
            
            lastEventPackage?.let { packageName ->
                val isCheckCurrentPackage = if (isForceShow) {
                    true
                } else {
                    packageName != currentApp
                }
                if (isCheckCurrentPackage && packageName != this.packageName) {
                    currentApp = packageName
                    val appName = getAppName(packageName)
                    Log.i(TAG, "Current app changed to: $appName ($packageName)")
                    
                    // Thông báo cho listener
                    appChangeListener?.onAppChanged(packageName, appName)
                    
                    // Kiểm tra nếu latest foreground task là launcher hệ thống
                    if (isSystemLauncher(packageName)) {
                        Log.d(TAG, "System launcher detected: $appName ($packageName)")
                        // Thông báo cho AuthenticatePinActivity để đóng nếu đang mở
                        AuthenticatePinActivity.notifyLauncherDetected()
                    }
                    
                    // Kiểm tra xem ứng dụng có cần khóa không
                    if (appLockConfig.isAppLockEnabled() && lockedAppsManager.isAppLocked(packageName)) {
                        Log.d(TAG, "App $appName needs to be locked")
                        lockApp(packageName, appName)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current app", e)
        }
    }
    
    // Phương pháp backup để lấy app hiện tại
    private fun getCurrentAppUsingUsageStats(): String? {
        try {
            val currentTime = System.currentTimeMillis()
            val usageStatsMap = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 24 * 60 * 60 * 1000, // 24 giờ trước
                currentTime
            )
            
            // Tìm app có lastTimeUsed gần nhất
            var mostRecentApp: String? = null
            var mostRecentTime: Long = 0
            
            for (usageStats in usageStatsMap) {
                if (usageStats.lastTimeUsed > mostRecentTime) {
                    mostRecentTime = usageStats.lastTimeUsed
                    mostRecentApp = usageStats.packageName
                }
            }
            
            Log.d(TAG, "Most recent app from usage stats: $mostRecentApp at $mostRecentTime")
            return mostRecentApp
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current app using usage stats", e)
            return null
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName // Trả về package name nếu không lấy được tên ứng dụng
        }
    }
    
    // Phương thức để lấy ứng dụng hiện tại từ bên ngoài
    fun getCurrentApp(): String? {
        return currentApp
    }
    
    private fun lockApp(packageName: String, appName: String) {
        try {
            // Khởi động màn hình xác thực PIN
            val intent = Intent(this, AuthenticatePinActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("locked_app_package", packageName)
                putExtra("locked_app_name", appName)
            }
            startActivity(intent)
            
            Log.d(TAG, "Launched authentication for app: $appName")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching authentication", e)
        }
    }
    
    /**
     * Lấy danh sách các launcher hệ thống
     */
    private fun getSystemLauncherPackages(): Set<String> {
        val launcherPackages = mutableSetOf<String>()
        
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            
            val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
            
            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.activityInfo.packageName
                launcherPackages.add(packageName)
                Log.d(TAG, "Found launcher package: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting launcher packages", e)
        }
        
        return launcherPackages
    }
    
    /**
     * Kiểm tra xem package có phải là launcher hệ thống không
     */
    private fun isSystemLauncher(packageName: String): Boolean {
        return systemLauncherPackages.contains(packageName)
    }
}