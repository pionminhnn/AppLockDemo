package com.nguyennhatminh614.applockdemo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class SelectAppsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AppSelectionAdapter
    private lateinit var lockedAppsManager: LockedAppsManager
    
    //private val job = SupervisorJob()
    //private val scope = CoroutineScope(Dispatchers.Main + job)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_apps)
        
        lockedAppsManager = LockedAppsManager.getInstance(this)
        
        setupViews()
        loadInstalledApps()
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewApps)
        progressBar = findViewById(R.id.progressBar)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Setup adapter with click listener
        adapter = AppSelectionAdapter { appInfo ->
            toggleAppLockStatus(appInfo)
        }
        recyclerView.adapter = adapter
        
        // Setup toolbar
        supportActionBar?.apply {
            title = "Chọn App cần Lock"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun loadInstalledApps() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) {
                    getInstalledUserApps()
                }

                withContext(Dispatchers.Main) {
                    adapter.updateApps(apps)
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@SelectAppsActivity, "Lỗi khi tải danh sách ứng dụng", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun getInstalledUserApps(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            /*val packageManager = packageManager
            val installedApps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            val userApps = mutableListOf<AppInfo>()
            val lockedApps = lockedAppsManager.getLockedApps()
            
            for (appInfo in installedApps) {
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val isLocked = lockedApps.contains(appInfo.packageName)

                    // Không hiển thị chính app này trong danh sách
                    if (appInfo.packageName != packageName) {
                        userApps.add(
                            AppInfo(
                                packageName = appInfo.packageName,
                                appName = appName,
                                icon = icon,
                                isLocked = isLocked
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Bỏ qua app nếu không thể lấy thông tin
                    continue
                }
            }*/

            val userApps = mutableListOf<AppInfo>()

            val packageManager = application.packageManager
            val apps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            for (app in apps) {
                if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {//check xem app co launcher
                    try {
                        userApps.add(
                            AppInfo(
                                packageName = app.packageName,
                                appName = app.applicationInfo?.loadLabel(packageManager).toString(),
                                icon = packageManager.getApplicationIcon(packageManager.getApplicationInfo(app.packageName, 0)),
                                isLocked = false
                            )
                        )
                    } catch (e: Exception) {
                        // Bỏ qua app nếu không thể lấy thông tin
                        continue
                    }
                }
            }
            
            // Sắp xếp theo tên app
            userApps.sortedBy { it.appName }
        }
    }
    
    private fun toggleAppLockStatus(appInfo: AppInfo) {
        if (appInfo.isLocked) {
            lockedAppsManager.unlockApp(appInfo.packageName)
            appInfo.isLocked = false
            Toast.makeText(this, "${appInfo.appName} đã được bỏ khóa", Toast.LENGTH_SHORT).show()
        } else {
            lockedAppsManager.lockApp(appInfo.packageName)
            appInfo.isLocked = true
            Toast.makeText(this, "${appInfo.appName} đã được khóa", Toast.LENGTH_SHORT).show()
        }
        
        // Cập nhật adapter
        adapter.notifyDataSetChanged()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}