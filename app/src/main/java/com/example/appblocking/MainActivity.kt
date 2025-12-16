package com.example.appblocking

import android.Manifest
import android.content.Context
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
import com.example.appblocking.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var faceRecognizer: FaceRecognizer

    // === CÁC BIẾN QUẢN LÝ QUY TRÌNH ĐĂNG KÝ ĐA GÓC ===
    enum class RegStep {
        IDLE,           // Bình thường (Đang mở khoá)
        SCAN_CENTER,    // Bước 1: Quét nhìn thẳng
        SCAN_LEFT,      // Bước 2: Quét quay trái
        SCAN_RIGHT,     // Bước 3: Quét quay phải
        DONE            // Đã xong
    }

    private var currentStep = RegStep.IDLE
    // Danh sách tạm để lưu 3 vector (Thẳng, Trái, Phải) trước khi ghi vào bộ nhớ
    private val collectedVectors = mutableListOf<FloatArray>()

    // Xin quyền Camera
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera()
            else Toast.makeText(this, "Cần cấp quyền Camera để sử dụng!", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo bộ não AI
        faceRecognizer = FaceRecognizer(this)

        // Xử lý nút bấm Đăng ký
        binding.btnRegister.setOnClickListener {
            startRegistrationProcess()
        }

        // Kiểm tra quyền và mở camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Hàm bắt đầu quy trình đăng ký
    private fun startRegistrationProcess() {
        collectedVectors.clear()
        currentStep = RegStep.SCAN_CENTER
        Toast.makeText(this, "Bắt đầu! Hãy giữ yên khuôn mặt nhìn thẳng", Toast.LENGTH_SHORT).show()

        binding.btnRegister.text = "Đang chờ: NHÌN THẲNG..."
        binding.btnRegister.setBackgroundColor(Color.DKGRAY)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1. Preview (Hiển thị hình ảnh)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // 2. Analyzer (Phân tích hình ảnh)
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), FaceAnalyzer { face, faceBitmap ->

                // === A. PHẦN VẼ KHUNG (UI) ===
                if (face != null) {
                    val boundingBox = face.boundingBox
                    val scaleX = binding.viewFinder.width.toFloat() / 480f
                    val scaleY = binding.viewFinder.height.toFloat() / 640f

                    val scaledRect = android.graphics.Rect(
                        (boundingBox.left * scaleX).toInt(),
                        (boundingBox.top * scaleY).toInt(),
                        (boundingBox.right * scaleX).toInt(),
                        (boundingBox.bottom * scaleY).toInt()
                    )

                    // Lật ngược khung vì camera trước bị ngược gương
                    val mirrorRect = android.graphics.Rect(
                        binding.viewFinder.width - scaledRect.right,
                        scaledRect.top,
                        binding.viewFinder.width - scaledRect.left,
                        scaledRect.bottom
                    )

                    runOnUiThread { binding.overlayView.setFaceRect(mirrorRect) }
                } else {
                    runOnUiThread { binding.overlayView.setFaceRect(null) }
                }

                // === B. PHẦN XỬ LÝ LOGIC (AI) ===
                if (face != null && faceBitmap != null) {
                    // Lấy góc quay đầu (Yaw): Âm là quay phải, Dương là quay trái (tuỳ máy)
                    val rotY = face.headEulerAngleY

                    // Lấy vector đặc trưng
                    val currentVector = faceRecognizer.getFaceEmbedding(faceBitmap)

                    // Nếu đang trong quá trình đăng ký -> Chạy hàm đăng ký
                    if (currentStep != RegStep.IDLE && currentStep != RegStep.DONE) {
                        runOnUiThread {
                            processRegistrationStep(rotY, currentVector)
                        }
                    }
                    // Nếu đang rảnh (không đăng ký) -> Chạy hàm kiểm tra mở khoá
                    else if (currentStep == RegStep.IDLE) {
                        checkFaceUnlock(currentVector)
                    }
                }
            })

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Không mở được camera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // === LOGIC XỬ LÝ TỪNG BƯỚC ĐĂNG KÝ ===
    private fun processRegistrationStep(rotY: Float, vector: FloatArray) {
        when (currentStep) {
            RegStep.SCAN_CENTER -> {
                // Yêu cầu: Góc nghiêng nhỏ (gần 0)
                if (Math.abs(rotY) < 10) {
                    collectedVectors.add(vector) // Lưu vector 1
                    currentStep = RegStep.SCAN_LEFT

                    binding.btnRegister.text = "Tốt! Hãy quay mặt sang TRÁI >>"
                    binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
                }
            }
            RegStep.SCAN_LEFT -> {
                // Yêu cầu: Quay trái khoảng > 20 độ (Số dương)
                if (rotY > 20) {
                    collectedVectors.add(vector) // Lưu vector 2
                    currentStep = RegStep.SCAN_RIGHT

                    binding.btnRegister.text = "Tuyệt! Hãy quay mặt sang PHẢI <<"
                    binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
                }
            }
            RegStep.SCAN_RIGHT -> {
                // Yêu cầu: Quay phải khoảng < -20 độ (Số âm)
                if (rotY < -20) {
                    collectedVectors.add(vector) // Lưu vector 3

                    // Xong cả 3 bước -> Lưu xuống máy
                    saveAllVectors(collectedVectors)

                    currentStep = RegStep.IDLE
                    binding.btnRegister.text = "ĐĂNG KÝ THÀNH CÔNG!"
                    binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_blue_bright))

                    // Reset lại nút sau 2 giây
                    binding.btnRegister.postDelayed({
                        binding.btnRegister.text = "Lưu lại khuôn mặt (Reset)"
                        binding.btnRegister.setBackgroundColor(getColor(com.google.android.material.R.color.design_default_color_primary))
                    }, 2000)
                }
            }
            else -> {}
        }
    }

    // === LOGIC KIỂM TRA MỞ KHOÁ (SO SÁNH VỚI 3 GÓC) ===
    private fun checkFaceUnlock(currentVector: FloatArray) {
        val sharedPref = getSharedPreferences("FaceDB", Context.MODE_PRIVATE)

        // Lấy cả 3 vector đã lưu ra
        val keys = listOf("vec_center", "vec_left", "vec_right")
        var minDistance = 10.0f // Khởi tạo số lớn nhất

        var hasData = false

        for (key in keys) {
            val savedStr = sharedPref.getString(key, null)
            if (savedStr != null) {
                hasData = true
                val savedVector = savedStr.split(",").map { it.toFloat() }.toFloatArray()

                // Tính khoảng cách
                val distance = faceRecognizer.calculateDistance(currentVector, savedVector)

                // Tìm khoảng cách nhỏ nhất trong 3 góc
                if (!distance.isNaN() && distance < minDistance) {
                    minDistance = distance
                }
            }
        }

        if (!hasData) return

        // === NGƯỠNG QUYẾT ĐỊNH (THRESHOLD) ===
        val threshold = 0.3f
        val isOwner = minDistance < threshold

        val distDisplay = String.format("%.2f", minDistance)

        runOnUiThread {
            if (isOwner) {
                binding.btnRegister.text = "MỞ KHOÁ ($distDisplay)"
                binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            } else {
                binding.btnRegister.text = "CHẶN ($distDisplay)"
                binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    // === HÀM LƯU DỮ LIỆU ===
    private fun saveAllVectors(vectors: List<FloatArray>) {
        if (vectors.size < 3) return

        val sharedPref = getSharedPreferences("FaceDB", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putString("vec_center", vectors[0].joinToString(","))
        editor.putString("vec_left", vectors[1].joinToString(","))
        editor.putString("vec_right", vectors[2].joinToString(","))

        editor.apply()
        Toast.makeText(this, "Đã lưu dữ liệu khuôn mặt đa góc!", Toast.LENGTH_LONG).show()
    }
}