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
import com.krishisakhi.farmassistant.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
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

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvLocation = findViewById<TextView>(R.id.tvLocation)
        val tvLandSize = findViewById<TextView>(R.id.tvLandSize)
        val tvSoilType = findViewById<TextView>(R.id.tvSoilType)
        val tvPrimaryCrops = findViewById<TextView>(R.id.tvPrimaryCrops)
        val tvLanguage = findViewById<TextView>(R.id.tvLanguage)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)

        val btnLogout = Button(this).apply {
            text = "Logout"
            layoutParams = btnEditProfile.layoutParams
        }

        // add logout button below edit profile
        (btnEditProfile.parent as? android.view.ViewGroup)?.addView(btnLogout)

        val auth = FirebaseAuth.getInstance()
        val phoneRaw = auth.currentUser?.phoneNumber

        // Load profile from Room and populate UI
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val normalizedPhone = normalizePhoneToDigits(phoneRaw)
                val profile = withContext(Dispatchers.IO) {
                    if (normalizedPhone == null) null else db.farmerProfileDao().getProfileByPhone(normalizedPhone)
                }

                if (profile != null) {
                    tvName.text = profile.name ?: "-"
                    tvLocation.text = listOfNotNull(profile.state, profile.district, profile.village).filter { it.isNotBlank() }.joinToString(", ")
                    tvLandSize.text = "Land: ${profile.totalLandSize}"
                    tvSoilType.text = "Soil: ${profile.soilType ?: "-"}"
                    tvPrimaryCrops.text = "Crops: ${profile.primaryCrops?.joinToString(", ") ?: "-"}"
                    tvLanguage.text = "Language: ${profile.languagePreference ?: "-"}"
                } else {
                    // show fallback
                    tvName.text = phoneRaw ?: "Guest"
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

        btnEditProfile.setOnClickListener {
            // Open registration form where user can edit (reuse RegistrationActivity)
            startActivity(Intent(this, RegistrationActivity::class.java))
        }

        btnLogout.setOnClickListener {
            // Sign out and clear registration flag
            auth.signOut()
            val prefs = getSharedPreferences("farm_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("is_registered", false).apply()

            val intent = Intent(this, PhoneAuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
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