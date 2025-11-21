package com.krishisakhi.farmassistant.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.data.FarmerProfile
import com.krishisakhi.farmassistant.db.AppDatabase
import com.krishisakhi.farmassistant.repository.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SyncManager handles synchronization between Room (local) and Firestore (cloud)
 *
 * Sync Strategy:
 * - On login: Fetch from Firestore → Write to Room
 * - On registration: Write to Room and Firestore
 * - On profile edit: Update Room → Update Firestore
 * - On logout: Flush Room → Firestore (if needed)
 */
class SyncManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firestoreRepo = FirestoreRepository.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "SyncManager"

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * On Login: Fetch profile from Firestore and sync to Room
     * @param uid Firebase UID of the logged-in user
     * @return FarmerProfile if found and synced, null otherwise
     */
    suspend fun syncOnLogin(uid: String): FarmerProfile? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync on login for uid: $uid")

            // Fetch profile from Firestore
            val firestoreProfile = firestoreRepo.fetchUserProfileFromFirestore(uid)

            if (firestoreProfile != null) {
                Log.d(TAG, "Profile found in Firestore, syncing to Room")

                // Write to Room database
                db.farmerProfileDao().insertProfile(firestoreProfile)

                Log.d(TAG, "Successfully synced profile to Room on login")
                firestoreProfile
            } else {
                Log.d(TAG, "No profile found in Firestore for uid: $uid")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync on login for uid: $uid", e)
            null
        }
    }

    /**
     * On Registration: Save profile to both Room and Firestore
     * @param profile FarmerProfile to save
     * @return true if successful, false otherwise
     */
    suspend fun syncOnRegistration(profile: FarmerProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync on registration for uid: ${profile.uid}")

            // 1. Save to Room (local first for immediate access)
            db.farmerProfileDao().insertProfile(profile)
            Log.d(TAG, "Profile saved to Room")

            // 2. Save to Firestore (cloud backup)
            val firestoreSuccess = firestoreRepo.saveUserProfileToFirestore(profile)

            if (firestoreSuccess) {
                Log.d(TAG, "Profile saved to Firestore successfully")
                true
            } else {
                Log.w(TAG, "Failed to save profile to Firestore, but Room save succeeded")
                // Still return true since Room save succeeded (offline-first approach)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync on registration for uid: ${profile.uid}", e)
            false
        }
    }

    /**
     * On Profile Edit: Update Room first, then Firestore
     * @param profile Updated FarmerProfile
     * @return true if successful, false otherwise
     */
    suspend fun syncOnProfileEdit(profile: FarmerProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync on profile edit for uid: ${profile.uid}")

            // Update timestamp for conflict resolution
            val updatedProfile = profile.copy(lastUpdated = System.currentTimeMillis())

            // 1. Update Room (local first for immediate access)
            db.farmerProfileDao().updateProfile(updatedProfile)
            Log.d(TAG, "Profile updated in Room")

            // 2. Update Firestore (cloud sync)
            val firestoreSuccess = firestoreRepo.updateUserProfile(updatedProfile)

            if (firestoreSuccess) {
                Log.d(TAG, "Profile updated in Firestore successfully")
                true
            } else {
                Log.w(TAG, "Failed to update profile in Firestore, but Room update succeeded")
                // Still return true since Room update succeeded (offline-first approach)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync on profile edit for uid: ${profile.uid}", e)
            false
        }
    }

    /**
     * On Logout: Flush Room data to Firestore (if needed) and clear local cache
     * @param uid Firebase UID of the user logging out
     * @return true if successful, false otherwise
     */
    suspend fun syncOnLogout(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync on logout for uid: $uid")

            // 1. Get current profile from Room
            val localProfile = db.farmerProfileDao().getProfileByUid(uid)

            if (localProfile != null) {
                // 2. Flush to Firestore (ensure cloud is up-to-date)
                val firestoreSuccess = firestoreRepo.updateUserProfile(localProfile)

                if (firestoreSuccess) {
                    Log.d(TAG, "Profile flushed to Firestore successfully")
                } else {
                    Log.w(TAG, "Failed to flush profile to Firestore")
                }

                // 3. Clear local cache (optional - you may want to keep it)
                // Uncomment the line below if you want to clear local data on logout
                // db.farmerProfileDao().deleteProfile(uid)
                // Log.d(TAG, "Local profile cleared from Room")
            } else {
                Log.d(TAG, "No local profile found for uid: $uid")
            }

            Log.d(TAG, "Logout sync completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync on logout for uid: $uid", e)
            false
        }
    }

    /**
     * Force sync: Fetch from Firestore and overwrite Room
     * Useful for manual refresh or conflict resolution
     * @param uid Firebase UID of the user
     * @return FarmerProfile if found and synced, null otherwise
     */
    suspend fun forceSync(uid: String): FarmerProfile? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting force sync for uid: $uid")

            // Fetch from Firestore
            val firestoreProfile = firestoreRepo.fetchUserProfileFromFirestore(uid)

            if (firestoreProfile != null) {
                // Overwrite Room
                db.farmerProfileDao().insertProfile(firestoreProfile)
                Log.d(TAG, "Force sync completed successfully")
                firestoreProfile
            } else {
                Log.d(TAG, "No profile found in Firestore for force sync")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during force sync for uid: $uid", e)
            null
        }
    }

    /**
     * Check if profile exists in Firestore
     * @param uid Firebase UID of the user
     * @return true if exists, false otherwise
     */
    suspend fun profileExistsInFirestore(uid: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext firestoreRepo.userProfileExists(uid)
    }

    /**
     * Get profile from local Room database
     * @param uid Firebase UID of the user
     * @return FarmerProfile if found, null otherwise
     */
    suspend fun getLocalProfile(uid: String): FarmerProfile? = withContext(Dispatchers.IO) {
        return@withContext db.farmerProfileDao().getProfileByUid(uid)
    }
}

