package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

/**
 * Manages the knowledge base including initialization and updates
 */
class KnowledgeBaseManager(private val context: Context) {

    companion object {
        private const val TAG = "KnowledgeBaseManager"
        private const val PREFS_NAME = "knowledge_base_prefs"
        private const val KEY_GENERAL_INITIALIZED = "kb_general_initialized"
    }

    private val vectorDbPersistent = VectorDatabasePersistent(context)
    private val embeddingService = EmbeddingService(context)

    /**
     * Public detection method: returns true if general knowledge already initialized.
     * Uses both preference flag and a lightweight count of general vectors.
     */
    suspend fun isGeneralKnowledgeInitialized(): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val flag = prefs.getBoolean(KEY_GENERAL_INITIALIZED, false)
        if (!flag) return@withContext false
        // Optional secondary check: ensure at least one general vector exists (farmerId = null)
        try {
            val dao = com.krishisakhi.farmassistant.db.AppDatabase.getDatabase(context).vectorEntryDao()
            val staticCount = dao.getStaticKnowledgeVectors().size
            staticCount > 0
        } catch (e: Exception) {
            Log.w(TAG, "Secondary static vector count check failed: ${e.message}")
            false
        }
    }

    /**
     * Initialize knowledge base with agricultural_knowledge.json only on first run.
     * Skips re-embedding if vectors already exist.
     */
    suspend fun initializeKnowledgeBase(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Ensure vector DB cache initialized
            vectorDbPersistent.initialize()

            // If already initialized, skip
            if (isGeneralKnowledgeInitialized()) {
                Log.d(TAG, "General knowledge already initialized; skipping JSON load & embeddings")
                return@withContext true
            }

            Log.d(TAG, "Loading agricultural_knowledge.json for initial general knowledge embedding")
            val docs = loadGeneralKnowledgeJson()
            if (docs.isEmpty()) {
                Log.w(TAG, "No documents parsed from agricultural_knowledge.json")
                return@withContext false
            }

            val vectorInsertDataList = mutableListOf<VectorInsertData>()
            var skipped = 0
            var embedded = 0

            for (doc in docs) {
                // Skip embedding if vector with same ID already exists (defensive re-run scenario)
                val existing = vectorDbPersistent.getVectorById(doc.id)
                if (existing != null) {
                    skipped++
                    continue
                }
                val embedding = embeddingService.generateEmbedding("${doc.title} ${doc.content}")
                if (embedding == null) {
                    Log.w(TAG, "Embedding generation failed for doc ${doc.id}; skipping")
                    continue
                }
                val metadata = mapOf(
                    "id" to doc.id,
                    "title" to doc.title,
                    "content" to doc.content,
                    "category" to doc.category,
                    "tags" to doc.tags.joinToString(","),
                    "type" to "general",
                    "timestamp" to System.currentTimeMillis()
                )
                vectorInsertDataList.add(
                    VectorInsertData(
                        id = doc.id,
                        embedding = embedding,
                        metadata = metadata,
                        farmerId = null
                    )
                )
                embedded++
            }

            if (vectorInsertDataList.isNotEmpty()) {
                vectorDbPersistent.insertVectorsBatch(vectorInsertDataList)
            }

            // Persist initialization flag only if some docs exist (embedded or previously present)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_GENERAL_INITIALIZED, true).apply()

            Log.d(TAG, "General knowledge initialization complete: embedded=$embedded, skipped(existing)=$skipped, total=${docs.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing general knowledge", e)
            false
        }
    }

    /**
     * Load and parse agricultural_knowledge.json from assets.
     */
    private fun loadGeneralKnowledgeJson(): List<SimpleDoc> {
        return try {
            val inputStream = context.assets.open("agricultural_knowledge.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.use { it.readText() }
            val jsonArray = JSONArray(content)
            val list = mutableListOf<SimpleDoc>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "Untitled")
                val contentText = obj.optString("content", "")
                val category = obj.optString("category", "general")
                val tagsJson = obj.optJSONArray("tags") ?: JSONArray()
                val tags = (0 until tagsJson.length()).map { idx -> tagsJson.optString(idx) }.filter { it.isNotBlank() }
                list.add(SimpleDoc(id, title, contentText, category, tags))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read agricultural_knowledge.json: ${e.message}")
            emptyList()
        }
    }

    // Simple data holder for JSON docs
    private data class SimpleDoc(
        val id: String,
        val title: String,
        val content: String,
        val category: String,
        val tags: List<String>
    )

    /**
     * Add a new document to the knowledge base (dynamic addition post-initialization)
     */
    suspend fun addDocument(
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        farmerId: String? = null
    ): Boolean {
        return try {
            val embedding = embeddingService.generateEmbedding("$title $content")
            if (embedding != null) {
                val id = UUID.randomUUID().toString()
                val metadata = mapOf(
                    "title" to title,
                    "content" to content,
                    "category" to category,
                    "tags" to tags.joinToString(","),
                    "type" to if (farmerId == null) "general" else "farmer_profile",
                    "timestamp" to System.currentTimeMillis()
                )
                vectorDbPersistent.insertVector(
                    id = id,
                    embedding = embedding,
                    metadata = metadata,
                    farmerId = farmerId
                )
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error adding document", e)
            false
        }
    }

    /**
     * Search (existing logic retained)
     */
    suspend fun search(
        query: String,
        topK: Int = 3,
        categoryFilter: String? = null,
        farmerIdFilter: String? = null
    ): List<SearchResult> {
        return try {
            val queryEmbedding = embeddingService.generateEmbedding(query) ?: return emptyList()
            val similarityResults = vectorDbPersistent.searchSimilar(
                queryEmbedding = queryEmbedding,
                topK = topK,
                minScore = 0.3f,
                farmerIdFilter = farmerIdFilter
            )
            similarityResults.mapNotNull { result ->
                try {
                    val metadata = result.metadata
                    val title = metadata.optString("title", "Untitled")
                    val content = metadata.optString("content", "")
                    val category = metadata.optString("category", "general")
                    val tagsStr = metadata.optString("tags", "")
                    val tags = if (tagsStr.isNotEmpty()) tagsStr.split(",") else emptyList()
                    val timestamp = metadata.optLong("timestamp", System.currentTimeMillis())
                    if (categoryFilter != null && category != categoryFilter) return@mapNotNull null
                    val document = KnowledgeDocument(
                        id = result.id,
                        title = title,
                        content = content,
                        category = category,
                        tags = tags,
                        embedding = null,
                        timestamp = timestamp
                    )
                    val snippet = if (content.length <= 150) content else content.substring(0, 150) + "..."
                    SearchResult(document, result.score, snippet)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting search result", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents", e)
            emptyList()
        }
    }

    /**
     * Reset knowledge base
     */
    suspend fun resetKnowledgeBase(): Boolean {
        return try {
            vectorDbPersistent.clearAll()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_GENERAL_INITIALIZED, false).apply()
            Log.d(TAG, "General knowledge base reset successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting knowledge base", e)
            false
        }
    }

    /**
     * Add farmer profile as a vector entry
     */
    suspend fun addFarmerProfile(
        farmerId: String,
        profileContent: String,
        profileData: Map<String, Any>
    ): Boolean {
        return try {
            val embedding = embeddingService.generateEmbedding(profileContent)
            if (embedding != null) {
                val id = "profile_$farmerId"
                val metadata = profileData.toMutableMap()
                metadata["timestamp"] = System.currentTimeMillis()
                metadata["type"] = "farmer_profile"
                vectorDbPersistent.insertVector(
                    id = id,
                    embedding = embedding,
                    metadata = metadata,
                    farmerId = farmerId
                )
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error adding farmer profile vector", e)
            false
        }
    }

    /**
     * Update farmer profile vector
     */
    suspend fun updateFarmerProfile(
        farmerId: String,
        profileContent: String,
        profileData: Map<String, Any>
    ): Boolean {
        return try {
            vectorDbPersistent.deleteVector("profile_$farmerId")
            addFarmerProfile(farmerId, profileContent, profileData)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating farmer profile vector", e)
            false
        }
    }

    /**
     * Delete farmer profile vector
     */
    suspend fun deleteFarmerProfile(farmerId: String): Boolean {
        return try {
            vectorDbPersistent.deleteVector("profile_$farmerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting farmer profile vector", e)
            false
        }
    }
}
