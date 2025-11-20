package com.krishisakhi.farmassistant

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.data.FarmerProfile
import com.krishisakhi.farmassistant.db.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class RegistrationActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etState: EditText
    private lateinit var etDistrict: EditText
    private lateinit var etVillage: EditText
    private lateinit var etLandSize: EditText
    private lateinit var etSoilType: EditText
    private lateinit var etCrop1: EditText
    private lateinit var etCrop2: EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSubmit: Button

    private val prefsName = "farm_prefs"
    private val registeredKey = "is_registered"

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        etName = findViewById(R.id.etName)
        etState = findViewById(R.id.etState)
        etDistrict = findViewById(R.id.etDistrict)
        etVillage = findViewById(R.id.etVillage)
        etLandSize = findViewById(R.id.etLandSize)
        etSoilType = findViewById(R.id.etSoilType)
        etCrop1 = findViewById(R.id.etCrop1)
        etCrop2 = findViewById(R.id.etCrop2)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        btnSubmit = findViewById(R.id.btnSubmit)

        spinnerLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("English", "Hindi", "Local")
        )

        btnSubmit.setOnClickListener {
            if (validateInputs()) saveToRoomDatabase()
        }
    }

    private fun validateInputs(): Boolean {
        val name = etName.text.toString().trim()
        val state = etState.text.toString().trim()
        val district = etDistrict.text.toString().trim()
        val village = etVillage.text.toString().trim()
        val landStr = etLandSize.text.toString().trim()
        val crop1 = etCrop1.text.toString().trim()
        val crop2 = etCrop2.text.toString().trim()

        if (name.isEmpty()) { etName.error = "Name required"; etName.requestFocus(); return false }
        if (state.isEmpty()) { etState.error = "State required"; etState.requestFocus(); return false }
        if (district.isEmpty()) { etDistrict.error = "District required"; etDistrict.requestFocus(); return false }
        if (village.isEmpty()) { etVillage.error = "Village required"; etVillage.requestFocus(); return false }
        if (landStr.isEmpty()) { etLandSize.error = "Land size required"; etLandSize.requestFocus(); return false }
        val land = landStr.toDoubleOrNull()
        if (land == null || land <= 0.0) { etLandSize.error = "Enter valid land size"; etLandSize.requestFocus(); return false }
        if (etSoilType.text.toString().trim().isEmpty()) { etSoilType.error = "Soil type required"; etSoilType.requestFocus(); return false }
        if (crop1.isEmpty() || crop2.isEmpty()) { etCrop1.error = "Enter two primary crops"; etCrop1.requestFocus(); return false }

        return true
    }

    private fun saveToRoomDatabase() {
        btnSubmit.isEnabled = false

        val currentUser = auth.currentUser
        var phoneNumber = currentUser?.phoneNumber
        Log.d("RegistrationActivity", "Raw phone from auth: $phoneNumber")
        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Could not get phone number from authenticated user.", Toast.LENGTH_LONG).show()
            btnSubmit.isEnabled = true
            return
        }

        // Normalize phone to digits-only form used as DB key
        val normalized = normalizePhoneToDigits(phoneNumber)
        Log.d("RegistrationActivity", "Normalized phone: $normalized")
        if (normalized == null) {
            Toast.makeText(this, "Error: Could not normalize phone number.", Toast.LENGTH_LONG).show()
            btnSubmit.isEnabled = true
            return
        }

        val profile = FarmerProfile(
            phoneNumber = normalized,
            name = etName.text.toString().trim(),
            state = etState.text.toString().trim(),
            district = etDistrict.text.toString().trim(),
            village = etVillage.text.toString().trim(),
            totalLandSize = etLandSize.text.toString().trim().toDouble(),
            soilType = etSoilType.text.toString().trim(),
            primaryCrops = listOf(etCrop1.text.toString().trim(), etCrop2.text.toString().trim()),
            languagePreference = spinnerLanguage.selectedItem.toString()
        )

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.farmerProfileDao().insertProfile(profile)
                }

                // mark registered
                val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
                prefs.edit().putBoolean(registeredKey, true).apply()

                // Log all profiles to aid debugging
                lifecycleScope.launch {
                    val all = withContext(Dispatchers.IO) { db.farmerProfileDao().getAllProfiles() }
                    Log.d("RegistrationActivity", "All saved profiles: $all")
                }

                Toast.makeText(this@RegistrationActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()

                // Go to MainActivity (clear backstack) so user lands on the main screen
                val intent = Intent(this@RegistrationActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                btnSubmit.isEnabled = true
                Toast.makeText(this@RegistrationActivity, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RegistrationActivity", "Save failed", e)
            }
        }
    }

    // Normalize phone to digits-only string. If 10 digits are provided it prefixes '91'. Returns null if not enough digits.
    private fun normalizePhoneToDigits(phone: String?): String? {
        if (phone == null) return null
        val digits = phone.filter { it.isDigit() }
        if (digits.length == 10) return "91$digits"
        if (digits.length >= 11) return digits // assume already includes country code
        return null
    }

}
