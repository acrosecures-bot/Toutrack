package com.example.tourist_main

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class LocationService : Service() {
    private var currentState: String? = null
    private var lastLocationUploadTime: Long = 0

    private lateinit var fusedClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        if (!PolygonStorage.isAvailable()) {
            PolygonStorage.load(this)
        }

        startForegroundService()
        startLocationUpdates()
    }

    private fun startForegroundService() {

        val channelId = "location_channel"

        val channel = NotificationChannel(
            channelId,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Location")
            .setContentText("Geofence monitoring active..\nfinding geofence status....")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return   // Stop if permission not granted
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->

                val lat = location.latitude
                val lng = location.longitude

                checkGeofence(lat, lng)
            }
        }
    }
    private fun isPointInsidePolygon(
        lat: Double,
        lng: Double,
        polygon: List<org.maplibre.android.geometry.LatLng>
    ): Boolean {

        var result = false
        var j = polygon.size - 1

        for (i in polygon.indices) {

            val xi = polygon[i].latitude
            val yi = polygon[i].longitude
            val xj = polygon[j].latitude
            val yj = polygon[j].longitude

            val intersect = ((yi > lng) != (yj > lng)) &&
                    (lat < (xj - xi) * (lng - yi) / (yj - yi) + xi)

            if (intersect) result = !result

            j = i
        }

        return result
    }

    private fun checkGeofence(lat: Double, lng: Double) {

        if (!PolygonStorage.isAvailable()) return

        val polygon = PolygonStorage.get(applicationContext)

        val isInside = isPointInsidePolygon(lat, lng, polygon)

        val newState = if (isInside) "INSIDE" else "OUTSIDE"

        // ✅ Trigger only when state changes
        if (newState != currentState) {
            currentState = newState

            updateNotification(newState)
            updateStateInFirebase(newState)
        }

        // ✅ Upload location every 20 seconds
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLocationUploadTime > 20000) {
            lastLocationUploadTime = currentTime
            uploadLocationToFirebase(lat, lng)
        }
    }
    private fun updateNotification(state: String) {

        val channelId = "location_channel"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Geofence Status")
            .setContentText("You are $state")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }
    private fun updateStateInFirebase(state: String) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("geofenceStatus", state)
    }
    private fun uploadLocationToFirebase(lat: Double, lng: Double) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val locationData = hashMapOf(
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to System.currentTimeMillis(),
            "userid" to userId,
         )

        FirebaseFirestore.getInstance()
            .collection("user_locations")
            .document(userId)
            .set(locationData)
    }
     override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}