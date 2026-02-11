package com.example.tourist_main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.encoder.QRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.material.chip.Chip
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
//



import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class MainActivity : AppCompatActivity( ) {
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null


    private val TAG = "TouristMain"

    // Firebase
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // UI state
    private var fullName: String = ""
    private var nationality: String = ""

    // Permission launcher
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallbackInternal: LocationCallback? = null

    // Weather API key (replace with your key)
    private val OPENWEATHER_API_KEY = "458dfe7fe95b9159d0534189a934a6bf"

    private var geofenceStatusFromWeb: String =""

    // ================= SOS CONFIG (INSERTED) =================
    private var sosProgress = 0
    private var sosRunnable: Runnable? = null
    private var sosPressStartTime: Long = 0
    private val SOS_HOLD_DURATION = 2000L // 2 seconds
    private val PROGRESS_INTERVAL = 20L   // update every 20 ms
    // ================= VIBRATION (INSERTED) =================
    private lateinit var vibrator: Vibrator
// =======================================================


    // ========================================================

    // ================= SMS PERMISSION (INSERTED) =================
    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                sendSOS_SMS()
            } else {
                Toast.makeText(this, "SMS permission required for SOS", Toast.LENGTH_LONG).show()
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                checkBackgroundLocation()
            }
        }

    private var lat : Double = 0.0
    private var lng : Double = 0.0

    /// user info
    private var name: String? = null
    private var email: String? = null
    private var phone: String? = null

    private var primaryName: String? = null
    private var primaryPhone: String? = null
    private var primaryRelation: String? = null

    private var secondaryName: String? = null
    private var secondaryPhone: String? = null
    private var secondaryRelation: String? = null

    private var bloodType: String? = null
    private var allergies: String? = null
    private var medicalConditions: String? = null
    private var medications: String? = null

    private lateinit var alertBtn: MaterialButton





    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sosButton = findViewById<Button>(R.id.sos_button)
        val sosProgressBar = findViewById<ProgressBar>(R.id.sos_progress)
        sosProgressBar.progress = 0
        // ================= INIT VIBRATOR (INSERTED) =================
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
// ===========================================================



        sosButton.setOnTouchListener { _, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    sosProgress = 0
                    sosProgressBar.progress = 0

                    sosRunnable = object : Runnable {
                        override fun run() {
                            sosProgress++
                            sosProgressBar.progress = sosProgress

                            // 🎨 COLOR CHANGE BASED ON PROGRESS
                            when {
                                sosProgress >= 90 -> {
                                    sosProgressBar.progressDrawable.setTint(
                                        ContextCompat.getColor(this@MainActivity, R.color.sos_danger)
                                    )
                                }
                                sosProgress >= 70 -> {
                                    sosProgressBar.progressDrawable.setTint(
                                        ContextCompat.getColor(this@MainActivity, R.color.sos_warning)
                                    )
                                }
                                else -> {
                                    sosProgressBar.progressDrawable.setTint(
                                        ContextCompat.getColor(this@MainActivity, R.color.sos_safe)
                                    )
                                }
                            }

                            // 📳 GENTLE VIBRATION WHILE FILLING
                            if (vibrator.hasVibrator()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(
                                            100,
                                            VibrationEffect.EFFECT_HEAVY_CLICK
                                        )
                                    )
                                } else {
                                    vibrator.vibrate(100)
                                }
                            }

                            if (sosProgress >= 100) {
                                vibrator.cancel()
                                triggerSOS() // 🚨 FINAL SOS
                            } else {
                                sosProgressBar.postDelayed(this, SOS_HOLD_DURATION / 100)
                            }
                        }
                    }

                    sosProgressBar.postDelayed(
                        sosRunnable!!,
                        SOS_HOLD_DURATION / 100
                    )

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    sosRunnable?.let { sosProgressBar.removeCallbacks(it) }
                    sosProgressBar.progress = 0
                    vibrator.cancel() // 🛑 stop vibration
                    true
                }

                else -> false
            }
        }
        // init fused client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Ensure user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Register permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.d(TAG, "Location permission granted")
                Toast.makeText(this, "Permission granted — obtaining location...", Toast.LENGTH_SHORT).show()
                getLocation()
            } else {
                Log.w(TAG, "Location permission denied")
                Toast.makeText(this, "Location permission denied. Weather unavailable.", Toast.LENGTH_LONG).show()
            }
        }

        // Check permission; request if needed; otherwise start getting location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLocation()
        }
        // sms
      // checkAndRequestLocation()



        // --- UI and Firebase setup (keeps your existing app features) ---
        val uid = currentUser.uid

        // find views
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val homeContainer = findViewById<ScrollView>(R.id.home_page_container)
        val mapContainer = findViewById<ScrollView>(R.id.map_page_container)
        val idContainer = findViewById<ScrollView>(R.id.id_page_container)
        val profileContainer = findViewById<ScrollView>(R.id.profile_page_container)
        val btnLogOut: Button = findViewById(R.id.btnLogOut)
        val QRCode : ImageView =findViewById(R.id.qrCode)
       val zone : Chip = findViewById(R.id.zone)



        // default display: show home
        homeContainer.visibility = View.VISIBLE
        mapContainer.visibility = View.GONE
        idContainer.visibility = View.GONE
        profileContainer.visibility = View.GONE




        // Load Firestore user data


        // Logout
        btnLogOut.setOnClickListener {
            val session = SessionManager(this)
            session.saveLoginState(false)
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Bottom nav behavior
        bottomNav.setOnItemSelectedListener { item ->
            // hide all pages first
            homeContainer.visibility = View.GONE
            mapContainer.visibility = View.GONE
            idContainer.visibility = View.GONE
            profileContainer.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_home -> {
                    homeContainer.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.greeting_textview).text = "Hello $fullName"
                    val btnShare = findViewById<LinearLayout>(R.id.btnShare)

                    btnShare.setOnClickListener {
                        shareMessage()
                        Toast.makeText(this, "Location shared", Toast.LENGTH_SHORT).show()
                    }


                }
                R.id.nav_map -> {
                    mapContainer.visibility = View.VISIBLE
                     alertBtn = findViewById(R.id.alert_btn)
                    alertBtn.setOnClickListener {
                        Toast.makeText(this, "🚨 Alert Button Clicked", Toast.LENGTH_SHORT).show()
                        shareMessage()
                    }
                    val startBtn = findViewById<MaterialButton>(R.id.start_trace)

                    val prefs = getSharedPreferences("tracking_pref", MODE_PRIVATE)

                    var isTracking = prefs.getBoolean("tracking", false)

                    updateTrackingUI(startBtn, isTracking)

                    startBtn.setOnClickListener {

                        if (!isTracking) {

                            // ✅ CHECK LOCATION PERMISSION FIRST
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {

                                val intent = Intent(this, LocationService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent)
                                } else {
                                    startService(intent)
                                }

                                isTracking = true
                                prefs.edit().putBoolean("tracking", true).apply()

                                Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show()

                            } else {
                                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                return@setOnClickListener
                            }

                        } else {

                            // ✅ STOP TRACKING
                            stopService(Intent(this, LocationService::class.java))

                            isTracking = false
                            prefs.edit().putBoolean("tracking", false).apply()

                            Toast.makeText(this, "Tracking Stopped", Toast.LENGTH_SHORT).show()
                        }

                        updateTrackingUI(startBtn, isTracking)


                     }




                }
                R.id.nav_id -> {
                    idContainer.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.idName).text = fullName
                    findViewById<TextView>(R.id.r_nationality).text = "Nationality: $nationality"
                    val qr = generateQRCode("Ganesh")
                    QRCode.setImageBitmap(qr)

                }
                R.id.nav_profile -> {
                    profileContainer.visibility = View.VISIBLE
                    val btnsetting=findViewById<ImageView>(R.id.settings_button)
                    findViewById<TextView>(R.id.p_username).text = fullName
                    findViewById<TextView>(R.id.p_email).text = email
                    findViewById<TextView>(R.id.p_phone).text = phone
                    findViewById<TextView>(R.id.p_blood).text = bloodType
                    findViewById<TextView>(R.id.p_allergies).text = allergies
                    findViewById<TextView>(R.id.p_conditions).text = medicalConditions
                    findViewById<TextView>(R.id.p_medications).text = medications
                    findViewById<TextView>(R.id.p_primaryName).text = primaryName
                    findViewById<TextView>(R.id.p_primaryPhone).text = primaryPhone
                  //  findViewById<TextView>(R.id.p_primaryRelation).text = primaryRelation
                    findViewById<TextView>(R.id.p_secondaryName).text = secondaryName
                    findViewById<TextView>(R.id.p_secondaryPhone).text = secondaryPhone
                    findViewById<TextView>(R.id.p_secondaryRelation).text = secondaryRelation



                    btnsetting.setOnClickListener { view ->
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }

                }
            }
            true
        }

        // --- START: WebView JS bridge ---
        // Attach TouristAppInterface if a WebView with id 'webview' exists in your layout.
        // This will allow JS to call:
        //   window.TouristAppInterface.setGeofenceStatus("Geofence Entered")
        //   window.TouristAppInterface.getGeofenceStatus()
        try {
            // load your live website (replace with your real URL)
            val webView = findViewById<WebView?>(R.id.web)
            val yourUrl = "https://xm5a.github.io/JS-Interface-Test/"
            webView.loadUrl("https://xm5a.github.io/JS-Interface-Test/")

            if (webView != null) {
                // --- WebView settings ---
                val settings: WebSettings = webView.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setGeolocationEnabled(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                // WebChromeClient: forward console messages and grant geolocation prompt
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d(TAG, "WebViewConsole: ${consoleMessage?.message()} -- ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}")
                        return super.onConsoleMessage(consoleMessage)
                    }

                    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                        // Auto-allow geolocation (change if you need a user prompt)
                        callback?.invoke(origin, true, false)
                    }
                }

                // Attach the JS interface under the exact name used by your HTML.
                // TouristAppInterface must accept a callback: TouristAppInterface(activity) { status -> ... }
                // --- Attach JSBridge as "Android" so page's Android.handleAction(...) will be handled ---
                // create a single instance (class property is better: private lateinit var bridge: jsbridge)
                val bridge = TouristAppInterface(
                    this, onAction = { action, payload ->
                        // Called when web calls Android.handleAction(...)
                        Log.d(TAG, "JSBridge action received -> action=$action payload=$payload")

                        when (action) {
                            "locationUpdate" -> {
                                payload?.let {
                                    lat = it.optDouble("lat", Double.NaN)
                                    lng = it.optDouble("lng", Double.NaN)
                                    val acc = it.optDouble("accuracy", Double.NaN)
                                    val ts = it.optString("timestamp", "")
                                    val s=it.optString("Hi","error")
                                    val city=it.optString("City","error")
                                    val isSafeZone = true   // change this based on geo-fence / API / logic

                                    Log.d(TAG, "LocationUpdate from web: lat=$lat lng=$lng acc=$acc ts=$ts s=$s")

                                    geofenceStatusFromWeb = "$s"
                                    val temp ="$s"
                                    runOnUiThread {
                                        if(geofenceStatusFromWeb==temp) {
                                          /*  zone.text = geofenceStatusFromWeb
                                            zone.setTextColor(Color.WHITE)
                                            zone.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("red")))*/
                                            zone.text = city
                                            zone.setTextColor(Color.WHITE)
                                            zone.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("green")))
                                            updateSafetyUI(isSafeZone)


                                        }
                                        else {
                                            zone.text = "Inside"
                                            zone.setTextColor(Color.WHITE)
                                            zone.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("green")))
                                        }
                                    }
                                }
                            }

                            "startTracking" -> {
                                Log.d(TAG, "Start tracking requested by web")
                                // start native tracking here
                            }

                            "stopTracking" -> {
                                Log.d(TAG, "Stop tracking requested by web")
                                // stop native tracking here
                            }

                            else -> {
                                Log.w(TAG, "Unhandled action from web: $action")
                            }
                        }
                    }
                )

// Attach the same instance ONCE under both names the page might use:
               // webView.addJavascriptInterface(bridge, "Android")
               webView.addJavascriptInterface(bridge, "TouristAppInterface")



                // call on UI thread (safe)


                // Load the URL after settings and interface are attached

                Log.d(TAG, "TouristAppInterface attached to WebView (id=web)")
            } else {
                Log.w(TAG, "No WebView with id 'web' found. TouristAppInterface NOT attached.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach TouristAppInterface: ${e.message}", e)
        }

        hideSystemBars()
        // --- END: WebView JS bridge ---

        // call background service
     }

    // Start bg tacking service btn behavior
    private fun updateTrackingUI(button: MaterialButton, isTracking: Boolean) {

        if (isTracking) {
            // 🔴 Tracking ON
            button.text = "Stop Tracking"
            button.setBackgroundColor(Color.parseColor("#D32F2F")) // Red
            button.setTextColor(Color.WHITE)
        } else {
            // 🟢 Tracking OFF
            button.text = "Start Tracking"
            button.setBackgroundColor(Color.parseColor("#388E3C")) // Green
            button.setTextColor(Color.WHITE)
        }
    }



    // user doc

    //For Back ground service

    private fun startBackgroundLocation() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkAndRequestLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Android 10+ → send user to settings
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                startBackgroundLocation()
            }
        } else {
            startBackgroundLocation()
        }
    }

    //---------------------------------------- Home share loc
private fun shareMessage() {

    // STEP 4.1: Your predefined data


    // STEP 4.2: Predefined message
    val message = """
    🚨 Emergency Alert 🚨
    I need help. This is my current location:
    https://www.google.com/maps?q=$lat,$lng
    """.trimIndent()

    // STEP 4.3: Share intent
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }

    // STEP 4.4: Open Share Drawer
    startActivity(
        Intent.createChooser(shareIntent, "Share via")
    )
}






 //  ---------------------------- //SOS----------------------------------------
    private fun updateSafetyUI(isSafe: Boolean) {

        val card = findViewById<MaterialCardView>(R.id.safetyCard)
        val icon = findViewById<ImageView>(R.id.safetyIcon)
        val title = findViewById<TextView>(R.id.safetyTitle)
        val location = findViewById<TextView>(R.id.location_text)

        if (isSafe) {
            // 🟢 SAFE
            card.setCardBackgroundColor(Color.parseColor("#F0FFF4"))
            card.strokeColor = Color.parseColor("#A7F3D0")

            icon.setColorFilter(Color.parseColor("#10B981"))

            title.text = "You are in a Safe Zone"
            title.setTextColor(Color.parseColor("#065F46"))

            location.setTextColor(Color.parseColor("#047857"))
            location.text = "(Safe Area)"

        } else {
            // 🔴 DANGER
            card.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            card.strokeColor = Color.parseColor("#FCA5A5")

            icon.setColorFilter(Color.parseColor("#DC2626"))

            title.text = "You are in a Danger Zone"
            title.setTextColor(Color.parseColor("#7F1D1D"))

            location.setTextColor(Color.parseColor("#B91C1C"))
            location.text = "Unsafe Location"
        }
    }

    // for sms
    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted
            // sendSOS()
        } else {
            // Show system permission popup
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    // sos
    // ================= TRIGGER SOS (INSERTED) =================
    private fun triggerSOS() {

        val prefs = getSharedPreferences("sos_pref", MODE_PRIVATE)
        val isConfirmed = prefs.getBoolean("sos_confirmed", false)

        if (!isConfirmed) {

            // Show confirmation only first time
            AlertDialog.Builder(this)
                .setTitle("Emergency Alert")
                .setMessage("Allow app to send emergency SMS automatically in future?")
                .setCancelable(false)
                .setPositiveButton("YES") { _, _ ->

                    // Save confirmation permanently
                    prefs.edit().putBoolean("sos_confirmed", true).apply()

                    checkAndSendSMS()
                }
                .setNegativeButton("CANCEL", null)
                .show()

        } else {
            // Already confirmed once
            checkAndSendSMS()
        }
    }
    private fun checkAndSendSMS() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            sendSOS_SMS()

        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    // =========================================================

    // ================= SEND SMS OFFLINE (INSERTED) =================
    private fun sendSOS_SMS() {

        val contacts = getEmergencyContacts()

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts saved", Toast.LENGTH_LONG).show()
            return
        }

        val message = """
        🚨 Emergency Alert 🚨
        I need help. My location:
        https://www.google.com/maps?q=$lat,$lng
    """.trimIndent()

        val smsManager = SmsManager.getDefault()

        for (number in contacts) {
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "SMS failed: ${e.message}")
            }
        }

        Toast.makeText(this, "SOS sent successfully", Toast.LENGTH_LONG).show()
    }

    // ===============================================================

    // ================= EMERGENCY CONTACTS (INSERTED) =================
    //-------------Admin-------------
    private fun getEmergencyContacts(): List<String> {
        val contacts = mutableListOf<String>()

        if (!primaryPhone.isNullOrBlank() && primaryPhone != "Nan") {
            contacts.add(primaryPhone!!)
        }

        if (!secondaryPhone.isNullOrBlank() && secondaryPhone != "Nan") {
            contacts.add(secondaryPhone!!)
        }

        return contacts
    }

    // For QR code generation:

    fun generateQRCode(text: String, size: Int = 800): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
        )

        val matrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bmp
    }

    // to hind sys bar (for Navigation bar)
    private fun hideSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Show transiently on swipe
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // --- LOCATION: one-shot then fallback to updates ---

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "getLocation: permission missing")
            return
        }

        // one-shot fresh high-accuracy location
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "getCurrentLocation -> success: ${location.latitude}, ${location.longitude}")
                    fetchWeather(location)
                    //findViewById<TextView>(R.id.location_text).text = " ${location.latitude}, ${location.longitude} "

                } else {
                    Log.d(TAG, "getCurrentLocation -> null; falling back to updates")
                    checkAndStartLocationUpdates()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "getCurrentLocation failed: ${e.message}", e)
                checkAndStartLocationUpdates()
            }
    }

    private fun checkAndStartLocationUpdates() {
        val checkRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .build()

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(checkRequest)
            .build()

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                Log.d(TAG, "Device location settings satisfied -> start updates")
                startLocationUpdates()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Location settings not satisfied: ${exception.message}")
                Toast.makeText(this, "Please enable device Location (GPS) and retry.", Toast.LENGTH_LONG).show()
                // Open Android location settings so user can enable GPS quickly
                try {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open location settings: ${e.message}", e)
                }
            }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "startLocationUpdates: missing permission")
            return
        }

        if (locationCallbackInternal == null) {
            locationCallbackInternal = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    val loc: Location? = result.lastLocation
                    if (loc != null) {
                        Log.d(TAG, "onLocationResult -> lat=${loc.latitude}, lon=${loc.longitude}")
                        fetchWeather(loc)
                        // If you want only first update then stop updates:
                        stopLocationUpdates()
                    } else {
                        Log.d(TAG, "onLocationResult -> location null")
                    }
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .setMaxUpdateDelayMillis(60_000L)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallbackInternal!!, Looper.getMainLooper())
        Log.d(TAG, "Requested location updates (foreground)")
    }

    private fun stopLocationUpdates() {
        locationCallbackInternal?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallbackInternal = null
            Log.d(TAG, "Stopped location updates")
        }
    }

    // --- WEATHER: fetch and update your layout IDs (imgWeather, txtLocation, txtTemp) ---
    private fun fetchWeather(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        val apiKey = OPENWEATHER_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_OPENWEATHER_API_KEY") {
            Toast.makeText(this, "OpenWeather API key missing — set OPENWEATHER_API_KEY.", Toast.LENGTH_LONG).show()
            Log.w(TAG, "OpenWeather API key missing.")
            return
        }

        lifecycleScope.launch {
            try {
                // 1) get weather
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getWeather(lat, lon, apiKey)
                }
                val weather = response.weather.firstOrNull()
                val description = weather?.description?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                } ?: "--"

                // 2) Update UI IMMEDIATELY with best-available name (response.name) and temp/icon
                val txtLocationView = findViewById<TextView>(R.id.txtLocation)
                val txtTempView = findViewById<TextView>(R.id.txtTemp)
                val imgView = findViewById<ImageView>(R.id.imgWeather)

                // show response.name (fast) or a placeholder while reverse-geocoding runs
                val initialName = if (!response.name.isNullOrBlank()) response.name else "Updating location..."
                txtLocationView.text = initialName
                txtTempView.text = "%.0f°C, %s".format(response.main.temp, description)
                val iconUrl = weather?.icon?.let { "https://openweathermap.org/img/wn/${it}@2x.png" }
                if (!iconUrl.isNullOrEmpty()) imgView.load(iconUrl)

                Log.d(TAG, "Weather updated immediately: ${response.main.temp}°C, $description for $lat,$lon")

                // 3) Fire reverse-geocoding in background (non-blocking). If it returns a nicer name, update the UI.
                lifecycleScope.launch(Dispatchers.IO) {
                    val geoName = try {
                        reverseGeocodeName(lat, lon) // your suspend helper; it uses Dispatchers.IO internally
                    } catch (e: Exception) {
                        Log.w(TAG, "reverseGeocodeName error: ${e.message}")
                        null
                    }

                    if (!geoName.isNullOrBlank()) {
                        // Only update if different/meaningful (avoid flicker)
                        if (geoName != initialName) {
                            withContext(Dispatchers.Main) {
                                txtLocationView.text = geoName
                                Log.d(TAG, "Location label updated from reverse geocode -> $geoName")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "fetchWeather failed: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Weather fetch failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
    private val geocodeCache = mutableMapOf<String, String>()

    private suspend fun reverseGeocodeName(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                val list = geocoder.getFromLocation(lat, lon, 5)

                if (list.isNullOrEmpty()) return@withContext null

                val addr = list[0]
                val candidates = listOf(
                    addr.locality,
                    addr.subLocality,
                    addr.featureName,
                    addr.thoroughfare,
                    addr.subAdminArea,
                    addr.adminArea
                )

                for (c in candidates) {
                    if (!c.isNullOrBlank()) return@withContext c.trim()
                }

                // fallback
                if (addr.maxAddressLineIndex >= 0) {
                    val line = addr.getAddressLine(0)
                    if (!line.isNullOrBlank()) {
                        return@withContext line.split(",").first().trim()
                    }
                }

                null
            } catch (e: Exception) {
                Log.e("Geocode", "Reverse geocoding failed: ${e.message}")
                null
            }
        }
    }

    private fun attachUserListener(uid: String) {

        userListener = db.collection("users")
            .document(uid)
            .addSnapshotListener { doc, error ->

                if (error != null) {
                    Log.e(TAG, "Snapshot error: ${error.message}")
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {

                    fullName = doc.getString("fullName") ?: ""
                    nationality = doc.getString("nationality") ?: ""
                    email = doc.getString("email") ?: ""
                    phone = doc.getString("mobile") ?: ""

                    primaryName = doc.getString("name1") ?: ""
                    primaryPhone = doc.getString("phone1") ?: ""
                    secondaryName = doc.getString("name2") ?: ""
                    secondaryPhone = doc.getString("phone2") ?: ""

                    bloodType = doc.getString("bloodType") ?: ""
                    allergies = doc.getString("allergies") ?: ""
                    medicalConditions = doc.getString("conditions") ?: ""
                    medications = doc.getString("medications") ?: ""

                    val greeting =
                        if (fullName.isNotEmpty()) "Hello $fullName"
                        else "Welcome"

                    findViewById<TextView>(R.id.greeting_textview).text = greeting

                    Log.d(TAG, "User data updated in real-time 🔥")
                    updateProfileUI()

                }
            }
    }
    private fun updateProfileUI() {

        findViewById<TextView>(R.id.p_username).text = fullName
        findViewById<TextView>(R.id.p_email).text = email
        findViewById<TextView>(R.id.p_phone).text = phone
        findViewById<TextView>(R.id.p_blood).text = bloodType
        findViewById<TextView>(R.id.p_allergies).text = allergies
        findViewById<TextView>(R.id.p_conditions).text = medicalConditions
        findViewById<TextView>(R.id.p_medications).text = medications
        findViewById<TextView>(R.id.p_primaryName).text = primaryName
        findViewById<TextView>(R.id.p_primaryPhone).text = primaryPhone
        findViewById<TextView>(R.id.p_secondaryName).text = secondaryName
        findViewById<TextView>(R.id.p_secondaryPhone).text = secondaryPhone
        findViewById<TextView>(R.id.p_secondaryRelation).text = secondaryRelation
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser ?: return
        attachUserListener(currentUser.uid)
    }


    override fun onStop() {
        super.onStop()
        userListener?.remove()
    }

}
