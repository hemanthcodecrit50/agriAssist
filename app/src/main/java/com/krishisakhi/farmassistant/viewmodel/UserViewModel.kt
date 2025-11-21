package com.krishisakhi.farmassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.data.FarmerProfile
import com.krishisakhi.farmassistant.repository.FirestoreRepository
import com.krishisakhi.farmassistant.sync.SyncManager
import kotlinx.coroutines.launch

/**
 * UserViewModel - MVVM ViewModel for managing user farmer profiles
 *
 * Follows Repository pattern with clean architecture
 * Handles all user profile operations with Room + Firestore sync
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val syncManager = SyncManager.getInstance(application)
    private val firestoreRepo = FirestoreRepository.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData for observing profile changes
    private val _userProfile = MutableLiveData<FarmerProfile?>()
    val userProfile: LiveData<FarmerProfile?> = _userProfile

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData for success messages
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    /**
     * Load user profile on login
     * Fetches from Firestore and syncs to Room
     */
    fun loadUserProfileOnLogin(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = syncManager.syncOnLogin(uid)
                _userProfile.value = profile

                if (profile != null) {
                    _successMessage.value = "Profile loaded successfully"
                } else {
                    _errorMessage.value = "No profile found for this user"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save user profile on registration
     * Saves to both Room and Firestore
     */
    fun saveUserProfileOnRegistration(profile: FarmerProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = syncManager.syncOnRegistration(profile)

                if (success) {
                    _userProfile.value = profile
                    _successMessage.value = "Profile created successfully"
                } else {
                    _errorMessage.value = "Failed to create profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update user profile
     * Updates Room first, then Firestore
     */
    fun updateUserProfile(profile: FarmerProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = syncManager.syncOnProfileEdit(profile)

                if (success) {
                    _userProfile.value = profile
                    _successMessage.value = "Profile updated successfully"
                } else {
                    _errorMessage.value = "Failed to update profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sync profile on logout
     * Flushes Room to Firestore
     */
    fun syncProfileOnLogout(uid: String) {
        viewModelScope.launch {
            try {
                syncManager.syncOnLogout(uid)
            } catch (e: Exception) {
                // Silent failure - logout should still proceed
            }
        }
    }

    /**
     * Check if profile exists in Firestore
     */
    fun checkProfileExists(uid: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val exists = syncManager.profileExistsInFirestore(uid)
                callback(exists)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    /**
     * Load profile from local Room database
     */
    fun loadLocalProfile(uid: String) {
        viewModelScope.launch {
            try {
                val profile = syncManager.getLocalProfile(uid)
                _userProfile.value = profile
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load local profile: ${e.message}"
            }
        }
    }

    /**
     * Force sync profile from Firestore
     * Useful for manual refresh
     */
    fun forceSyncProfile(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = syncManager.forceSync(uid)
                _userProfile.value = profile

                if (profile != null) {
                    _successMessage.value = "Profile synced successfully"
                } else {
                    _errorMessage.value = "Failed to sync profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to sync profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get current user UID
     */
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}

