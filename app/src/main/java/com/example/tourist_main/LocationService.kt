package com.example.tourist_main

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.model.RadarEvent

class LocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var userId: String = ""
    private var lastSentTime = 0L
    private val SEND_INTERVAL = 30_000L

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "Service started")

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        userId = auth.currentUser?.uid ?: ""

        if (hasLocationPermission()) {

           // Radar.setUserId(userId)
            Radar.setUserId(userId)
                      // Radar.startTracking(RadarTrackingOptions.RESPONSIVE)

            startNotification()
            startLocationUpdates()

        } else {
            Log.e("LocationService", "Location permission not granted")
            stopSelf()
        }
    }

    // ================= FOREGROUND NOTIFICATION =================
    private fun startNotification() {

        val channelId = "location_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tourist Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tourist Safety Active Radar")
            .setContentText("Tracking location in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    // ================= LOCATION =================
    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {

        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            15_000
        ).build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {

            val loc = result.lastLocation ?: return
            Radar.trackOnce { status, location, events, user ->

                if (status == Radar.RadarStatus.SUCCESS && events != null) {

                    for (event in events) {

                        when (event.type) {

                            RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE -> {
                                sendGeofenceBroadcast(true)
                            }

                            RadarEvent.RadarEventType.USER_EXITED_GEOFENCE -> {
                                sendGeofenceBroadcast(false)
                            }

                            else -> {}
                        }
                    }
                }
            }
            val now = System.currentTimeMillis()

            if (now - lastSentTime < SEND_INTERVAL) return
            lastSentTime = now

            if (userId.isEmpty()) return

            val data = hashMapOf(
                "lat" to loc.latitude,
                "lng" to loc.longitude,
                "accuracy" to loc.accuracy,
                "time" to now
            )

            firestore.collection("users")
                .document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener {
                    Log.e("BG_LOCATION", "Firestore error: ${it.message}")
                }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun sendGeofenceBroadcast(isSafe: Boolean) {
        val intent = Intent("GEOFENCE_STATUS")
        intent.putExtra("isSafe", isSafe)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
        Radar.stopTracking()
    }
}
