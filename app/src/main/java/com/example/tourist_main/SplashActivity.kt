package com.example.tourist_main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)
        if (session.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java)) // your Home activity
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
