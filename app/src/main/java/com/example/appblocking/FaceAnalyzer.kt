package com.example.appblocking

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(private val onFaceDetected: (com.google.mlkit.vision.face.Face?, Bitmap?, frameWidth: Int, frameHeight: Int) -> Unit
) : ImageAnalysis.Analyzer {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()

    private val detector = FaceDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val frameWidth = image.width
            val frameHeight = image.height

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        val boundingBox = face.boundingBox
                        val fullBitmap = imageProxy.toBitmap()
                        val rotatedBitmap = rotateBitmap(fullBitmap, imageProxy.imageInfo.rotationDegrees)
                        val cropRect = getValidRect(boundingBox, rotatedBitmap.width, rotatedBitmap.height)
                        val faceBitmap = Bitmap.createBitmap(
                            rotatedBitmap,
                            cropRect.left,
                            cropRect.top,
                            cropRect.width(),
                            cropRect.height()
                        )

                        onFaceDetected(face, faceBitmap, frameWidth, frameHeight)
                    } else {
                        onFaceDetected(null, null, frameWidth, frameHeight)
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }


    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getValidRect(rect: Rect, width: Int, height: Int): Rect {
        val left = rect.left.coerceAtLeast(0)
        val top = rect.top.coerceAtLeast(0)
        val right = rect.right.coerceAtMost(width)
        val bottom = rect.bottom.coerceAtMost(height)
        if (left >= right || top >= bottom) return Rect(0,0,1,1)

        return Rect(left, top, right, bottom)
    }
}