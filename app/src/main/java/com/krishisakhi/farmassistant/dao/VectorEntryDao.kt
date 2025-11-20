package com.krishisakhi.farmassistant.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.krishisakhi.farmassistant.data.VectorEntryEntity

/**
 * DAO for vector entry operations
 * Provides basic CRUD operations; similarity search is computed at service level
 */
@Dao
interface VectorEntryDao {

    /**
     * Insert a single vector entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVector(entry: VectorEntryEntity)

    /**
     * Insert multiple vector entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVectors(entries: List<VectorEntryEntity>)

    /**
     * Update an existing vector entry
     */
    @Update
    suspend fun updateVector(entry: VectorEntryEntity)

    /**
     * Delete a vector entry by ID
     */
    @Query("DELETE FROM vector_entries WHERE id = :id")
    suspend fun deleteVectorById(id: String)

    /**
     * Delete vector entries by farmer ID
     */
    @Query("DELETE FROM vector_entries WHERE farmerId = :farmerId")
    suspend fun deleteVectorsByFarmerId(farmerId: String)

    /**
     * Get all vector entries (for loading into memory)
     */
    @Query("SELECT * FROM vector_entries")
    suspend fun getAllVectors(): List<VectorEntryEntity>

    /**
     * Get vector entry by ID
     */
    @Query("SELECT * FROM vector_entries WHERE id = :id")
    suspend fun getVectorById(id: String): VectorEntryEntity?

    /**
     * Get vector entries by farmer ID
     */
    @Query("SELECT * FROM vector_entries WHERE farmerId = :farmerId")
    suspend fun getVectorsByFarmerId(farmerId: String): List<VectorEntryEntity>

    /**
     * Get count of vector entries
     */
    @Query("SELECT COUNT(*) FROM vector_entries")
    suspend fun getVectorCount(): Int

    /**
     * Clear all vector entries
     */
    @Query("DELETE FROM vector_entries")
    suspend fun clearAll()

    /**
     * Get vector entries where farmerId is null (static knowledge)
     */
    @Query("SELECT * FROM vector_entries WHERE farmerId IS NULL")
    suspend fun getStaticKnowledgeVectors(): List<VectorEntryEntity>

    /**
     * Get vector entries where farmerId is not null (farmer-specific)
     */
    @Query("SELECT * FROM vector_entries WHERE farmerId IS NOT NULL")
    suspend fun getFarmerSpecificVectors(): List<VectorEntryEntity>

    /**
     * Get vector entries by source type
     */
    @Query("SELECT * FROM vector_entries WHERE sourceType = :sourceType")
    suspend fun getVectorsBySourceType(sourceType: String): List<VectorEntryEntity>
}
