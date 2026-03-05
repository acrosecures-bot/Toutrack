package com.example.tourist_main

import RadarApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RadarRetrofit {

    private const val BASE_URL = "https://api.radar.io/"

    val api: RadarApi by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadarApi::class.java)
    }
}