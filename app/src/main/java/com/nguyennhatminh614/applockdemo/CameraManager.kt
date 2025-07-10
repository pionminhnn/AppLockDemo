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
        private const val IMAGE_WIDTH = 1080
        private const val IMAGE_HEIGHT = 1920
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
            
            // Tạo ImageReader
            imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 1)
            
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
    
    private fun getFrontCameraId(cameraManager: android.hardware.camera2.CameraManager): String? {
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
                        captureStillPicture()
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
    
    private fun captureStillPicture() {
        try {
            val surface = imageReader?.surface
            if (surface == null) {
                Log.e(TAG, "ImageReader surface is null")
                return
            }
            
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(surface)

            // Cài đặt auto focus và auto exposure
            captureBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureBuilder?.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            
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
                    Log.e(TAG, "Photo capture failed")
                }
            }
            captureSession?.setRepeatingRequest(captureBuilder?.build()!!, null, backgroundHandler)
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