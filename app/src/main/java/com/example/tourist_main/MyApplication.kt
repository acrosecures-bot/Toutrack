package com.example.tourist_main

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Radar.initialize(
            this,
            "prj_live_pk_ed91d1d636ae3f3366727a2c648ba6a976e29d40",
         )

        // Set user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            Radar.setUserId(userId)
        }

        // 🔥 THIS IS THE IMPORTANT LINE
       // Radar.startTracking(RadarTrackingOptions.CONTINUOUS)
    }
}