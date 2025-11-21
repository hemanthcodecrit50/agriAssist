package com.krishisakhi.farmassistant

import android.content.Intent
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
import com.krishisakhi.farmassistant.rag.EmbeddingService
import com.krishisakhi.farmassistant.rag.VectorDatabasePersistent
import com.krishisakhi.farmassistant.utils.FarmerProfileSerializer
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

        // Prefill fields if intent contains extras from ProfileActivity
        intent?.let { i ->
            if (i.hasExtra("prefill_name")) etName.setText(i.getStringExtra("prefill_name"))
            if (i.hasExtra("prefill_state")) etState.setText(i.getStringExtra("prefill_state"))
            if (i.hasExtra("prefill_district")) etDistrict.setText(i.getStringExtra("prefill_district"))
            if (i.hasExtra("prefill_village")) etVillage.setText(i.getStringExtra("prefill_village"))
            if (i.hasExtra("prefill_landSize")) etLandSize.setText(i.getDoubleExtra("prefill_landSize", 0.0).toString())
            if (i.hasExtra("prefill_soilType")) etSoilType.setText(i.getStringExtra("prefill_soilType"))
            if (i.hasExtra("prefill_crop1")) etCrop1.setText(i.getStringExtra("prefill_crop1"))
            if (i.hasExtra("prefill_crop2")) etCrop2.setText(i.getStringExtra("prefill_crop2"))
            if (i.hasExtra("prefill_language")) {
                val lang = i.getStringExtra("prefill_language")
                val idx = when (lang) {
                    "English" -> 0
                    "Hindi" -> 1
                    else -> 2
                }
                spinnerLanguage.setSelection(idx)
            }
        }

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
        val uid = currentUser?.uid
        val phoneNumber = currentUser?.phoneNumber

        Log.d("RegistrationActivity", "User UID: $uid")
        Log.d("RegistrationActivity", "Raw phone from auth: $phoneNumber")

        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Could not get user UID from authenticated user.", Toast.LENGTH_LONG).show()
            btnSubmit.isEnabled = true
            return
        }

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Could not get phone number from authenticated user.", Toast.LENGTH_LONG).show()
            btnSubmit.isEnabled = true
            return
        }

        // Normalize phone to digits-only form
        val normalized = normalizePhoneToDigits(phoneNumber)
        Log.d("RegistrationActivity", "Normalized phone: $normalized")
        if (normalized == null) {
            Toast.makeText(this, "Error: Could not normalize phone number.", Toast.LENGTH_LONG).show()
            btnSubmit.isEnabled = true
            return
        }

        val currentTime = System.currentTimeMillis()
        val profile = FarmerProfile(
            uid = uid,
            phoneNumber = normalized,
            name = etName.text.toString().trim(),
            state = etState.text.toString().trim(),
            district = etDistrict.text.toString().trim(),
            village = etVillage.text.toString().trim(),
            totalLandSize = etLandSize.text.toString().trim().toDouble(),
            soilType = etSoilType.text.toString().trim(),
            primaryCrops = listOf(etCrop1.text.toString().trim(), etCrop2.text.toString().trim()),
            languagePreference = spinnerLanguage.selectedItem.toString(),
            lastUpdated = currentTime,
            createdAt = currentTime
        )

        lifecycleScope.launch {
            try {
                // Use SyncManager to save profile to both Room and Firestore
                val syncManager = com.krishisakhi.farmassistant.sync.SyncManager.getInstance(this@RegistrationActivity)
                val success = syncManager.syncOnRegistration(profile)

                if (!success) {
                    btnSubmit.isEnabled = true
                    Toast.makeText(this@RegistrationActivity, "Failed to save profile", Toast.LENGTH_LONG).show()
                    return@launch
                }

                Log.d("RegistrationActivity", "Profile saved to Room and Firestore successfully")

                // 2. Convert profile to natural language text using FarmerProfileSerializer
                val profileText = FarmerProfileSerializer.toEmbeddingSummary(profile)
                Log.d("RegistrationActivity", "Profile serialized to text (${profileText.length} chars)")

                // 3. Generate embedding using EmbeddingService
                val embeddingService = EmbeddingService(this@RegistrationActivity)
                val embedding = withContext(Dispatchers.IO) {
                    embeddingService.generateEmbedding(profileText)
                }

                if (embedding != null) {
                    Log.d("RegistrationActivity", "Embedding generated (${embedding.size} dimensions)")

                    // 4. Store vector in persistent Vector DB
                    val vectorDb = VectorDatabasePersistent(this@RegistrationActivity)
                    val vectorId = "farmer_profile_${uid}"

                    val metadata = mapOf(
                        "type" to "farmer_profile",
                        "uid" to uid,
                        "phoneNumber" to normalized,
                        "name" to profile.name,
                        "state" to profile.state,
                        "district" to profile.district,
                        "village" to profile.village,
                        "landSize" to profile.totalLandSize.toString(),
                        "soilType" to profile.soilType,
                        "crops" to profile.primaryCrops.joinToString(","),
                        "language" to profile.languagePreference,
                        "timestamp" to System.currentTimeMillis().toString()
                    )

                    val vectorInserted = withContext(Dispatchers.IO) {
                        vectorDb.insertVector(
                            id = vectorId,
                            embedding = embedding,
                            metadata = metadata,
                            farmerId = uid
                        )
                    }

                    if (vectorInserted) {
                        Log.d("RegistrationActivity", "Farmer profile vector created successfully: $vectorId")
                    } else {
                        Log.w("RegistrationActivity", "Failed to insert farmer profile vector")
                    }
                } else {
                    Log.e("RegistrationActivity", "Failed to generate embedding for profile")
                }

                // Mark as registered
                val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
                prefs.edit().putBoolean(registeredKey, true).apply()

                // Log all profiles to aid debugging
                val all = withContext(Dispatchers.IO) { db.farmerProfileDao().getAllProfiles() }
                Log.d("RegistrationActivity", "All saved profiles: $all")

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
