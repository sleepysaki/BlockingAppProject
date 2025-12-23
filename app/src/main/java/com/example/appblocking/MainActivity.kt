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

        // Khởi tạo AI
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

            // 1. Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // 2. Analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                FaceAnalyzer { face, faceBitmap ->

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

                    // === B. XỬ LÝ LOGIC ===
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

    // Hàm kiểm tra mở khoá
    private fun checkFaceUnlock(currentVector: FloatArray) {
        // Chạy trên luồng IO để không làm đơ giao diện khi chờ mạng
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Gọi API lấy dữ liệu về từ Supabase
                // Cú pháp "eq.$OWNER_UUID" nghĩa là: Lọc user_id BẰNG cái ID của máy này
                val response = RetrofitClient.api.getFace(userIdQuery = "eq.$OWNER_UUID")

                if (response.isSuccessful && response.body() != null) {
                    val faces = response.body()!!

                    // Nếu tìm thấy dữ liệu trên Server
                    if (faces.isNotEmpty()) {
                        val ownerFace = faces[0] // Lấy người đầu tiên tìm thấy

                        // 2. Chuyển đổi chuỗi String từ Server về lại FloatArray để tính toán
                        // Dùng toán tử ?.let để an toàn nếu dữ liệu trên server bị null
                        val vecCenter = ownerFace.embeddingCenter?.split(",")?.map { it.toFloat() }
                            ?.toFloatArray()
                        val vecLeft = ownerFace.embeddingLeft?.split(",")?.map { it.toFloat() }
                            ?.toFloatArray()
                        val vecRight = ownerFace.embeddingRight?.split(",")?.map { it.toFloat() }
                            ?.toFloatArray()

                        // 3. Thuật toán so sánh (Tìm khoảng cách nhỏ nhất trong 3 góc)
                        var minDistance = 10f // Khởi tạo số lớn

                        // So với góc thẳng
                        if (vecCenter != null) {
                            val d = faceRecognizer.calculateDistance(currentVector, vecCenter)
                            if (d < minDistance) minDistance = d
                        }
                        // So với góc trái
                        if (vecLeft != null) {
                            val d = faceRecognizer.calculateDistance(currentVector, vecLeft)
                            if (d < minDistance) minDistance = d
                        }
                        // So với góc phải
                        if (vecRight != null) {
                            val d = faceRecognizer.calculateDistance(currentVector, vecRight)
                            if (d < minDistance) minDistance = d
                        }

                        // 4. Cập nhật giao diện (Phải quay về luồng Main)
                        withContext(Dispatchers.Main) {
                            val threshold = 0.5f
                            val isOwner = minDistance < threshold

                            val distString = String.format("%.2f", minDistance)

                            if (isOwner) {
                                binding.btnRegister.text = "CLOUD MỞ KHOÁ ($distString)"
                                binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_green_dark))
                            } else {
                                binding.btnRegister.text = "CLOUD CHẶN ($distString)"
                                binding.btnRegister.setBackgroundColor(getColor(android.R.color.holo_red_dark))
                            }
                        }
                    } else {
                        // Trường hợp trên Server chưa có dữ liệu ID này
                        withContext(Dispatchers.Main) {
                            binding.btnRegister.text = "Chưa có dữ liệu trên Cloud"
                            binding.btnRegister.setBackgroundColor(getColor(android.R.color.darker_gray))
                        }
                    }
                } else {
                    Log.e("Supabase", "Lỗi Server: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("Supabase", "Lỗi kết nối: ${e.message}")
                withContext(Dispatchers.Main) {
                }
            }
        }
    }

    // === HÀM LƯU DỮ LIỆU ===
    private val OWNER_UUID = "e1488c9f-3316-4545-9730-1e58245c5555"

    private fun saveAllVectors(vectors: List<FloatArray>) {
        Log.e("API_TEST", "Bắt đầu chạy hàm lưu SaveAllVectors...")

        val faceData = FaceModel(
            userId = OWNER_UUID,
            embeddingCenter = vectors[0].joinToString(","),
            embeddingLeft = vectors[1].joinToString(","),
            embeddingRight = vectors[2].joinToString(",")
        )
    }
}