package com.example.tourist_main

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var userId: String = ""
    private var lastSentTime = 0L
    private val SEND_INTERVAL = 30_000L // 30 seconds

    override fun onCreate() {
        super.onCreate()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // Get dynamic user ID
        userId = auth.currentUser?.uid ?: ""

        startNotification()
        startLocationUpdates()
    }

    // 🔐 Permission check
    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startNotification() {
        val channelId = "location_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Location",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tourist Safety Active")
            .setContentText("Tracking location in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {

        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            15_000
        ).build()

        try {
            fusedClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {

            val loc = result.lastLocation ?: return
            val now = System.currentTimeMillis()

            // ⛔ send only every 30 seconds
            if (now - lastSentTime < SEND_INTERVAL) return
            lastSentTime = now

            if (userId.isEmpty()) return

            val data = hashMapOf(
                "lat" to loc.latitude,
                "lng" to loc.longitude,
                "accuracy" to loc.accuracy,
                "time" to now
            )

            firestore
                .collection("users")
                .document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener {
                    Log.e("BG_LOCATION", "Firestore error: ${it.message}")
                }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
