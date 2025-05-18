package com.example.moviereviewapp.di

import com.example.moviereviewapp.data.api.TMDBService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d("NetworkModule", "Making request to: ${request.url}")
                Log.d("NetworkModule", "Request headers: ${request.headers}")
                
                try {
                    val response = chain.proceed(request)
                    Log.d("NetworkModule", "Response code: ${response.code}")
                    Log.d("NetworkModule", "Response headers: ${response.headers}")
                    
                    val responseBody = response.peekBody(Long.MAX_VALUE).string()
                    Log.d("NetworkModule", "Response body: $responseBody")
                    
                    if (!response.isSuccessful) {
                        Log.e("NetworkModule", "Error response: ${response.code} - $responseBody")
                    }
                    
                    response.newBuilder()
                        .body(responseBody.toResponseBody(response.body?.contentType()))
                        .build()
                } catch (e: Exception) {
                    Log.e("NetworkModule", "Network error: ${e.message}", e)
                    throw e
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTMDBService(retrofit: Retrofit): TMDBService {
        return retrofit.create(TMDBService::class.java)
    }

    @Provides
    @Singleton
    fun provideApiKey(): String {
        // TODO: Replace with your actual TMDB API key
        return "YOUR_API_KEY"
    }
} 