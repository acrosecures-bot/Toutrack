package com.example.tourist_main

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject

/**
 * A merged interface that replaces BOTH:
 *
 *   JSBridge  → Android.handleAction(action, payload)
 *   TouristAppInterface → window.TouristAppInterface.setGeofenceStatus(...)
 *                         window.TouristAppInterface.getGeofenceStatus()
 *
 * Attach like:
 *   webView.addJavascriptInterface(MergedBridge(this, callback), "Android")
 *   webView.addJavascriptInterface(MergedBridge(this, callback), "TouristAppInterface")
 *
 * Your webpage can now use BOTH names:
 *   Android.handleAction(...)
 *   TouristAppInterface.setGeofenceStatus(...)
 */
class TouristAppInterface(
    private val activity: Activity,
     private val onAction: (String, JSONObject?) -> Unit
) {
    private val ui = Handler(Looper.getMainLooper())

    // stores latest geofence status
    @Volatile
    private var lastGeofenceStatus: String = "Unknown"

    // region ===== TouristAppInterface (Geofence) =====



    @JavascriptInterface
    fun getGeofenceStatus(): String {
        Log.d("jsbridge", "getGeofenceStatus -> $lastGeofenceStatus")
        return lastGeofenceStatus
    }


    // endregion ======================================


    // region ===== JSBridge (Android.handleAction) =====

    @JavascriptInterface
    fun handleAction(action: String?, jsonPayload: String?) {
        ui.post {
            try {
                val a = action ?: ""
                val payload =
                    if (!jsonPayload.isNullOrBlank()) JSONObject(jsonPayload) else null

                Log.d("MergedBridge", "handleAction -> $a payload=$jsonPayload")
                onAction(a, payload)

            } catch (e: Exception) {
                Log.e("MergedBridge", "Invalid JSON payload: $jsonPayload", e)
                Toast.makeText(activity, "Invalid JSON from Web", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // endregion ======================================
}
