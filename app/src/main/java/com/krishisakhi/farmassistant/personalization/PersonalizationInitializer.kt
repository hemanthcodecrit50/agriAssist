package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class to initialize personalization system for a farmer
 * Call this once when a new farmer signs up or first logs in
 */
class PersonalizationInitializer(private val context: Context) {

    companion object {
        private const val TAG = "PersonalizationInitializer"
        private const val PREFS_NAME = "personalization_prefs"
    }

    private val fileManager = FarmerFileManager(context)
    private val embeddingManager = PersonalizationEmbeddingManager(context)

    /**
     * Initialize personalization for a farmer
     * - Creates empty files if they don't exist
     * - Checks if already initialized
     */
    suspend fun initializeForFarmer(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing personalization for farmer: $farmerId")

            // Check if already initialized
            if (isInitialized(farmerId)) {
                Log.d(TAG, "Personalization already initialized for farmer: $farmerId")
                return@withContext true
            }

            // Create empty profile file if it doesn't exist
            if (!fileManager.hasProfile(farmerId)) {
                fileManager.writeProfile(farmerId, "")
                Log.d(TAG, "Created empty profile file for farmer: $farmerId")
            }

            // Insights file is created automatically when first insight is added

            // Mark as initialized
            markAsInitialized(farmerId)

            Log.d(TAG, "Personalization initialized successfully for farmer: $farmerId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing personalization for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Initialize for current logged-in farmer
     */
    suspend fun initializeForCurrentFarmer(): Boolean = withContext(Dispatchers.IO) {
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid
        if (farmerId == null) {
            Log.w(TAG, "No farmer logged in")
            return@withContext false
        }
        initializeForFarmer(farmerId)
    }

    /**
     * Check if personalization is initialized for a farmer
     */
    private fun isInitialized(farmerId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("initialized_$farmerId", false)
    }

    /**
     * Mark personalization as initialized for a farmer
     */
    private fun markAsInitialized(farmerId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("initialized_$farmerId", true).apply()
    }

    /**
     * Reset initialization flag (for testing)
     */
    fun resetInitialization(farmerId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("initialized_$farmerId", false).apply()
    }
}

