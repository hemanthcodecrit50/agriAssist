package com.krishisakhi.farmassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_profiles")
data class FarmerProfile(
    @PrimaryKey
    val phoneNumber: String, // Using phone number as the unique ID

    val name: String,
    val state: String,
    val district: String,
    val village: String,
    val totalLandSize: Double,
    val soilType: String,
    val primaryCrops: List<String>,
    val languagePreference: String
)