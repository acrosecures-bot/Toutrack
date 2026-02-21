package com.example.tourist_main

import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat


class MyRadarReceiver : RadarReceiver() {

    companion object {
        private var lastStatus = "SAFE"   // Prevent repeated triggers
    }

    override fun onEventsReceived(
        context: Context,
        events: Array<RadarEvent>,
        user: RadarUser?
    ) {

        val userId = Radar.getUserId() ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                val assignedGeofence = document.getString("geofenceassign")

                for (event in events) {

                    val triggeredGeofenceId = event.geofence?.externalId

                    // Only react if this is the assigned geofence
                    if (triggeredGeofenceId == assignedGeofence) {

                        when (event.type) {

                            RadarEvent.RadarEventType.USER_EXITED_GEOFENCE -> {

                                if (lastStatus != "DANGER") {
                                    lastStatus = "DANGER"
                                    handleDanger(context, userId)
                                }
                            }

                            RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE -> {

                                if (lastStatus != "SAFE") {
                                    lastStatus = "SAFE"
                                    handleSafe(context, userId)
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
    }

    private fun handleDanger(context: Context, userId: String) {

        Log.d("RADAR", "User exited assigned geofence → DANGER")

        // 🔔 Show Notification
        showNotification(context, "⚠ You entered Danger Zone")


        // 🟢 Update UI via Broadcast
        val intent = Intent("GEOFENCE_STATUS")
        intent.putExtra("isSafe", false)
        context.sendBroadcast(intent)

        // ☁ Save to Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("geofenceStatus", "DANGER")

        // 🚨 Auto SOS
        val sosIntent = Intent(context, LocationService::class.java)
        sosIntent.action = "AUTO_SOS"
        context.startService(sosIntent)
    }

    private fun handleSafe(context: Context, userId: String) {

        Log.d("RADAR", "User re-entered assigned geofence → SAFE")

        // 🔔 Show Notification
        showNotification(context, "You entered Safe Zone")

        // 🟢 Update UI via Broadcast
        val intent = Intent("GEOFENCE_STATUS")
        intent.putExtra("isSafe", true)
        context.sendBroadcast(intent)

        // ☁ Save to Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("geofenceStatus", "SAFE")
    }

    private fun showNotification(context: Context, message: String) {

        val channelId = "geofence_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Tourist Safety Alert")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(200, notification)
    }


    override fun onLocationUpdated(
        context: Context,
        location: Location,
        user: RadarUser
    ) {}

    override fun onClientLocationUpdated(
        context: Context,
        location: Location,
        stopped: Boolean,
        source: Radar.RadarLocationSource
    ) {}

    override fun onError(
        context: Context,
        status: Radar.RadarStatus
    ) {}

    override fun onLog(
        context: Context,
        message: String
    ) {}
}
