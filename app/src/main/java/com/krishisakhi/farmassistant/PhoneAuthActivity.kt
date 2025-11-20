package com.krishisakhi.farmassistant

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.krishisakhi.farmassistant.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var phoneNumberEditText: EditText
    private lateinit var sendOtpButton: Button
    private var verificationId: String? = null

    companion object {
        private const val TAG = "PhoneAuthActivity"
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification completed
            Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
            Toast.makeText(this@PhoneAuthActivity, "Auto-verification successful!", Toast.LENGTH_SHORT).show()
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "onVerificationFailed: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error cause: ${e.cause}")

            sendOtpButton.isEnabled = true
            sendOtpButton.text = "Send OTP"

            val errorMessage = when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    "Invalid phone number format. Please check and try again."
                }
                is FirebaseTooManyRequestsException -> {
                    "Too many requests. SMS quota exceeded. Please try again later or use test phone numbers."
                }
                else -> {
                    val detailedError = e.message ?: "Unknown error"
                    "Verification failed: $detailedError\n\n" +
                    "Common causes:\n" +
                    "• SHA-1 not added to Firebase\n" +
                    "• Phone auth not enabled in Firebase Console\n" +
                    "• Google Play Services outdated\n" +
                    "• App signature mismatch\n\n" +
                    "Use test phone numbers for development."
                }
            }

            Toast.makeText(
                this@PhoneAuthActivity,
                errorMessage,
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // OTP sent successfully
            Log.d(TAG, "onCodeSent: verificationId=$verificationId")
            this@PhoneAuthActivity.verificationId = verificationId

            sendOtpButton.isEnabled = true
            sendOtpButton.text = "Send OTP"

            Toast.makeText(
                this@PhoneAuthActivity,
                "OTP sent successfully! Check your SMS.",
                Toast.LENGTH_LONG
            ).show()

            // Navigate to OTP verification screen
            val intent = Intent(this@PhoneAuthActivity, OtpVerificationActivity::class.java)
            intent.putExtra("verificationId", verificationId)
            intent.putExtra("phoneNumber", phoneNumberEditText.text.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            // Try to load authenticated choices layout by resource name. This avoids static R references when
            // resource index may not be up-to-date for the static analyzer in this tool environment.
            val layoutId = resources.getIdentifier("activity_authenticated_choices", "layout", packageName)
            if (layoutId != 0) {
                setContentView(layoutId)

                val tvId = resources.getIdentifier("tvSignedInAs", "id", packageName)
                val btnContinueId = resources.getIdentifier("btnContinue", "id", packageName)
                val btnRegisterId = resources.getIdentifier("btnRegister", "id", packageName)
                val btnProfileId = resources.getIdentifier("btnProfile", "id", packageName)

                val tvSignedInAs = if (tvId != 0) findViewById<TextView>(tvId) else null
                val btnContinue = if (btnContinueId != 0) findViewById<Button>(btnContinueId) else null
                val btnRegister = if (btnRegisterId != 0) findViewById<Button>(btnRegisterId) else null
                val btnProfile = if (btnProfileId != 0) findViewById<Button>(btnProfileId) else null

                val phoneRaw = auth.currentUser?.phoneNumber ?: "Unknown"
                val name = auth.currentUser?.displayName ?: "Unknown"
                tvSignedInAs?.text = "Signed in as $name"

                btnRegister?.setOnClickListener {
                    val intent = Intent(this, RegistrationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }

                btnProfile?.setOnClickListener {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }

                btnContinue?.setOnClickListener {
                    it.isEnabled = false
                    (it as? Button)?.text = "Checking..."
                    lifecycleScope.launch {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val normalized = normalizePhoneToDigits(phoneRaw)
                        val profile = withContext(Dispatchers.IO) {
                            if (normalized.isNullOrBlank()) null else db.farmerProfileDao().getProfileByPhone(normalized)
                        }

                        val intent = if (profile != null) {
                            Intent(this@PhoneAuthActivity, MainActivity::class.java)
                        } else {
                            Intent(this@PhoneAuthActivity, RegistrationActivity::class.java)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                }

                return
            } else {
                // If layout not found for some reason, fall back to normal auth screen
                Log.w(TAG, "Authenticated choices layout not found; falling back to phone auth layout")
            }
        }

        setContentView(R.layout.activity_phone_auth)

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        sendOtpButton = findViewById(R.id.sendOtpButton)

        sendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                phoneNumberEditText.error = "Please enter phone number"
                return@setOnClickListener
            }

            if (phoneNumber.length != 10) {
                phoneNumberEditText.error = "Please enter valid 10-digit phone number"
                return@setOnClickListener
            }

            sendOtpButton.isEnabled = false
            sendOtpButton.text = "Sending OTP..."

            Toast.makeText(
                this,
                "Please wait... Verifying phone number",
                Toast.LENGTH_SHORT
            ).show()

            sendVerificationCode("+91$phoneNumber")
        }

        // Add info text about test numbers
        val infoText = findViewById<TextView>(R.id.testNumberInfo)
        infoText?.text = "For testing: Add test phone numbers in Firebase Console"
    }

    private fun sendVerificationCode(phoneNumber: String) {
        Log.d(TAG, "Sending verification code to: $phoneNumber")
        Toast.makeText(this, "Sending OTP to $phoneNumber...", Toast.LENGTH_SHORT).show()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()

                    // After sign-in, check Room DB for profile and navigate accordingly
                    val phoneRaw = auth.currentUser?.phoneNumber
                    lifecycleScope.launch {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val normalized = normalizePhoneToDigits(phoneRaw)
                        val profile = withContext(Dispatchers.IO) {
                            if (normalized.isNullOrBlank()) null else db.farmerProfileDao().getProfileByPhone(normalized)
                        }

                        val intent = if (profile != null) {
                            Intent(this@PhoneAuthActivity, MainActivity::class.java)
                        } else {
                            Intent(this@PhoneAuthActivity, RegistrationActivity::class.java)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Sign in failed
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
