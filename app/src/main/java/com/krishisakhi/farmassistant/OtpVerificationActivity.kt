package com.krishisakhi.farmassistant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var otpEditText: EditText
    private lateinit var verifyOtpButton: Button
    private lateinit var phoneNumberTextView: TextView
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()

        otpEditText = findViewById(R.id.otpEditText)
        verifyOtpButton = findViewById(R.id.verifyOtpButton)
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView)

        verificationId = intent.getStringExtra("verificationId")
        val phoneNumber = intent.getStringExtra("phoneNumber")

        phoneNumberTextView.text = "OTP sent to +91$phoneNumber"

        verifyOtpButton.setOnClickListener {
            val otp = otpEditText.text.toString().trim()

            if (otp.isEmpty()) {
                otpEditText.error = "Please enter OTP"
                return@setOnClickListener
            }

            if (otp.length != 6) {
                otpEditText.error = "Please enter valid 6-digit OTP"
                return@setOnClickListener
            }

            verifyOtpButton.isEnabled = false
            verifyCode(otp)
        }
    }

    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show()

                    // After successful Firebase Auth, check Firestore for user profile
                    val currentUser = auth.currentUser
                    val uid = currentUser?.uid

                    if (uid != null) {
                        checkFirestoreProfileAndNavigate(uid)
                    } else {
                        Toast.makeText(this, "Error: Could not get user UID", Toast.LENGTH_LONG).show()
                        verifyOtpButton.isEnabled = true
                    }
                } else {
                    // Sign in failed
                    Toast.makeText(
                        this,
                        "Invalid OTP: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    verifyOtpButton.isEnabled = true
                }
            }
    }

    private fun checkFirestoreProfileAndNavigate(uid: String) {
        // Use SyncManager to check if profile exists in Firestore
        lifecycleScope.launch {
            try {
                val syncManager = com.krishisakhi.farmassistant.sync.SyncManager.getInstance(this@OtpVerificationActivity)

                // Check if profile exists in Firestore
                val profileExists = syncManager.profileExistsInFirestore(uid)

                if (profileExists) {
                    // Profile exists in Firestore - sync to Room and go to MainActivity
                    val profile = syncManager.syncOnLogin(uid)
                    if (profile != null) {
                        Toast.makeText(this@OtpVerificationActivity, "Welcome back, ${profile.name}!", Toast.LENGTH_SHORT).show()
                    }
                    val intent = Intent(this@OtpVerificationActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    // Profile doesn't exist - go to RegistrationActivity
                    val intent = Intent(this@OtpVerificationActivity, RegistrationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@OtpVerificationActivity,
                    "Error checking profile: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                verifyOtpButton.isEnabled = true
            }
        }
    }
}
