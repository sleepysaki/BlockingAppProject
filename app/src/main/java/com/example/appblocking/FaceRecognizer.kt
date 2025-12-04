package com.example.appblocking

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer

class FaceRecognizer(context: Context) {

    private var interpreter: Interpreter? = null
    private val INPUT_SIZE = 160
    private val OUTPUT_SIZE = 128

    init {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "facenet.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFaceEmbedding(bitmap: Bitmap): FloatArray {
        if (interpreter == null) return FloatArray(OUTPUT_SIZE)
        val rgbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        var tensorImage = TensorImage.fromBitmap(rgbBitmap)
        tensorImage = imageProcessor.process(tensorImage)


        val outputBuffer = ByteBuffer.allocateDirect(OUTPUT_SIZE * 4)
        outputBuffer.order(java.nio.ByteOrder.nativeOrder()) // Quan trọng: Đảm bảo thứ tự byte đúng
        interpreter?.run(tensorImage.buffer, outputBuffer)


        outputBuffer.rewind()
        val rawEmbeddings = FloatArray(OUTPUT_SIZE)
        outputBuffer.asFloatBuffer().get(rawEmbeddings)
        return l2Normalize(rawEmbeddings)
    }

    fun calculateDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        var sum = 0.0
        for (i in 0 until OUTPUT_SIZE) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }
        return Math.sqrt(sum).toFloat()
    }

    private fun l2Normalize(embeddings: FloatArray): FloatArray {
        var sum = 0.0
        for (value in embeddings) {
            sum += value * value
        }
        val norm = Math.sqrt(sum).toFloat()

        // Tránh chia cho 0
        if (norm == 0f) return embeddings

        return embeddings.map { it / norm }.toFloatArray()
    }
}