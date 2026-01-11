package com.example.setsolver

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.setsolver.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    
    private lateinit var diagnosticLogger: DiagnosticLogger
    private lateinit var cardDetector: CardDetector
    private lateinit var setFinder: SetFinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed")
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "OpenCV initialized successfully")
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Create diagnostic logger wrapper
        diagnosticLogger = object : DiagnosticLogger {
            override fun log(message: String) {
                runOnUiThread {
                    binding.diagnosticsView.log(message)
                }
            }
            
            override fun logSection(title: String) {
                runOnUiThread {
                    binding.diagnosticsView.logSection(title)
                }
            }
            
            override fun clear() {
                runOnUiThread {
                    binding.diagnosticsView.clear()
                }
            }
        }
        
        // Initialize detectors with diagnostic logger
        cardDetector = CardDetector(diagnosticLogger)
        setFinder = SetFinder(diagnosticLogger)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up capture button
        binding.captureButton.setOnClickListener {
            captureAndProcess()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initial diagnostic message
        diagnosticLogger.log("App started - ready to capture")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndProcess() {
        val imageCapture = imageCapture ?: return

        binding.statusText.text = "Processing..."
        binding.captureButton.isEnabled = false
        
        // Clear previous diagnostics
        diagnosticLogger.clear()
        diagnosticLogger.logSection("Processing Started")

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImage(image)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    runOnUiThread {
                        binding.statusText.text = "Capture failed"
                        binding.captureButton.isEnabled = true
                        diagnosticLogger.log("ERROR: Photo capture failed - ${exception.message}")
                    }
                }
            }
        )
    }

    private fun processImage(image: ImageProxy) {
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(image)
            
            // Detect cards in the image
            val cards = cardDetector.detectCards(bitmap)
            
            // Find sets among the detected cards
            val sets = setFinder.findAllSets(cards)
            
            // Update UI on main thread
            runOnUiThread {
                if (cards.isEmpty()) {
                    binding.statusText.text = "No cards detected"
                    binding.overlayView.clear()
                    diagnosticLogger.logSection("Result")
                    diagnosticLogger.log("No cards were detected in the image")
                } else if (sets.isEmpty()) {
                    binding.statusText.text = getString(R.string.no_sets_found)
                    binding.overlayView.setCards(cards)
                    diagnosticLogger.logSection("Result")
                    diagnosticLogger.log("Cards detected but no valid sets found")
                } else {
                    binding.statusText.text = getString(R.string.sets_found, sets.size)
                    binding.overlayView.setSets(sets)
                    diagnosticLogger.logSection("Result")
                    diagnosticLogger.log("SUCCESS: Found ${sets.size} valid set(s)")
                }
                binding.captureButton.isEnabled = true
                
                Log.d(TAG, "Found ${cards.size} cards and ${sets.size} sets")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            runOnUiThread {
                binding.statusText.text = "Error processing image"
                binding.captureButton.isEnabled = true
                diagnosticLogger.log("FATAL ERROR: ${e.message}")
                diagnosticLogger.log("Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        // Create bitmap from YUV format
        return ImageUtils.yuv420ToBitmap(
            bytes,
            image.width,
            image.height
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
