package com.example.pitjarusproject.api

import okhttp3.OkHttpClient
import okhttp3.internal.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieHandler
import java.net.CookieManager
import java.util.concurrent.TimeUnit

object APIConfig {
    private const val BASE_URL = "http://dev.pitjarus.co/api/sariroti_md/index.php/login/"
    private val client: Retrofit
        get() {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val cookieHandler: CookieHandler = CookieManager()
            val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .cookieJar(JavaNetCookieJar(cookieHandler))
                .connectTimeout(40, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

    val instanceRetrofit: API
        get() = client.create(API::class.java)
}