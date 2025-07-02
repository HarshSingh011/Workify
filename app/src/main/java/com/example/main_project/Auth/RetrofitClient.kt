package com.example.main_project.Auth

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://workify-springboot.onrender.com/"
    private const val TAG = "API_URL_LOG"

    // Interceptor to log the request URL and method, including failures
    private val urlLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        Log.d(TAG, "🌐 REQUEST: $method $url")
        try {
            chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED: $method $url\nReason: ${e.localizedMessage}")
            throw e
        }
    }

    private val detailLogger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(urlLoggingInterceptor)
        .addInterceptor(detailLogger)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}