package com.example.appblocking.network
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {
    // 1. Lấy thông tin khuôn mặt
    @GET("face_embeddings")
    suspend fun getFace(
        @Query("user_id") userIdQuery: String,
        @Query("select") select: String = "*"
    ): Response<List<FaceModel>>

    // 2. Lưu/Cập nhật khuôn mặt
    @Headers(
        "Content-Type: application/json",
        "Prefer: resolution=merge-duplicates"
    )
    @POST("face_embeddings")
    suspend fun upsertFace(@Body face: FaceModel): Response<Void>
}