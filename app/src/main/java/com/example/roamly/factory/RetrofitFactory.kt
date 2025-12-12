package com.example.roamly.factory

import android.R.attr.level
import com.example.roamly.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {
    //IP при подключении через мобильную точку доступа
    const val BASE_URL = "http://10.39.189.228:8080"
    //const val BASE_URL = "https://x8tsh9tc-8080.euw.devtunnels.ms"


    // const val BASE_URL = "http://192.168.100.174:8080"
    //const val BASE_URL = "http://192.168.1.136:8080"

    private val client = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS) // Время на соединение
        .readTimeout(180, TimeUnit.SECONDS)    // Время ожидания ответа
        .writeTimeout(180, TimeUnit.SECONDS)   // Время на отправку
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // Логируем полный запрос/ответ для отладки
        })
        .build()

    fun create(): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}