package com.nguyennhatminh614.applockdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>() {
    
    private var apps: List<AppInfo> = emptyList()
    
    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }
    
    override fun getItemCount(): Int = apps.size
    
    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val appName: TextView = itemView.findViewById(R.id.tvAppName)
        private val packageName: TextView = itemView.findViewById(R.id.tvPackageName)
        private val checkBox: CheckBox = itemView.findViewById(R.id.cbAppLocked)
        
        fun bind(appInfo: AppInfo) {
            appIcon.setImageDrawable(appInfo.icon)
            appName.text = appInfo.appName
            packageName.text = appInfo.packageName
            checkBox.isChecked = appInfo.isLocked
            
            // Set click listeners
            itemView.setOnClickListener {
                onAppClick(appInfo)
            }
            
            checkBox.setOnClickListener {
                onAppClick(appInfo)
            }
        }
    }
}