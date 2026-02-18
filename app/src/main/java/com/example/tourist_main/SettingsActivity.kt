package com.example.tourist_main

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SettingsActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private var selectedImageUri: Uri? = null

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText

    private lateinit var etPrimaryName: EditText
    private lateinit var etPrimaryPhone: EditText
    private lateinit var spPrimaryRelation: Spinner

    private lateinit var etSecondaryName: EditText
    private lateinit var etSecondaryPhone: EditText
    private lateinit var spSecondaryRelation: Spinner

    private lateinit var spLocationSharing: Spinner
    private lateinit var swRealtimeTracking: Switch
    private lateinit var spPrecision: Spinner

    private lateinit var etBloodType: EditText
    private lateinit var etAllergies: EditText
    private lateinit var etMedicalConditions: EditText
    private lateinit var etMedications: EditText

    private lateinit var swConsentLocation: Switch
    private lateinit var swConsentKyc: Switch
    private lateinit var spLanguage: Spinner

    private lateinit var btnSaveTop: Button
    private lateinit var btnSaveBottom: Button
    private lateinit var btnCancel: Button


    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                ivProfile.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        ivProfile = findViewById(R.id.iv_profile)

        ivProfile.setOnClickListener {
            pickImage.launch("image/*")
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Bind UI
        etName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)

        etPrimaryName = findViewById(R.id.et_primary_name)
        etPrimaryPhone = findViewById(R.id.et_primary_phone)
        spPrimaryRelation = findViewById(R.id.sp_primary_relation)

        etSecondaryName = findViewById(R.id.et_secondary_name)
        etSecondaryPhone = findViewById(R.id.et_secondary_phone)
        spSecondaryRelation = findViewById(R.id.sp_secondary_relation)

        spLocationSharing = findViewById(R.id.sp_location_sharing)
        swRealtimeTracking = findViewById(R.id.sw_realtime_tracking)
        spPrecision = findViewById(R.id.sp_precision)

        etBloodType = findViewById(R.id.et_blood_type)
        etAllergies = findViewById(R.id.et_allergies)
        etMedicalConditions = findViewById(R.id.et_medical_conditions)
        etMedications = findViewById(R.id.et_medications)

        swConsentLocation = findViewById(R.id.sw_consent_location)
        swConsentKyc = findViewById(R.id.sw_consent_kyc)
        spLanguage = findViewById(R.id.sp_language)

        btnSaveTop = findViewById(R.id.btn_save_top)
        btnSaveBottom = findViewById(R.id.btn_save_bottom)
        btnCancel = findViewById(R.id.btn_cancel)

        btnSaveTop.setOnClickListener { saveSettings() }
        btnSaveBottom.setOnClickListener { saveSettings() }
        btnCancel.setOnClickListener { finish() }
    }

    private suspend fun uploadImageToSupabase(
        context: Context,
        imageUri: Uri
    ): String? = withContext(Dispatchers.IO) {

        try {

            runOnUiThread {
                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            }

            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()

            if (bytes == null) {
                runOnUiThread {
                    Toast.makeText(context, "Image read failed", Toast.LENGTH_SHORT).show()
                }
                return@withContext null
            }

            val fileName = "${UUID.randomUUID()}.jpg"

            SupabaseManager.client.storage
                .from("profileimg")  // 🔴 Make sure this bucket exists
                .upload(fileName, bytes)

            val publicUrl = SupabaseManager.client.storage
                .from("profileimg")
                .publicUrl(fileName)

            runOnUiThread {
                Toast.makeText(context, "Image Uploaded Successfully", Toast.LENGTH_SHORT).show()
            }

            return@withContext publicUrl

        } catch (e: Exception) {

            e.printStackTrace()

            runOnUiThread {
                Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }

            null
        }
    }

    private fun saveSettings() {

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        lifecycleScope.launch {

            val updates = HashMap<String, Any>()

            // Profile Image
            if (selectedImageUri != null) {
                val imageUrl = uploadImageToSupabase(this@SettingsActivity, selectedImageUri!!)
                if (imageUrl != null) {
                    updates["profileImage"] = imageUrl
                }
            }

            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) updates["fullName"] = name

            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) updates["email"] = email

            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty()) updates["mobile"] = phone

            val name1 = etPrimaryName.text.toString().trim()
            if (name1.isNotEmpty()) updates["name1"] = name1

            val phone1 = etPrimaryPhone.text.toString().trim()
            if (phone1.isNotEmpty()) updates["phone1"] = phone1

            val relation1 = spPrimaryRelation.selectedItem?.toString()
            if (!relation1.isNullOrEmpty()) updates["relation1"] = relation1

            val name2 = etSecondaryName.text.toString().trim()
            if (name2.isNotEmpty()) updates["name2"] = name2

            val phone2 = etSecondaryPhone.text.toString().trim()
            if (phone2.isNotEmpty()) updates["phone2"] = phone2

            val relation2 = spSecondaryRelation.selectedItem?.toString()
            if (!relation2.isNullOrEmpty()) updates["relation2"] = relation2

            val sharing = spLocationSharing.selectedItem?.toString()
            if (!sharing.isNullOrEmpty())
                updates["locationSettings.sharing"] = sharing

            updates["locationSettings.realtimeTracking"] = swRealtimeTracking.isChecked

            val precision = spPrecision.selectedItem?.toString()
            if (!precision.isNullOrEmpty())
                updates["locationSettings.precision"] = precision

            val blood = etBloodType.text.toString().trim()
            if (blood.isNotEmpty()) updates["bloodType"] = blood

            val allergies = etAllergies.text.toString().trim()
            if (allergies.isNotEmpty()) updates["allergies"] = allergies

            val conditions = etMedicalConditions.text.toString().trim()
            if (conditions.isNotEmpty()) updates["conditions"] = conditions

            val meds = etMedications.text.toString().trim()
            if (meds.isNotEmpty()) updates["medications"] = meds

            updates["privacy.consentLocation"] = swConsentLocation.isChecked
            updates["privacy.consentKyc"] = swConsentKyc.isChecked

            val language = spLanguage.selectedItem?.toString()
            if (!language.isNullOrEmpty())
                updates["language"] = language

            if (updates.isEmpty()) {
                Toast.makeText(this@SettingsActivity, "No changes made", Toast.LENGTH_SHORT).show()
                return@launch
            }

            db.collection("users")
                .document(user.uid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this@SettingsActivity, "Settings Updated Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    Toast.makeText(this@SettingsActivity, e.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}
