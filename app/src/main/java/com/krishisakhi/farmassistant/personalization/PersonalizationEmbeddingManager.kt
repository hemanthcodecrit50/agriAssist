package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.rag.EmbeddingService
import com.krishisakhi.farmassistant.rag.VectorDatabasePersistent
import com.krishisakhi.farmassistant.rag.VectorInsertData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages embedding and indexing of personalization files (farmer_profile.txt and farmer_insights.txt)
 * into the RAG vector database
 */
class PersonalizationEmbeddingManager(private val context: Context) {

    companion object {
        private const val TAG = "PersonalizationEmbeddingManager"
        private const val CHUNK_SIZE = 500 // Characters per chunk
        private const val CHUNK_OVERLAP = 50 // Overlap between chunks
    }

    private val fileManager = FarmerFileManager(context)
    private val embeddingService = EmbeddingService(context)
    private val vectorDb = VectorDatabasePersistent(context)

    /**
     * Index both farmer_profile.txt and farmer_insights.txt into vector database
     * Deletes existing personalization vectors and re-embeds everything
     */
    suspend fun indexPersonalizationFiles(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Indexing personalization files for farmer: $farmerId")

            // Delete existing personalization vectors for this farmer
            deletePersonalizationVectors(farmerId)

            // Read both files
            val personalizationData = fileManager.readAllPersonalizationData(farmerId)

            if (personalizationData.isEmpty()) {
                Log.d(TAG, "No personalization data found for farmer: $farmerId")
                return@withContext true // Not an error, just empty
            }

            val vectorsToInsert = mutableListOf<VectorInsertData>()

            // Index profile file if it exists
            if (personalizationData.profileContent.isNotEmpty()) {
                val profileVectors = embedFile(
                    farmerId = farmerId,
                    content = personalizationData.profileContent,
                    sourceType = "farmer_profile",
                    sourceFile = "farmer_profile.txt"
                )
                vectorsToInsert.addAll(profileVectors)
                Log.d(TAG, "Created ${profileVectors.size} embeddings from farmer_profile.txt")
            }

            // Index insights file if it exists
            if (personalizationData.insightsContent.isNotEmpty()) {
                val insightsVectors = embedFile(
                    farmerId = farmerId,
                    content = personalizationData.insightsContent,
                    sourceType = "farmer_insights",
                    sourceFile = "farmer_insights.txt"
                )
                vectorsToInsert.addAll(insightsVectors)
                Log.d(TAG, "Created ${insightsVectors.size} embeddings from farmer_insights.txt")
            }

            // Batch insert all vectors
            if (vectorsToInsert.isNotEmpty()) {
                val success = vectorDb.insertVectorsBatch(vectorsToInsert)
                if (success) {
                    Log.d(TAG, "Successfully indexed ${vectorsToInsert.size} personalization vectors for farmer: $farmerId")
                } else {
                    Log.e(TAG, "Failed to insert personalization vectors for farmer: $farmerId")
                }
                return@withContext success
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing personalization files for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Embed a single file's content by chunking it and generating embeddings
     */
    private suspend fun embedFile(
        farmerId: String,
        content: String,
        sourceType: String,
        sourceFile: String
    ): List<VectorInsertData> = withContext(Dispatchers.IO) {
        try {
            // Split content into chunks
            val chunks = chunkText(content)

            val vectorList = mutableListOf<VectorInsertData>()

            for ((index, chunk) in chunks.withIndex()) {
                if (chunk.isBlank()) continue

                // Generate embedding for chunk
                val embedding = embeddingService.generateEmbedding(chunk)
                if (embedding == null || embedding.isEmpty()) {
                    Log.w(TAG, "Failed to generate embedding for chunk $index of $sourceFile")
                    continue
                }

                // Create unique ID for this chunk
                val chunkId = "${farmerId}_${sourceType}_chunk_$index"

                // Create metadata
                val metadata = mapOf(
                    "id" to chunkId,
                    "type" to sourceType,
                    "source_file" to sourceFile,
                    "chunk_index" to index,
                    "content" to chunk,
                    "name" to "Farmer Personalization",
                    "timestamp" to System.currentTimeMillis()
                )

                vectorList.add(
                    VectorInsertData(
                        id = chunkId,
                        embedding = embedding,
                        metadata = metadata,
                        farmerId = farmerId
                    )
                )
            }

            vectorList
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding file $sourceFile", e)
            emptyList()
        }
    }

    /**
     * Split text into overlapping chunks for better context preservation
     */
    private fun chunkText(text: String): List<String> {
        if (text.length <= CHUNK_SIZE) {
            return listOf(text)
        }

        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + CHUNK_SIZE, text.length)
            val chunk = text.substring(startIndex, endIndex)
            chunks.add(chunk)

            // Move to next chunk with overlap
            startIndex += CHUNK_SIZE - CHUNK_OVERLAP
        }

        return chunks
    }

    /**
     * Delete all personalization vectors (both profile and insights) for a farmer
     */
    suspend fun deletePersonalizationVectors(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting personalization vectors for farmer: $farmerId")

            // VectorDatabasePersistent has deleteVectorsByFarmerId method
            val success = vectorDb.deleteVectorsByFarmerId(farmerId)

            if (success) {
                Log.d(TAG, "Deleted personalization vectors for farmer: $farmerId")
            } else {
                Log.w(TAG, "Failed to delete personalization vectors for farmer: $farmerId")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting personalization vectors for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Incrementally update embeddings after new insights are added
     * This only re-embeds the insights file, not the profile
     */
    suspend fun updateInsightsEmbeddings(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating insights embeddings for farmer: $farmerId")

            // Delete only insights vectors
            deleteInsightsVectors(farmerId)

            // Re-embed insights file
            val insightsContent = fileManager.readInsights(farmerId)
            if (insightsContent.isEmpty()) {
                Log.d(TAG, "No insights to embed for farmer: $farmerId")
                return@withContext true
            }

            val insightsVectors = embedFile(
                farmerId = farmerId,
                content = insightsContent,
                sourceType = "farmer_insights",
                sourceFile = "farmer_insights.txt"
            )

            if (insightsVectors.isNotEmpty()) {
                val success = vectorDb.insertVectorsBatch(insightsVectors)
                if (success) {
                    Log.d(TAG, "Updated ${insightsVectors.size} insights embeddings for farmer: $farmerId")
                }
                return@withContext success
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating insights embeddings for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Delete only insights vectors (keep profile vectors)
     */
    private suspend fun deleteInsightsVectors(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get all vectors for this farmer
            val dao = com.krishisakhi.farmassistant.db.AppDatabase.getDatabase(context).vectorEntryDao()
            val allVectors = dao.getVectorsByFarmerId(farmerId)

            // Filter insights vectors
            val insightsVectorIds = allVectors
                .filter { it.sourceType == "farmer_insights" }
                .map { it.id }

            // Delete each insights vector
            for (id in insightsVectorIds) {
                vectorDb.deleteVector(id)
            }

            Log.d(TAG, "Deleted ${insightsVectorIds.size} insights vectors for farmer: $farmerId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting insights vectors for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Get current farmer ID from Firebase Auth
     */
    fun getCurrentFarmerId(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current farmer ID", e)
            null
        }
    }
}

