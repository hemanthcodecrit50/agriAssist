// In: RegistrationActivity.kt
package com.krishisakhi.farmassistant

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
// ... other imports
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.db.AppDatabase
import com.krishisakhi.farmassistant.data.FarmerProfile
import kotlinx.coroutines.launch // Import launch

class RegistrationActivity : AppCompatActivity() {
    val name = findViewById<EditText>(R.id.etName)
    val state = findViewById<EditText>(R.id.etState)
    val district = findViewById<EditText>(R.id.etDistrict)
    val village = findViewById<EditText>(R.id.etVillage)
    val landStr = findViewById<EditText>(R.id.etLandSize)
    val name = findViewById<EditText>(R.id.etName)
    val name = findViewById<EditText>(R.id.etName)

    private val prefsName = "farm_prefs"
    private val registeredKey = "is_registered"

    // Get instances of Firebase Auth and your Room Database
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { AppDatabase.getDatabase(this) } // Database instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // ... (findViewById calls remain the same) ...

        spinnerLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("English", "Hindi", "Local")
        )

        btnSubmit.setOnClickListener {
            // We'll now save to the Room database
            if (validate()) saveToRoomDatabase()
        }
    }

    private fun validate(): Boolean {
        // ... (This function remains unchanged) ...
        val name = name.text.toString().trim()
        val state = etState.text.toString().trim()
        val district = etDistrict.text.toString().trim()
        val village = etVillage.text.toString().trim()
        val landStr = etLandSize.text.toString().trim()
        val crop1 = etCrop1.text.toString().trim()
        val crop2 = etCrop2.text.toString().trim()

        if (name.isEmpty()) { showToast("Name required"); return false }
        if (state.isEmpty()) { showToast("State required"); return false }
        if (district.isEmpty()) { showToast("District required"); return false }
        if (village.isEmpty()) { showToast("Village required"); return false }
        if (landStr.isEmpty()) { showToast("Land size required"); return false }
        val land = landStr.toDoubleOrNull()
        if (land == null || land <= 0.0) { showToast("Enter valid land size"); return false }
        if (etSoilType.text.toString().trim().isEmpty()) { showToast("Soil type required"); return false }
        if (crop1.isEmpty() || crop2.isEmpty()) { showToast("Enter at least two primary crops"); return false }
        return true
    }

    private fun saveToRoomDatabase() {
        btnSubmit.isEnabled = false
        val currentUser = auth.currentUser

        // Get the phone number, it's crucial for the primary key
        val phoneNumber = currentUser?.phoneNumber
        if (phoneNumber.isNullOrEmpty()) {
            showToast("Error: Could not get phone number.")
            btnSubmit.isEnabled = true
            return
        }

        val profile = FarmerProfile(
            phoneNumber = phoneNumber, // Use phone number as the key
            name = etName.text.toString().trim(),
            state = etState.text.toString().trim(),
            district = etDistrict.text.toString().trim(),
            village = etVillage.text.toString().trim(),
            totalLandSize = etLandSize.text.toString().trim().toDouble(),
            soilType = etSoilType.text.toString().trim(),
            primaryCrops = listOf(etCrop1.text.toString().trim(), etCrop2.text.toString().trim()),
            languagePreference = spinnerLanguage.selectedItem.toString()
        )

        // Use a coroutine to save data on a background thread
        lifecycleScope.launch {
            try {
                db.farmerProfileDao().insertProfile(profile)

                // mark registered in SharedPreferences
                val prefs: SharedPreferences = getSharedPreferences(prefsName, MODE_PRIVATE)
                prefs.edit().putBoolean(registeredKey, true).apply()

                showToast("Profile saved successfully")
                startActivity(Intent(this@RegistrationActivity, ProfileActivity::class.java))
                finish()
            } catch (e: Exception) {
                btnSubmit.isEnabled = true
                showToast("Failed to save profile: ${e.message}")
            }
        }
    }

    // This is how you would fetch a farmer's details by their phone number
    private fun fetchFarmerProfile(phoneNumber: String) {
        lifecycleScope.launch {
            val farmer = db.farmerProfileDao().getProfileByPhone(phoneNumber)
            if (farmer != null) {
                // You have the farmer's details!
                // You can now populate UI fields or pass this object to another activity
                etName.setText(farmer.name)
                etState.setText(farmer.state)
                // ... and so on for other fields
                showToast("Fetched details for ${farmer.name}")
            } else {
                showToast("No profile found for this number.")
            }
        }
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

