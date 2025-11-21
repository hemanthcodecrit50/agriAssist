package com.krishisakhi.farmassistant.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.krishisakhi.farmassistant.data.FarmerProfile

@Dao
interface FarmerProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FarmerProfile)

    @Update
    suspend fun updateProfile(profile: FarmerProfile)

    @Query("SELECT * FROM farmer_profiles WHERE uid = :uid")
    suspend fun getProfileByUid(uid: String): FarmerProfile?

    @Query("SELECT * FROM farmer_profiles WHERE phoneNumber = :phone")
    suspend fun getProfileByPhone(phone: String): FarmerProfile?

    @Query("SELECT * FROM farmer_profiles")
    suspend fun getAllProfiles(): List<FarmerProfile>

    @Query("DELETE FROM farmer_profiles WHERE uid = :uid")
    suspend fun deleteProfile(uid: String)

    @Query("DELETE FROM farmer_profiles")
    suspend fun deleteAllProfiles()
}