package com.example.pitjarusproject.api

import com.example.pitjarusproject.api.response.Response
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface API {
    @FormUrlEncoded
    @POST("loginTest")
    fun getData(
        @Field("username") username: String,
        @Field("password") password: String,
    ): Call<Response>
}