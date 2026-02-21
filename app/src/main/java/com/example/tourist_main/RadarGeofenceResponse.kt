package com.example.tourist_main

data class RadarGeofenceResponse(
    val geofences: List<RadarGeofence>
)

data class RadarGeofence(
    val geometry: Geometry
)

data class Geometry(
    val coordinates: List<List<List<Double>>>
)
