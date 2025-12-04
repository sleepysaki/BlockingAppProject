package com.example.appblocking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import com.example.appblocking.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var faceRecognizer: FaceRecognizer

    private var isRegistering = false

    // permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Need permission to use app", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        faceRecognizer = FaceRecognizer(this)
        // Check permission then start
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        binding.btnRegister.setOnClickListener {
            isRegistering = true
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 1. Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // 2. Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), FaceAnalyzer { boundingBox, faceBitmap ->


                val scaleX = binding.viewFinder.width.toFloat() / 480f
                val scaleY = binding.viewFinder.height.toFloat() / 640f

                val scaledRect = android.graphics.Rect(
                    (boundingBox.left * scaleX).toInt(),
                    (boundingBox.top * scaleY).toInt(),
                    (boundingBox.right * scaleX).toInt(),
                    (boundingBox.bottom * scaleY).toInt()
                )

                val mirrorRect = android.graphics.Rect(
                    binding.viewFinder.width - scaledRect.right,
                    scaledRect.top,
                    binding.viewFinder.width - scaledRect.left,
                    scaledRect.bottom
                )

                runOnUiThread {
                    binding.overlayView.setFaceRect(mirrorRect)
                }

                // AI
                if (faceBitmap != null) {
                    val currentVector = faceRecognizer.getFaceEmbedding(faceBitmap)

                    if (isRegistering) {
                        saveFaceVector(currentVector)
                        isRegistering = false

                        runOnUiThread {
                            binding.btnRegister.text = "Face detected!"
                        }
                    }

                    val ownerVector = getSavedVector()

                    if (ownerVector != null) {
                        val distance = faceRecognizer.calculateDistance(currentVector, ownerVector)
                        val distanceShort = String.format("%.2f", distance)
                        val isOwner = distance < 0.5f

                        runOnUiThread {
                            if (isOwner) {
                                binding.btnRegister.text = "Open ( MASTER DETECTED!!) ($distanceShort)"
                                binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_green_dark))
                            } else {
                                binding.btnRegister.text = "CHẶN ( STRANGER ALERT!!) ($distanceShort)"
                                binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_red_dark))
                            }
                        }
                    }
                }
            })
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "can't open camera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    // Hàm lưu vector vào bộ nhớ
    private fun saveFaceVector(vector: FloatArray) {
        val sharedPref = getSharedPreferences("FaceDB", Context.MODE_PRIVATE)
        val vectorString = vector.joinToString(",")
        with(sharedPref.edit()) {
            putString("owner_vector", vectorString)
            apply()
        }
        Toast.makeText(this, "lưu thành công!", Toast.LENGTH_SHORT).show()
    }

    // Hàm lấy vector từ bộ nhớ ra
    private fun getSavedVector(): FloatArray? {
        val sharedPref = getSharedPreferences("FaceDB", Context.MODE_PRIVATE)
        val string = sharedPref.getString("owner_vector", null) ?: return null
        return string.split(",").map { it.toFloat() }.toFloatArray()
    }
}

