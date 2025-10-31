package com.example.roamly // Убедитесь, что пакет правильный

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory // или другой конвертер
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Говорим Hilt, что зависимости будут жить пока живо приложение
object AppModule { // или NetworkModule
    private const val BASE_URL = "http://10.52.115.228:8080/"

    // Инструкция №1: Как создавать Retrofit
    @Provides
    @Singleton // Создаем только один экземпляр на все приложение
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL) // <-- ВАЖНО: Укажите ваш базовый URL!
            .addConverterFactory(GsonConverterFactory.create()) // Убедитесь, что у вас есть зависимость для этого
            .build()
    }

    // Инструкция №2: Как создавать ApiService
    // Hilt видит, что этому методу нужен Retrofit. Он посмотрит выше, увидит provideRetrofit() и использует его.
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
