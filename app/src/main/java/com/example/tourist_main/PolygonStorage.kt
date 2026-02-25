package com.example.tourist_main

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng

object PolygonStorage {

    private const val PREF_NAME = "geo_prefs"
    private const val KEY_POLYGON = "polygon_data"

    private var polygonPoints: MutableList<LatLng> = mutableListOf()

    // Save in memory + SharedPreferences
    fun save(context: Context, points: List<LatLng>) {
        polygonPoints.clear()
        polygonPoints.addAll(points)

        val jsonArray = JSONArray()
        points.forEach {
            val obj = JSONObject()
            obj.put("lat", it.latitude)
            obj.put("lng", it.longitude)
            jsonArray.put(obj)
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_POLYGON, jsonArray.toString())
            .apply()
    }

    fun get(applicationContext: Context): List<LatLng> = polygonPoints

    fun isAvailable(): Boolean = polygonPoints.isNotEmpty()

    // Load from SharedPreferences after reboot
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_POLYGON, null) ?: return

        val jsonArray = JSONArray(jsonString)
        polygonPoints.clear()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val lat = obj.getDouble("lat")
            val lng = obj.getDouble("lng")
            polygonPoints.add(LatLng(lat, lng))
        }
    }

    fun clear(context: Context) {
        polygonPoints.clear()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_POLYGON)
            .apply()
    }
}