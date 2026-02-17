package com.example.tourist_main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.Geofence

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val statusIntent = Intent("GEOFENCE_STATUS")

        when (geofencingEvent.geofenceTransition) {

            Geofence.GEOFENCE_TRANSITION_ENTER ->
                statusIntent.putExtra("status", "Inside Safe Zone")

            Geofence.GEOFENCE_TRANSITION_EXIT ->
                statusIntent.putExtra("status", "Outside Safe Zone")
        }

        context.sendBroadcast(statusIntent)
    }
}
