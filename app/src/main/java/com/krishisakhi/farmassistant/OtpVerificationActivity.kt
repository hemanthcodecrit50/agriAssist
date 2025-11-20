package com.krishisakhi.farmassistant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

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

                    // After successful auth decide where to go: profile registration or main
                    val prefs = getSharedPreferences("farm_prefs", MODE_PRIVATE)
                    val isRegistered = prefs.getBoolean("is_registered", false)

                    val nextIntent = if (isRegistered) {
                        Intent(this, MainActivity::class.java)
                    } else {
                        Intent(this, RegistrationActivity::class.java)
                    }
                    nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(nextIntent)
                    finish()
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
}
