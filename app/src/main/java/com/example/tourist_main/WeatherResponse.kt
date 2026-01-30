package com.example.tourist_main // <- same package

data class WeatherResponse(
    val weather: List<WeatherItem>,
    val main: Main,
    val name: String = ""
)

data class WeatherItem(
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double
)
