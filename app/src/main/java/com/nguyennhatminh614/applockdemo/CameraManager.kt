package com.nguyennhatminh614.applockdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Manager để xử lý việc chụp ảnh bằng camera trước
 */
class CameraManager(private val context: Context) {
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    
    companion object {
        private const val TAG = "CameraManager"
        private const val IMAGE_WIDTH = 640
        private const val IMAGE_HEIGHT = 480
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
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking front camera", e)
            false
        }
    }
    
    /**
     * Chụp ảnh bằng camera trước
     */
    fun captureIntruderPhoto(callback: (String?) -> Unit) {
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
        
        startBackgroundThread()
        
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val frontCameraId = getFrontCameraId(cameraManager)
            
            if (frontCameraId == null) {
                Log.e(TAG, "Front camera ID not found")
                callback(null)
                return
            }
            
            // Lấy kích thước ảnh được hỗ trợ
            val supportedSize = getSupportedImageSize(cameraManager, frontCameraId)
            
            // Tạo ImageReader với kích thước được hỗ trợ
            imageReader = ImageReader.newInstance(
                supportedSize.width, 
                supportedSize.height, 
                ImageFormat.JPEG, 
                1
            )
            
            Log.d(TAG, "Using image size: ${supportedSize.width}x${supportedSize.height}")
            
            val readerListener = ImageReader.OnImageAvailableListener { reader ->
                val image = reader.acquireLatestImage()
                val filePath = saveImageToFile(image)
                image.close()

                callback(filePath)

                // Cleanup
                closeCamera()
                stopBackgroundThread()
            }
            
            imageReader?.setOnImageAvailableListener(readerListener, backgroundHandler)
            
            // Mở camera
            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    Log.e(TAG, "Camera error: $error")
                    callback(null)
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing photo", e)
            callback(null)
        }
    }
    
    private fun getFrontCameraId(cameraManager: CameraManager): String? {
        return try {
            val cameraIds = cameraManager.cameraIdList
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting front camera ID", e)
            null
        }
    }
    
    private fun getSupportedImageSize(cameraManager: CameraManager, cameraId: String): Size {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)
            
            if (sizes != null && sizes.isNotEmpty()) {
                // Tìm kích thước phù hợp (không quá lớn để tránh lỗi memory)
                for (size in sizes) {
                    if (size.width <= 1920 && size.height <= 1080) {
                        Log.d(TAG, "Selected size: ${size.width}x${size.height}")
                        return size
                    }
                }
                // Nếu không tìm thấy kích thước phù hợp, dùng kích thước nhỏ nhất
                val smallestSize = sizes.minByOrNull { it.width * it.height }
                Log.d(TAG, "Using smallest size: ${smallestSize?.width}x${smallestSize?.height}")
                return smallestSize ?: Size(IMAGE_WIDTH, IMAGE_HEIGHT)
            }
            
            // Fallback về kích thước mặc định
            Size(IMAGE_WIDTH, IMAGE_HEIGHT)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting supported image size", e)
            Size(IMAGE_WIDTH, IMAGE_HEIGHT)
        }
    }
    
    private fun createCaptureSession() {
        try {
            val surface = imageReader?.surface
            if (surface == null) {
                Log.e(TAG, "ImageReader surface is null")
                return
            }
            
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startPreviewAndCapture()
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session", e)
        }
    }
    
    private fun startPreviewAndCapture() {
        try {
            val surface = imageReader?.surface
            if (surface == null) {
                Log.e(TAG, "ImageReader surface is null")
                return
            }
            
            // Tạo preview request để camera ổn định trước
            val previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewBuilder?.addTarget(surface)
            
            // Cài đặt auto focus và auto exposure
            previewBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            previewBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            previewBuilder?.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            previewBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            
            // Bắt đầu preview để camera ổn định
            captureSession?.setRepeatingRequest(previewBuilder?.build()!!, null, backgroundHandler)
            
            // Đợi một chút để camera ổn định rồi chụp ảnh
            backgroundHandler?.postDelayed({
                captureStillPicture()
            }, 1500) // Đợi 1.5 giây
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting preview", e)
        }
    }
    
    private fun captureStillPicture() {
        try {
            val surface = imageReader?.surface
            if (surface == null) {
                Log.e(TAG, "ImageReader surface is null")
                return
            }
            
            // Dừng preview trước khi chụp
            captureSession?.stopRepeating()
            
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(surface)

            // Cài đặt auto focus và auto exposure
            captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureBuilder?.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            captureBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            
            // Cài đặt chất lượng JPEG cao nhất
            captureBuilder?.set(CaptureRequest.JPEG_QUALITY, 100.toByte())
            
            // Cài đặt flash mode (tắt flash cho camera trước)
            captureBuilder?.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            
            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, "Photo captured successfully")
                }
                
                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    Log.e(TAG, "Photo capture failed: ${failure.reason}")
                }
            }
            
            captureSession?.capture(captureBuilder?.build()!!, captureCallback, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing still picture", e)
        }
    }
    
    private fun saveImageToFile(image: Image): String? {
        return try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            // Tạo thư mục lưu ảnh
            val intruderDir = File(context.filesDir, "intruder_photos")
            if (!intruderDir.exists()) {
                intruderDir.mkdirs()
            }
            
            // Tạo tên file với timestamp
            val fileName = "intruder_${System.currentTimeMillis()}.jpg"
            val file = File(intruderDir, fileName)
            
            val output = FileOutputStream(file)
            output.write(bytes)
            output.close()
            
            Log.d(TAG, "Image saved to: ${file.absolutePath}")
            file.absolutePath
            
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image", e)
            null
        }
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
    
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        
        cameraDevice?.close()
        cameraDevice = null
        
        imageReader?.close()
        imageReader = null
    }
}