package com.krishisakhi.farmassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_profiles")
data class FarmerProfile(
    @PrimaryKey
    val uid: String = "", // Firebase UID as primary key

    val phoneNumber: String = "", // Phone number for reference

    val name: String = "",

    val state: String = "",

    val district: String = "",

    val village: String = "",

    val totalLandSize: Double = 0.0,

    val soilType: String = "",

    val primaryCrops: List<String> = emptyList(),

    val languagePreference: String = "English",

    val lastUpdated: Long = System.currentTimeMillis(), // Timestamp for conflict resolution

    val createdAt: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firestore
    constructor() : this(
        uid = "",
        phoneNumber = "",
        name = "",
        state = "",
        district = "",
        village = "",
        totalLandSize = 0.0,
        soilType = "",
        primaryCrops = emptyList(),
        languagePreference = "English",
        lastUpdated = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )

    // Convert to Firestore map
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "phoneNumber" to phoneNumber,
            "name" to name,
            "state" to state,
            "district" to district,
            "village" to village,
            "totalLandSize" to totalLandSize,
            "soilType" to soilType,
            "primaryCrops" to primaryCrops,
            "languagePreference" to languagePreference,
            "lastUpdated" to lastUpdated,
            "createdAt" to createdAt
        )
    }
}

