package com.example.tourist_main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        val btnSave = findViewById<Button>(R.id.btn_save_top)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)
        btnSave.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
        }
        btnCancel.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }




    }

}
