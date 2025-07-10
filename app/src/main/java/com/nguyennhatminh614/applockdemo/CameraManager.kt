package com.nguyennhatminh614.applockdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manager để xử lý việc chụp ảnh bằng camera trước sử dụng CameraX
 */
class CameraManager(private val context: Context) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    companion object {
        private const val TAG = "CameraManager"
    }
    
    /**
     * Kiểm tra quyền camera
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Kiểm tra xem thiết bị có camera trước không
     */
    fun hasFrontCamera(): Boolean {
        return try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking front camera", e)
            false
        }
    }
    
    /**
     * Chụp ảnh bằng camera trước sử dụng CameraX
     */
    fun captureIntruderPhoto(lifecycleOwner: LifecycleOwner, callback: (String?) -> Unit) {
        if (!hasCameraPermission()) {
            Log.e(TAG, "Camera permission not granted")
            callback(null)
            return
        }
        
        if (!hasFrontCamera()) {
            Log.e(TAG, "Front camera not available")
            callback(null)
            return
        }
        
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    startCameraAndCapture(lifecycleOwner, callback)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting camera provider", e)
                    callback(null)
                }
            }, ContextCompat.getMainExecutor(context))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing photo", e)
            callback(null)
        }
    }
    
    private fun startCameraAndCapture(lifecycleOwner: LifecycleOwner, callback: (String?) -> Unit) {
        try {
            // Unbind tất cả use cases trước
            cameraProvider?.unbindAll()
            
            // Chọn camera trước
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            // Cấu hình ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(80)
                .build()
            
            // Bind camera với lifecycle (sử dụng context như LifecycleOwner nếu có thể)
            if (context is LifecycleOwner) {
                cameraProvider?.bindToLifecycle(
                    context,
                    cameraSelector,
                    imageCapture
                )
            } else {
                // Nếu context không phải LifecycleOwner, tạo một lifecycle đơn giản
                Log.w(TAG, "Context is not LifecycleOwner, using alternative approach")
                bindCameraWithoutLifecycle(lifecycleOwner, cameraSelector, callback)
                return
            }
            
            // Đợi một chút để camera ổn định rồi chụp ảnh
            cameraExecutor.execute {
                Thread.sleep(1500) // Đợi 1.5 giây
                capturePhoto(callback)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            callback(null)
        }
    }
    
    private fun bindCameraWithoutLifecycle(lifecycleOwner: LifecycleOwner, cameraSelector: CameraSelector, callback: (String?) -> Unit) {
        try {
            // Sử dụng cách tiếp cận đơn giản hơn cho trường hợp không có LifecycleOwner
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageCapture
            )
            
            // Đợi một chút để camera ổn định rồi chụp ảnh
            cameraExecutor.execute {
                Thread.sleep(1500) // Đợi 1.5 giây
                capturePhoto(callback)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera without lifecycle", e)
            callback(null)
        }
    }
    
    private fun capturePhoto(callback: (String?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture is null")
            callback(null)
            return
        }
        
        // Tạo thư mục lưu ảnh
        val intruderDir = File(context.filesDir, "intruder_photos")
        if (!intruderDir.exists()) {
            intruderDir.mkdirs()
        }
        
        // Tạo tên file với timestamp
        val fileName = "intruder_${System.currentTimeMillis()}.jpg"
        val outputFile = File(intruderDir, fileName)
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo captured successfully: ${outputFile.absolutePath}")
                    
                    // Xử lý xoay ảnh nếu cần
                    val processedPath = processAndRotateImage(outputFile.absolutePath)
                    
                    // Cleanup camera
                    cleanup()
                    
                    callback(processedPath)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    cleanup()
                    callback(null)
                }
            }
        )
    }

    fun rotateBitmapVertical(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(-90f) // Xoay 90 độ theo chiều kim đồng hồ

        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    private fun processAndRotateImage(imagePath: String): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image from path: $imagePath")
                return imagePath
            }

            val finalBitmap = if (bitmap.width > bitmap.height) {
                rotateBitmapVertical(bitmap)
            } else bitmap

            val output = FileOutputStream(imagePath)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.close()

            bitmap.recycle()
            finalBitmap.recycle()
            
            Log.d(TAG, "Image processed: $imagePath")
            imagePath
            
        } catch (e: IOException) {
            Log.e(TAG, "Error processing image", e)
            imagePath
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            imagePath
        }
    }

    /**
     * Cleanup camera resources
     */
    private fun cleanup() {
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
            imageCapture = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Shutdown executor khi không cần thiết nữa
     */
    fun shutdown() {
        cleanup()
        cameraExecutor.shutdown()
    }
}