package com.exemple.blockingapps.model.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.exemple.blockingapps.model.BlockRule

// 1. Định nghĩa các hành động gọi API
interface ApiService {
    @GET("/rules") // Đường dẫn này nối đuôi vào BASE_URL -> http://ip:8080/rules
    suspend fun getBlockRules(): List<BlockRule>
}

// 2. Tạo cục kết nối (Singleton)
object RetrofitClient {


    private const val BASE_URL = "http://10.0.2.2:8080/"
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}