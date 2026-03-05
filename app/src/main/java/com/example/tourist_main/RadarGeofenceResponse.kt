package com.example.tourist_main

data class RadarGeofenceResponse(
    val geofences: List<RadarGeofence>
)

data class RadarGeofence(
    val _id: String,
    val description: String?,
    val externalId: String?,
    val metadata: Map<String, Any>?,
    val geometry: Geometry
)

data class Geometry(
    val type: String,
    val coordinates: List<List<List<Double>>>
)