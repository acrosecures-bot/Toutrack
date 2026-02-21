package com.example.tourist_main

import RadarApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RadarRetrofit {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.radar.io/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: RadarApi = retrofit.create(RadarApi::class.java)
}
