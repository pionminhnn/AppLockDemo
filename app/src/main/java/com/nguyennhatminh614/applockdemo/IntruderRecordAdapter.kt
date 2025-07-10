package com.nguyennhatminh614.applockdemo

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter cho RecyclerView hiển thị danh sách bản ghi kẻ đột nhập
 */
class IntruderRecordAdapter(
    private val context: Context,
    private var records: MutableList<IntruderRecord>,
    private val onDeleteClick: (IntruderRecord) -> Unit
) : RecyclerView.Adapter<IntruderRecordAdapter.ViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivIntruderPhoto)
        val appNameTextView: TextView = itemView.findViewById(R.id.tvAppName)
        val timestampTextView: TextView = itemView.findViewById(R.id.tvTimestamp)
        val deleteButton: ImageView = itemView.findViewById(R.id.ivDelete)
        val noPhotoTextView: TextView = itemView.findViewById(R.id.tvNoPhoto)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_intruder_record, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        
        // Hiển thị tên app
        holder.appNameTextView.text = record.appName
        
        // Hiển thị thời gian
        holder.timestampTextView.text = dateFormat.format(record.getTimestampAsDate())
        
        // Hiển thị ảnh hoặc placeholder
        if (record.imagePath != null && File(record.imagePath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(record.imagePath)
                holder.imageView.setImageBitmap(bitmap)
                holder.imageView.visibility = View.VISIBLE
                holder.noPhotoTextView.visibility = View.GONE
            } catch (e: Exception) {
                // Nếu không thể load ảnh, hiển thị placeholder
                showNoPhotoPlaceholder(holder)
            }
        } else {
            showNoPhotoPlaceholder(holder)
        }
        
        // Xử lý nút xóa
        holder.deleteButton.setOnClickListener {
            onDeleteClick(record)
        }
    }
    
    private fun showNoPhotoPlaceholder(holder: ViewHolder) {
        holder.imageView.visibility = View.GONE
        holder.noPhotoTextView.visibility = View.VISIBLE
        holder.noPhotoTextView.text = "Không có ảnh"
    }
    
    override fun getItemCount(): Int = records.size
    
    /**
     * Cập nhật danh sách bản ghi
     */
    fun updateRecords(newRecords: List<IntruderRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }
    
    /**
     * Xóa một bản ghi khỏi danh sách
     */
    fun removeRecord(record: IntruderRecord) {
        val position = records.indexOf(record)
        if (position != -1) {
            records.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}