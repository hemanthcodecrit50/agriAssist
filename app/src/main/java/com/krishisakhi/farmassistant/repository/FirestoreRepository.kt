package com.krishisakhi.farmassistant.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.krishisakhi.farmassistant.data.FarmerProfile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing user farmer profiles in Firestore
 * Uses suspend functions for coroutine support
 */
class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "FirestoreRepository"

        @Volatile
        private var INSTANCE: FirestoreRepository? = null

        fun getInstance(): FirestoreRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Save or update user profile to Firestore
     * @param user The FarmerProfile to save
     * @return true if successful, false otherwise
     */
    suspend fun saveUserProfileToFirestore(user: FarmerProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving user profile to Firestore: uid=${user.uid}")

            val profileData = user.toFirestoreMap()

            usersCollection
                .document(user.uid)
                .set(profileData)
                .await()

            Log.d(TAG, "Successfully saved user profile to Firestore: uid=${user.uid}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile to Firestore: uid=${user.uid}", e)
            false
        }
    }

    /**
     * Fetch user profile from Firestore
     * @param uid Firebase UID of the user
     * @return FarmerProfile if found, null otherwise
     */
    suspend fun fetchUserProfileFromFirestore(uid: String): FarmerProfile? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching user profile from Firestore: uid=$uid")

            val document = usersCollection
                .document(uid)
                .get()
                .await()

            if (document.exists()) {
                val profile = document.toObject(FarmerProfile::class.java)
                Log.d(TAG, "Successfully fetched user profile from Firestore: uid=$uid, name=${profile?.name}")
                profile
            } else {
                Log.d(TAG, "User profile not found in Firestore: uid=$uid")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile from Firestore: uid=$uid", e)
            null
        }
    }

    /**
     * Update user profile in Firestore
     * @param user The FarmerProfile with updated data
     * @return true if successful, false otherwise
     */
    suspend fun updateUserProfile(user: FarmerProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating user profile in Firestore: uid=${user.uid}")

            // Update lastUpdated timestamp
            val updatedProfile = user.copy(lastUpdated = System.currentTimeMillis())
            val profileData = updatedProfile.toFirestoreMap()

            usersCollection
                .document(user.uid)
                .set(profileData)
                .await()

            Log.d(TAG, "Successfully updated user profile in Firestore: uid=${user.uid}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile in Firestore: uid=${user.uid}", e)
            false
        }
    }

    /**
     * Check if user profile exists in Firestore
     * @param uid Firebase UID of the user
     * @return true if profile exists, false otherwise
     */
    suspend fun userProfileExists(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking if user profile exists in Firestore: uid=$uid")

            val document = usersCollection
                .document(uid)
                .get()
                .await()

            val exists = document.exists()
            Log.d(TAG, "User profile exists in Firestore: uid=$uid, exists=$exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user profile exists in Firestore: uid=$uid", e)
            false
        }
    }

    /**
     * Delete user profile from Firestore
     * @param uid Firebase UID of the user
     * @return true if successful, false otherwise
     */
    suspend fun deleteUserProfile(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting user profile from Firestore: uid=$uid")

            usersCollection
                .document(uid)
                .delete()
                .await()

            Log.d(TAG, "Successfully deleted user profile from Firestore: uid=$uid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user profile from Firestore: uid=$uid", e)
            false
        }
    }
}

