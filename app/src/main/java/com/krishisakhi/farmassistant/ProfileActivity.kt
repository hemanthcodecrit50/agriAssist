package com.krishisakhi.farmassistant

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.data.FarmerProfile
import com.krishisakhi.farmassistant.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvLandSize: TextView
    private lateinit var tvSoilType: TextView
    private lateinit var tvPrimaryCrops: TextView
    private lateinit var tvLanguage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Use the activity's content view so we don't depend on a specific id in the layout.
        val rootView = findViewById<View>(android.R.id.content)
        if (rootView == null) {
            Log.w("ProfileActivity", "Could not find activity content view to apply window insets")
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvName = findViewById(R.id.tvName)
        tvLocation = findViewById(R.id.tvLocation)
        tvLandSize = findViewById(R.id.tvLandSize)
        tvSoilType = findViewById(R.id.tvSoilType)
        tvPrimaryCrops = findViewById(R.id.tvPrimaryCrops)
        tvLanguage = findViewById(R.id.tvLanguage)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Load profile on create
        loadAndDisplayProfile()

        btnEditProfile.setOnClickListener {
            // When user taps Edit Profile, fetch latest profile from DB then open RegistrationActivity with prefilled values
            lifecycleScope.launch {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.currentUser?.uid
                    val db = AppDatabase.getDatabase(applicationContext)
                    val profile = if (uid != null) withContext(Dispatchers.IO) { db.farmerProfileDao().getProfileByUid(uid) } else null

                    val intent = Intent(this@ProfileActivity, RegistrationActivity::class.java)
                    if (profile != null) {
                        intent.putExtra("prefill_name", profile.name)
                        intent.putExtra("prefill_state", profile.state)
                        intent.putExtra("prefill_district", profile.district)
                        intent.putExtra("prefill_village", profile.village)
                        intent.putExtra("prefill_landSize", profile.totalLandSize)
                        intent.putExtra("prefill_soilType", profile.soilType)
                        // primaryCrops may have 1..N entries; map to two fields if available
                        if (!profile.primaryCrops.isNullOrEmpty()) intent.putExtra("prefill_crop1", profile.primaryCrops.getOrNull(0))
                        if (profile.primaryCrops.size > 1) intent.putExtra("prefill_crop2", profile.primaryCrops.getOrNull(1))
                        intent.putExtra("prefill_language", profile.languagePreference)
                        intent.putExtra("prefill_phone", profile.phoneNumber)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Failed to open RegistrationActivity with prefill", e)
                    startActivity(Intent(this@ProfileActivity, RegistrationActivity::class.java))
                }
            }
        }

        btnLogout.setOnClickListener {
            // Sync profile to Firestore before logout
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid

            lifecycleScope.launch {
                if (uid != null) {
                    // Sync profile on logout to ensure Firestore is up-to-date
                    val syncManager = com.krishisakhi.farmassistant.sync.SyncManager.getInstance(applicationContext)
                    syncManager.syncOnLogout(uid)
                }

                // Sign out
                auth.signOut()

                // Clear registration flag
                val prefs = getSharedPreferences("farm_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("is_registered", false).apply()

                val intent = Intent(this@ProfileActivity, PhoneAuthActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload profile when returning from edit (RegistrationActivity)
        loadAndDisplayProfile()
    }

    private fun loadAndDisplayProfile() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val profile = withContext(Dispatchers.IO) {
                    if (uid == null) null else db.farmerProfileDao().getProfileByUid(uid)
                }

                if (profile != null) {
                    tvName.text = profile.name.ifEmpty { "-" }
                    tvLocation.text = listOfNotNull(profile.state, profile.district, profile.village).filter { it.isNotBlank() }.joinToString(", ")
                    tvLandSize.text = "Land: ${profile.totalLandSize}"
                    tvSoilType.text = "Soil: ${profile.soilType.ifEmpty { "-" }}"
                    tvPrimaryCrops.text = "Crops: ${profile.primaryCrops.joinToString(", ").ifEmpty { "-" }}"
                    tvLanguage.text = "Language: ${profile.languagePreference.ifEmpty { "-" }}"
                } else {
                    // show fallback
                    tvName.text = "Guest"
                    tvLocation.text = "-"
                    tvLandSize.text = "-"
                    tvSoilType.text = "-"
                    tvPrimaryCrops.text = "-"
                    tvLanguage.text = "-"
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Failed to load profile", e)
            }
        }
    }
}