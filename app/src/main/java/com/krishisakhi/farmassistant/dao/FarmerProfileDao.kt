package com.krishisakhi.farmassistant.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.krishisakhi.farmassistant.data.FarmerProfile

@Dao
interface FarmerProfileDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertProfile(profile: FarmerProfile)

    @Query("SELECT * FROM farmer_profiles WHERE phoneNumber = :phone")
    suspend fun getProfileByPhone(phone: String): FarmerProfile?

    @Query("SELECT * FROM farmer_profiles")
    suspend fun getAllProfiles(): List<FarmerProfile>
}