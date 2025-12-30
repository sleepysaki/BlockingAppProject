package com.example.appblocking

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appblocking.databinding.ActivityMainBinding
import com.example.appblocking.network.FaceModel
import com.example.appblocking.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var faceRecognizer: FaceRecognizer

    // === CONSTANTS & CONFIG ===
    private val OWNER_UUID = "e1488c9f-3316-4545-9730-1e58245c5555"
    private val TOO_CLOSE_RATIO = 0.45f // Face > 45% of screen width = Too Close
    private val AUTH_CHECK_INTERVAL = 5000L // Check every 5s for Continuous Auth
    private val MAX_SUSPICIOUS_COUNT = 3 // Lock after 3 fails

    // === STATE VARIABLES ===
    private var isAuthenticated = false
    private var lastAuthCheckTime = 0L
    private var suspiciousCounter = 0
    private var isWarningShown = false // Flag to control "Too Close" UI updates

    // Registration Steps
    enum class RegStep { IDLE, SCAN_CENTER, SCAN_LEFT, SCAN_RIGHT }
    private var currentStep = RegStep.IDLE
    private val collectedVectors = mutableListOf<FloatArray>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera()
            else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        faceRecognizer = FaceRecognizer(this)

        binding.btnRegister.setOnClickListener { startRegistrationProcess() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startRegistrationProcess() {
        collectedVectors.clear()
        currentStep = RegStep.SCAN_CENTER
        isAuthenticated = false
        Toast.makeText(this, "Look straight at the camera", Toast.LENGTH_SHORT).show()
        binding.btnRegister.text = "Step 1: LOOK STRAIGHT"
        binding.btnRegister.setBackgroundColor(Color.DKGRAY)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // 2. Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), FaceAnalyzer { face, faceBitmap, frameWidth, frameHeight ->

                // === A. DRAW BOUNDING BOX ===
                if (face != null) {
                    val boundingBox = face.boundingBox
                    val scaleX = binding.viewFinder.width.toFloat() / frameHeight.toFloat()
                    val scaleY = binding.viewFinder.height.toFloat() / frameWidth.toFloat()

                    val scaledRect = android.graphics.Rect(
                        (boundingBox.left * scaleX).toInt(),
                        (boundingBox.top * scaleY).toInt(),
                        (boundingBox.right * scaleX).toInt(),
                        (boundingBox.bottom * scaleY).toInt()
                    )
                    // Mirroring
                    val mirrorRect = android.graphics.Rect(
                        binding.viewFinder.width - scaledRect.right,
                        scaledRect.top,
                        binding.viewFinder.width - scaledRect.left,
                        scaledRect.bottom
                    )
                    runOnUiThread { binding.overlayView.setFaceRect(mirrorRect) }

                    // === B. FEATURE: SMART DISTANCE (FIXED) ===
                    val faceRatio = face.boundingBox.width().toFloat() / frameWidth.toFloat()

                    if (faceRatio > TOO_CLOSE_RATIO) {
                        // Case: Too Close
                        if (!isWarningShown) {
                            isWarningShown = true
                            runOnUiThread {
                                binding.btnRegister.text = "⚠️ TOO CLOSE! MOVE BACK"
                                binding.btnRegister.setBackgroundColor(Color.parseColor("#FFCC00"))
                            }
                        }
                        return@FaceAnalyzer // Stop processing here
                    } else {
                        // Case: Safe Distance
                        if (isWarningShown) {
                            isWarningShown = false // Reset flag
                            runOnUiThread {
                                // Restore UI based on current state
                                if (isAuthenticated) {
                                    binding.btnRegister.text = "UNLOCKED (Protecting...)"
                                    binding.btnRegister.setBackgroundColor(Color.GREEN)
                                } else {
                                    binding.btnRegister.text = "Authenticating..."
                                    binding.btnRegister.setBackgroundColor(Color.DKGRAY)
                                }
                            }
                        }
                    }
                } else {
                    runOnUiThread { binding.overlayView.setFaceRect(null) }
                }

                // === C. MAIN LOGIC ===
                if (face != null && faceBitmap != null) {
                    val rotY = face.headEulerAngleY
                    val currentVector = faceRecognizer.getFaceEmbedding(faceBitmap)

                    if (currentStep != RegStep.IDLE) {
                        // Registration Mode
                        runOnUiThread { processRegistrationStep(rotY, currentVector) }
                    } else {
                        // Auth Mode
                        if (!isAuthenticated) {
                            checkFaceUnlock(currentVector)
                        } else {
                            // Continuous Auth (Silent Check)
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastAuthCheckTime > AUTH_CHECK_INTERVAL) {
                                lastAuthCheckTime = currentTime
                                performSilentCheck(currentVector)
                            }
                        }
                    }
                }
            })

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // === REGISTRATION LOGIC ===
    private fun processRegistrationStep(rotY: Float, vector: FloatArray) {
        when (currentStep) {
            RegStep.SCAN_CENTER -> {
                if (Math.abs(rotY) < 10) {
                    collectedVectors.add(vector)
                    currentStep = RegStep.SCAN_LEFT
                    binding.btnRegister.text = "Step 2: Turn LEFT >>"
                    binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
                }
            }
            RegStep.SCAN_LEFT -> {
                if (rotY > 20) {
                    collectedVectors.add(vector)
                    currentStep = RegStep.SCAN_RIGHT
                    binding.btnRegister.text = "Step 3: Turn RIGHT <<"
                }
            }
            RegStep.SCAN_RIGHT -> {
                if (rotY < -20) {
                    collectedVectors.add(vector)
                    saveAllVectors(collectedVectors)
                    currentStep = RegStep.IDLE
                    binding.btnRegister.text = "UPLOADING..."
                    binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_blue_bright))
                }
            }
            else -> {}
        }
    }

    // === SUPABASE: SAVE ===
    private fun saveAllVectors(vectors: List<FloatArray>) {
        val faceData = FaceModel(
            userId = OWNER_UUID,
            embeddingCenter = vectors[0].joinToString(","),
            embeddingLeft = vectors[1].joinToString(","),
            embeddingRight = vectors[2].joinToString(",")
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.upsertFace(faceData)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Registration Complete!", Toast.LENGTH_SHORT).show()
                        binding.btnRegister.text = "REGISTERED (Login Ready)"
                    } else {
                        binding.btnRegister.text = "Error: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Save Error: ${e.message}")
            }
        }
    }

    // === SUPABASE: LOGIN CHECK ===
    private fun checkFaceUnlock(currentVector: FloatArray) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getFace("eq.$OWNER_UUID")
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val ownerFace = response.body()!![0]
                    val minDistance = calculateMinDistance(currentVector, ownerFace)

                    withContext(Dispatchers.Main) {
                        if (minDistance < 1.0f) {
                            isAuthenticated = true
                            suspiciousCounter = 0
                            binding.btnRegister.text = "UNLOCKED (Protecting...)"
                            binding.btnRegister.setBackgroundColor(Color.GREEN)
                        } else {
                            binding.btnRegister.text = "Stranger Detected ($minDistance)"
                            binding.btnRegister.setBackgroundColor(Color.RED)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Auth Error: ${e.message}")
            }
        }
    }

    // === CONTINUOUS AUTH CHECK ===
    private fun performSilentCheck(currentVector: FloatArray) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getFace("eq.$OWNER_UUID")
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val ownerFace = response.body()!![0]
                    val minDistance = calculateMinDistance(currentVector, ownerFace)

                    withContext(Dispatchers.Main) {
                        if (minDistance < 1.0f) {
                            suspiciousCounter = 0 // Verified owner
                        } else {
                            suspiciousCounter++
                            Log.w("Auth", "Suspicious count: $suspiciousCounter")
                            if (suspiciousCounter >= MAX_SUSPICIOUS_COUNT) {
                                isAuthenticated = false // Lock App
                                binding.btnRegister.text = "LOCKED: Stranger Detected!"
                                binding.btnRegister.setBackgroundColor(Color.RED)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Silent Error: ${e.message}")
            }
        }
    }

    // Helper: Min Distance Logic
    private fun calculateMinDistance(current: FloatArray, owner: FaceModel): Float {
        val vecCenter = owner.embeddingCenter?.split(",")?.map { it.toFloat() }?.toFloatArray()
        val vecLeft = owner.embeddingLeft?.split(",")?.map { it.toFloat() }?.toFloatArray()
        val vecRight = owner.embeddingRight?.split(",")?.map { it.toFloat() }?.toFloatArray()

        var minD = 10f
        if (vecCenter != null) minD = minOf(minD, faceRecognizer.calculateDistance(current, vecCenter))
        if (vecLeft != null) minD = minOf(minD, faceRecognizer.calculateDistance(current, vecLeft))
        if (vecRight != null) minD = minOf(minD, faceRecognizer.calculateDistance(current, vecRight))
        return minD
    }
}