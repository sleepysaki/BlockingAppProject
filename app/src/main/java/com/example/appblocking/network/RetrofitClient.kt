package com.example.appblocking.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val SUPABASE_URL = "https://wtyalqkxbtqwirxyhwus.supabase.co/rest/v1/"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind0eWFscWt4YnRxd2lyeHlod3VzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM5MDEwNTQsImV4cCI6MjA3OTQ3NzA1NH0.0LrXGoOQuMQSjkXyqYEVp3s0JQv-SM2QBXAUNSxULhY"

    val api: SupabaseApi by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}