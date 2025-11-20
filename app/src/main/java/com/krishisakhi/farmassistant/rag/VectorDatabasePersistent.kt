package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import com.krishisakhi.farmassistant.dao.VectorEntryDao
import com.krishisakhi.farmassistant.data.VectorEntryEntity
import com.krishisakhi.farmassistant.db.AppDatabase
import com.krishisakhi.farmassistant.utils.VectorSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Persistent vector database that stores embeddings in Room and loads them into memory for fast similarity search
 * This class bridges the gap between persistent storage and in-memory vector operations
 */
class VectorDatabasePersistent(context: Context) {

    companion object {
        private const val TAG = "VectorDatabasePersistent"
    }

    private val dao: VectorEntryDao = AppDatabase.getDatabase(context).vectorEntryDao()

    // In-memory cache for fast similarity search
    private var vectorCache: MutableList<VectorCacheEntry> = mutableListOf()
    private val cacheMutex = Mutex()

    /**
     * Internal data class for in-memory vector cache
     */
    private data class VectorCacheEntry(
        val id: String,
        val farmerId: String?,
        val embedding: FloatArray,
        val metadata: JSONObject
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VectorCacheEntry

            if (id != other.id) return false
            if (farmerId != other.farmerId) return false
            if (!embedding.contentEquals(other.embedding)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (farmerId?.hashCode() ?: 0)
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    /**
     * Initialize the database by loading all vectors from Room into memory
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing persistent vector database...")
            loadVectorsIntoMemory()
            val count = vectorCache.size
            Log.d(TAG, "Loaded $count vectors into memory")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing vector database", e)
            false
        }
    }

    /**
     * Load all vectors from Room database into memory cache
     */
    private suspend fun loadVectorsIntoMemory() = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val entries = dao.getAllVectors()
            vectorCache.clear()

            for (entry in entries) {
                try {
                    val embedding = VectorSerializer.byteArrayToFloatArray(entry.vectorBlob)
                    val metadata = JSONObject(entry.metadataJson)

                    vectorCache.add(
                        VectorCacheEntry(
                            id = entry.id,
                            farmerId = entry.farmerId,
                            embedding = embedding,
                            metadata = metadata
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading vector entry ${entry.id}", e)
                }
            }

            Log.d(TAG, "Loaded ${vectorCache.size} vectors into memory cache")
        }
    }

    /**
     * Insert a new vector entry into both persistent storage and memory cache
     */
    suspend fun insertVector(
        id: String,
        embedding: FloatArray,
        metadata: Map<String, Any>,
        farmerId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadataJson = JSONObject(metadata).toString()
            val vectorBlob = VectorSerializer.floatArrayToByteArray(embedding)
            val sourceType = metadata["type"]?.toString() ?: if (farmerId == null) "general" else "farmer_profile"
            val entity = VectorEntryEntity(
                id = id,
                farmerId = farmerId,
                sourceType = sourceType,
                vectorBlob = vectorBlob,
                metadataJson = metadataJson
            )

            // Insert into Room
            dao.insertVector(entity)

            // Update memory cache
            cacheMutex.withLock {
                // Remove existing entry with same ID if present
                vectorCache.removeAll { it.id == id }

                // Add new entry
                vectorCache.add(
                    VectorCacheEntry(
                        id = id,
                        farmerId = farmerId,
                        embedding = embedding,
                        metadata = JSONObject(metadataJson)
                    )
                )
            }

            Log.d(TAG, "Inserted vector: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting vector: $id", e)
            false
        }
    }

    /**
     * Insert multiple vector entries in batch
     */
    suspend fun insertVectorsBatch(entries: List<VectorInsertData>): Boolean = withContext(Dispatchers.IO) {
        try {
            val entities = entries.map { data ->
                val sourceType = data.metadata["type"]?.toString() ?: if (data.farmerId == null) "general" else "farmer_profile"
                VectorEntryEntity(
                    id = data.id,
                    farmerId = data.farmerId,
                    sourceType = sourceType,
                    vectorBlob = VectorSerializer.floatArrayToByteArray(data.embedding),
                    metadataJson = JSONObject(data.metadata).toString()
                )
            }

            // Insert into Room in batch
            dao.insertVectors(entities)

            // Reload memory cache
            loadVectorsIntoMemory()

            Log.d(TAG, "Inserted ${entries.size} vectors in batch")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting vectors batch", e)
            false
        }
    }

    /**
     * Search for similar vectors using cosine similarity
     * Returns raw entries sorted by similarity score
     */
    suspend fun searchSimilar(
        queryEmbedding: FloatArray,
        topK: Int = 5,
        minScore: Float = 0.3f,
        farmerIdFilter: String? = null
    ): List<SimilaritySearchResult> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val results = mutableListOf<SimilaritySearchResult>()

            for (entry in vectorCache) {
                // Apply farmer ID filter if specified
                if (farmerIdFilter != null && entry.farmerId != farmerIdFilter) {
                    continue
                }

                // Calculate similarity
                val similarity = VectorSerializer.cosineSimilarity(queryEmbedding, entry.embedding)

                // Filter by minimum score
                if (similarity >= minScore) {
                    results.add(
                        SimilaritySearchResult(
                            id = entry.id,
                            farmerId = entry.farmerId,
                            score = similarity,
                            metadata = entry.metadata
                        )
                    )
                }
            }

            // Sort by score descending and take top K
            results.sortedByDescending { it.score }.take(topK)
        }
    }

    /**
     * Get vector by ID
     */
    suspend fun getVectorById(id: String): VectorEntryEntity? = withContext(Dispatchers.IO) {
        dao.getVectorById(id)
    }

    /**
     * Delete vector by ID
     */
    suspend fun deleteVector(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            dao.deleteVectorById(id)

            // Remove from cache
            cacheMutex.withLock {
                vectorCache.removeAll { it.id == id }
            }

            Log.d(TAG, "Deleted vector: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting vector: $id", e)
            false
        }
    }

    /**
     * Delete all vectors for a specific farmer
     */
    suspend fun deleteVectorsByFarmerId(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            dao.deleteVectorsByFarmerId(farmerId)

            // Remove from cache
            cacheMutex.withLock {
                vectorCache.removeAll { it.farmerId == farmerId }
            }

            Log.d(TAG, "Deleted vectors for farmer: $farmerId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting vectors for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Get count of all vectors
     */
    suspend fun getVectorCount(): Int = withContext(Dispatchers.IO) {
        dao.getVectorCount()
    }

    /**
     * Clear all vectors
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            dao.clearAll()

            // Clear cache
            cacheMutex.withLock {
                vectorCache.clear()
            }

            Log.d(TAG, "Cleared all vectors")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing vectors", e)
            false
        }
    }

    /**
     * Reload vectors from database into memory
     * Useful after external database modifications
     */
    suspend fun reloadCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            loadVectorsIntoMemory()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading cache", e)
            false
        }
    }
}

/**
 * Data class for inserting vectors
 */
data class VectorInsertData(
    val id: String,
    val embedding: FloatArray,
    val metadata: Map<String, Any>,
    val farmerId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorInsertData

        if (id != other.id) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (farmerId != other.farmerId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + (farmerId?.hashCode() ?: 0)
        return result
    }
}

/**
 * Result of similarity search
 */
data class SimilaritySearchResult(
    val id: String,
    val farmerId: String?,
    val score: Float,
    val metadata: JSONObject
)
