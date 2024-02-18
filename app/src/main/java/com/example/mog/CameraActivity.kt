package com.example.mog
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.File
import android.widget.Button
import androidx.camera.view.PreviewView
import android.util.Log

class CameraActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        Log.d("CameraActivity", "onCreate called")

        // Initialize ExecutorService
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera permissions
        if (!allPermissionsGranted()) {
            Log.d("CameraActivity", "Requesting permissions")
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            Log.d("CameraActivity", "Starting camera")
            startCamera()
        }

        // Set up the listener for the take photo button
        findViewById<Button>(R.id.camera_capture_button).setOnClickListener { takePhoto() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            Log.d("CameraActivity", "CameraProvider is available")
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Handle exception
                Log.e("CameraActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        Log.d("CameraActivity", "Taking photo")
        val imageCapture = imageCapture ?: return

        // Assuming a method to create a file for storing the image
        val photoFile = createPhotoFile()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Handle the saved photo, e.g., display it in an ImageView or save to gallery
                }

                override fun onError(exc: ImageCaptureException) {
                    // Handle any errors
                }
            })
    }

    // Placeholder for creating a photo file


    private fun createPhotoFile(): File {
        // Use external media if it is available, our app's file directory otherwise
        val outputDirectory = getOutputDirectory()
        val photoFile = File(outputDirectory, "JPEG_${System.currentTimeMillis()}.jpg")
        return photoFile
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d("CameraActivity", "Permissions granted by the user.")
                startCamera()
            } else {
                Log.d("CameraActivity", "Permissions not granted by the user.")
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
