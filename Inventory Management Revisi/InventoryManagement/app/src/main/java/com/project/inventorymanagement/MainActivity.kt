package com.project.inventorymanagement

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var buttonCapture: Button
    private lateinit var croppingOverlay: View
    private var imageCapture: ImageCapture? = null
    private val filename = "captured_image.jpg"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            // Permission denied, show message to the user
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        buttonCapture = findViewById(R.id.button_capture)
        croppingOverlay = findViewById(R.id.croppingOverlay)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        buttonCapture.setOnClickListener { captureScreenshotOfOverlay() }

    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder()
                .build()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureScreenshotOfOverlay() {
        val previewBitmap = previewView.bitmap ?: return

        // Convert dp to px
        val overlayHeightPx = dpToPx(80) // 80dp in px

        val overlayLocation = IntArray(2)
        croppingOverlay.getLocationOnScreen(overlayLocation)

        val overlayX = overlayLocation[0]
        val overlayY = overlayLocation[1]
        val overlayWidth = croppingOverlay.width
        val overlayHeight = overlayHeightPx // Fixed height in px

        Log.d(TAG, "Overlay Dimensions - X: $overlayX, Y: $overlayY, Width: $overlayWidth, Height: $overlayHeight")

        val previewViewLocation = IntArray(2)
        previewView.getLocationOnScreen(previewViewLocation)

        val previewViewX = previewViewLocation[0]
        val previewViewY = previewViewLocation[1]

        // Calculate crop start and end positions
        val cropStartX = (overlayX - previewViewX).coerceIn(0, previewBitmap.width)
        val cropStartY = (overlayY - previewViewY).coerceIn(0, previewBitmap.height)
        val cropEndX = (cropStartX + overlayWidth).coerceAtMost(previewBitmap.width)
        val cropEndY = (cropStartY + overlayHeight).coerceAtMost(previewBitmap.height)

        // Ensure crop dimensions are valid
        val cropWidth = (cropEndX - cropStartX).coerceAtLeast(1)
        val cropHeight = (cropEndY - cropStartY).coerceAtLeast(1)

        Log.d(TAG, "Crop Area - StartX: $cropStartX, StartY: $cropStartY, EndX: $cropEndX, EndY: $cropEndY")
        Log.d(TAG, "Crop Dimensions - Width: $cropWidth, Height: $cropHeight")
        Log.d(TAG, "previewBitmap - width: ${previewBitmap.width}, height: ${previewBitmap.height}")

        // Ensure the cropping area is within the bitmap bounds
        if (cropStartX < 0 || cropStartY < 0 || cropEndX > previewBitmap.width || cropEndY > previewBitmap.height) {
            Log.e(TAG, "Crop area is out of bounds")
            return
        }

        try {
            // Create a new bitmap with the desired dimensions
            val croppedBitmap = Bitmap.createBitmap(previewBitmap, cropStartX, cropStartY, cropWidth, cropHeight)

            val croppedFile = File(externalMediaDirs.firstOrNull(), "screenshot_overlay.jpg")
            val outputStream = FileOutputStream(croppedFile)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()

            val intent = Intent(this, ReviewActivity::class.java).apply {
                putExtra("imagePath", croppedFile.absolutePath)
            }
            startActivity(intent)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Cropping failed: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
        }
    }


    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


//
//    private fun capturePhoto() {
//        val imageCapture = imageCapture ?: return
//
//        val photoFile = File(externalMediaDirs.firstOrNull(), filename)
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    saveImage(photoFile.absolutePath)
//                }
//            })
//    }
//    private fun saveImage(filePath: String) {
//        val bitmap = BitmapFactory.decodeFile(filePath)
//        val croppedFile = File(externalMediaDirs.firstOrNull(), "cropped_image.jpg")
//        val outputStream = FileOutputStream(croppedFile)
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//        outputStream.close()
//
//        val intent = Intent(this, ReviewActivity::class.java).apply {
//            putExtra("imagePath", croppedFile.absolutePath)
//        }
//        startActivity(intent)
//    }

    companion object {
        private const val TAG = "CameraXApp"
    }
}