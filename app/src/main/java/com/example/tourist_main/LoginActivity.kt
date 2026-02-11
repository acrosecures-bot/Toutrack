package com.example.tourist_main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    // Tabs
    private lateinit var btnRegisterTab: Button
    private lateinit var btnSignInTab: Button
    private lateinit var underlineRegister: View
    private lateinit var underlineSignIn: View

    // SignIn views
    private lateinit var signinCard: View
    private lateinit var signinEmail: EditText
    private lateinit var signinPassword: EditText
    private lateinit var btnSignIn: Button

    // Register views
    private lateinit var registerCard: View
    private lateinit var r_fullName: EditText
    private lateinit var r_Gender: EditText
    private lateinit var r_mobile: EditText
    private lateinit var r_email: EditText
    private lateinit var r_password: EditText
    private lateinit var r_confirm: EditText
    private lateinit var r_emerName: EditText
    private lateinit var r_emerNo: EditText
    private lateinit var r_dob: EditText
    private lateinit var r_nationality: EditText
    private lateinit var r_address: EditText
    private lateinit var r_agree: CheckBox
    private lateinit var btnRegister: Button

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Bind views
        btnRegisterTab = findViewById(R.id.btnRegisterTab)
        btnSignInTab = findViewById(R.id.btnSignInTab)
        underlineRegister = findViewById(R.id.underlineRegister)
        underlineSignIn = findViewById(R.id.underlineSignIn)

        signinCard = findViewById(R.id.signinCard)
        signinEmail = findViewById(R.id.signinEmail)
        signinPassword = findViewById(R.id.signinPassword)
        btnSignIn = findViewById(R.id.btnSignIn)

        registerCard = findViewById(R.id.registerCard)
        r_fullName = findViewById(R.id.r_fullName)
        r_Gender = findViewById(R.id.r_Gender)
        r_mobile = findViewById(R.id.r_mobile)
        r_email = findViewById(R.id.r_email)
        r_password = findViewById(R.id.r_password)
        r_confirm = findViewById(R.id.r_confirm)
        r_emerName = findViewById(R.id.r_emerName)
        r_emerNo = findViewById(R.id.r_emerNo)
        r_dob = findViewById(R.id.r_dob)
        r_nationality = findViewById(R.id.r_nationality)
        r_address = findViewById(R.id.r_address)
        r_agree = findViewById(R.id.r_agree)
        btnRegister = findViewById(R.id.btnRegister)

        auth = FirebaseAuth.getInstance()

        showSignIn()

        btnSignInTab.setOnClickListener { showSignIn() }
        btnRegisterTab.setOnClickListener { showRegister() }

        // Sign in
        btnSignIn.setOnClickListener {
            val email = signinEmail.text.toString().trim()
            val pass = signinPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                signinEmail.error = "Invalid email"
                return@setOnClickListener
            }
            if (pass.length < 6) {
                signinPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Log.d(TAG, "Login success: ${user?.uid}")
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                        SessionManager(this).saveLoginState(true)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Log.e(TAG, "Login failed: ${task.exception?.message}")
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Register
        btnRegister.setOnClickListener {
            val fullName = r_fullName.text.toString().trim()
            val gender = r_Gender.text.toString().trim()
            val mobile = r_mobile.text.toString().trim()        // toString() fixed
            val email = r_email.text.toString().trim()
            val pass = r_password.text.toString().trim()
            val confirm = r_confirm.text.toString().trim()
            val emerName = r_emerName.text.toString().trim()
            val emerNo = r_emerNo.text.toString().trim()        // toString() fixed
            val dob = r_dob.text.toString().trim()             // toString() fixed
            val nationality = r_nationality.text.toString().trim()
            val address = r_address.text.toString().trim()

            // Validation (kept yours)
            if (fullName.isEmpty()) { r_fullName.error = "Full name is required"; return@setOnClickListener }
            if (gender.isEmpty()) { r_Gender.error = "Gender is required"; return@setOnClickListener }
            if (mobile.isEmpty()) { r_mobile.error = "Mobile number is required"; return@setOnClickListener }
            if (email.isEmpty()) { r_email.error = "Email is required"; return@setOnClickListener }
            if (pass.isEmpty()) { r_password.error = "Password is required"; return@setOnClickListener }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { r_email.error = "Invalid email"; return@setOnClickListener }
            if (pass.length < 6) { r_password.error = "Password must be at least 6 characters"; return@setOnClickListener }
            if (pass != confirm) { r_confirm.error = "Passwords don't match"; return@setOnClickListener }
            if (!r_agree.isChecked) { Toast.makeText(this, "Agree to Terms & Privacy", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (emerName.isEmpty()) { r_emerName.error = "Emergency contact name is required"; return@setOnClickListener }
            if (emerNo.isEmpty()) { r_emerNo.error = "Emergency contact number is required"; return@setOnClickListener }
            if (dob.isEmpty()) { r_dob.error = "Date of birth is required"; return@setOnClickListener }
            if (nationality.isEmpty()) { r_nationality.error = "Nationality is required"; return@setOnClickListener }
            if (address.isEmpty()) { r_address.error = "Address is required"; return@setOnClickListener }

            // Show a simple progress (optional)
            btnRegister.isEnabled = false

            // 1) Create Auth user
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser

                        val uid = firebaseUser?.uid ?: run {
                            Log.e(TAG, "Registration: created user but uid null")
                            Toast.makeText(this, "Registration failed: uid missing", Toast.LENGTH_LONG).show()
                            btnRegister.isEnabled = true
                            return@addOnCompleteListener
                        }

                        Log.d(TAG, "User created: $uid")
                        Toast.makeText(this, "User registered!", Toast.LENGTH_SHORT).show()

                        // 2) Prepare user document and write to Firestore using UID as doc id
                        val userDoc = hashMapOf(
                            "fullName" to fullName,
                            "gender" to gender,
                            "mobile" to mobile,
                            "email" to email,
                            "emerName" to emerName,
                            "emerNo" to emerNo,
                            "dob" to dob,
                            "nationality" to nationality,
                            "address" to address,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users")
                            .document(uid) // use uid so it's easy to secure and query
                            .set(userDoc)
                            .addOnSuccessListener {

                                Log.d(TAG, "User document written for uid=$uid")
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()

                                // successful registration flow
                                SessionManager(this).saveLoginState(true)
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error writing user doc: ${e.message}", e)
                                Toast.makeText(this, "Registration saved failed: ${e.message}", Toast.LENGTH_LONG).show()
                                btnRegister.isEnabled = true
                            }
                    } else {
                        Log.e(TAG, "Error creating user: ${task.exception?.message}")
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        btnRegister.isEnabled = true
                    }
                }
        }
    }

    private fun showSignIn() {

            signinCard.visibility = View.VISIBLE
            registerCard.visibility = View.GONE

            underlineSignIn.visibility = View.VISIBLE
            underlineRegister.visibility = View.INVISIBLE

            btnSignInTab.setTextColor(getColor(R.color.purple_700))
            btnRegisterTab.setTextColor(getColor(R.color.gray_500))

    }

    private fun showRegister() {
        signinCard.visibility = View.GONE
        registerCard.visibility = View.VISIBLE

        underlineRegister.visibility = View.VISIBLE
        underlineSignIn.visibility = View.INVISIBLE

        btnRegisterTab.setTextColor(getColor(R.color.purple_700))
        btnSignInTab.setTextColor(getColor(R.color.gray_500))
    }
}
