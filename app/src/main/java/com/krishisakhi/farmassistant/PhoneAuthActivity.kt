package com.krishisakhi.farmassistant

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
            // User is already authenticated, go directly to MainActivity
            Log.d(TAG, "User already logged in: ${auth.currentUser?.phoneNumber}")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
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
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
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
}

