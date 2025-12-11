package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.classifier.IntentClassifier
import com.krishisakhi.farmassistant.classifier.QueryIntent
import com.krishisakhi.farmassistant.personalization.InsightsFileManager
import com.krishisakhi.farmassistant.personalization.UserProfileFileManager
import com.krishisakhi.farmassistant.utils.ContextManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Enhanced AI Service with Stateless RAG Pipeline
 *
 * Architecture:
 * - Embeds: agricultural_knowledge.json + farmer_profile.txt + farmer_insights.txt
 * - Retrieves: Top 3 most relevant chunks (profile prioritized)
 * - Generates: Context-aware responses using Gemini AI
 * - Updates: Incremental vector DB updates for insights
 *
 * Features:
 * - Stateless (no conversation history)
 * - Top 3 semantic chunks per query
 * - Always includes 1 profile chunk if relevant
 * - Automatic personalization file embedding
 * - Automatic insight extraction
 */
class EnhancedAIService(private val context: Context, private val apiKey: String) {

    companion object {
        private const val TAG = "EnhancedAIService"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val TOP_K = 3 // Top 3 semantic chunks only
        private const val MIN_SIMILARITY_SCORE = 0.3f
        private const val PROFILE_BOOST = 1.2f // Boost profile chunks by 20%
    }

    private val knowledgeBaseManager = KnowledgeBaseManager(context)
    private val intentClassifier = IntentClassifier(context)
    private val contextManager = ContextManager(context)
    private val vectorDb = VectorDatabasePersistent(context)
    private val embeddingService = EmbeddingService(context)
    private val insightsFileManager = InsightsFileManager(context, apiKey)
    private val profileFileManager = UserProfileFileManager(context)

    private val model = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048
        }
    )

    /**
     * Callback interface for AI responses
     */
    interface EnhancedAICallback {
        fun onSuccess(response: String, intent: QueryIntent, sources: List<String>)
        fun onError(error: String)
    }

    /**
     * Initialize RAG pipeline
     *
     * Steps:
     * 1. Initialize vector database (load cache)
     * 2. Embed agricultural_knowledge.json (general knowledge)
     * 3. Embed farmer_profile.txt chunks
     * 4. Embed farmer_insights.txt chunks
     *
     * @return true if initialization successful
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Initializing Enhanced AI Service ===")

            // Step 1: Initialize vector database
            Log.d(TAG, "Step 1: Initializing vector database...")
            vectorDb.initialize()

            // Step 2: Initialize general knowledge base (agricultural_knowledge.json)
            Log.d(TAG, "Step 2: Initializing general knowledge base...")
            val kbSuccess = knowledgeBaseManager.initializeKnowledgeBase()
            if (!kbSuccess) {
                Log.e(TAG, "Failed to initialize general knowledge base")
                return@withContext false
            }

            // Step 3: Initialize personalization files
            val farmerId = getCurrentFarmerId()
            if (farmerId != null) {
                Log.d(TAG, "Step 3: Initializing personalization for farmer: $farmerId")

                // Initialize files
                profileFileManager.createOrLoadProfileFile()
                insightsFileManager.createOrLoadInsightsFile()

                // Embed files
                embedProfileFile(farmerId)
                embedInsightsFile(farmerId)
            } else {
                Log.w(TAG, "No farmer logged in, skipping personalization")
            }

            Log.d(TAG, "=== Enhanced AI Service initialized successfully ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Enhanced AI Service", e)
            false
        }
    }

    /**
     * Generate enhanced response using stateless RAG pipeline
     *
     * Process:
     * 1. Classify query intent
     * 2. Retrieve top 3 semantic chunks (with profile priority)
     * 3. Build context: SYSTEM + RAG chunks + Query
     * 4. Generate AI response
     * 5. Extract and store insights (async)
     *
     * @param userQuery The user's question
     * @param callback Callback for response/error
     */
    suspend fun generateEnhancedResponse(userQuery: String, callback: EnhancedAICallback) {
        var responseText = ""
        try {
            Log.d(TAG, "=== Processing Query ===")
            Log.d(TAG, "Query: ${userQuery.take(100)}...")

            // Step 1: Classify intent
            val intentResult = intentClassifier.classify(userQuery)
            val intent = intentResult.intent
            Log.d(TAG, "Intent: $intent")

            // Step 2: Retrieve top 3 semantic chunks
            val chunks = retrieveTopChunks(userQuery)
            Log.d(TAG, "Retrieved ${chunks.size} chunks")

            chunks.forEachIndexed { index, chunk ->
                Log.d(TAG, "  ${index + 1}. ${chunk.type} (score: ${String.format(Locale.US, "%.3f", chunk.score)})")
            }

            // Step 3: Build RAG context
            val ragContext = buildRAGContext(chunks)
            val sources = chunks.map { "${it.type}: ${it.title}" }

            // Step 4: Build final prompt
            val prompt = buildPrompt(userQuery, intent, ragContext)

            // Step 5: Generate AI response
            Log.d(TAG, "Generating AI response...")
            val response = withContext(Dispatchers.IO) {
                model.generateContent(prompt)
            }

            responseText = response.text ?: "I couldn't generate a response. Please try again."
            Log.d(TAG, "Response generated (${responseText.length} chars)")

            // Return to UI
            withContext(Dispatchers.Main) {
                callback.onSuccess(responseText, intent, sources)
            }

            // Step 6: Extract insights asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                extractAndStoreInsights(userQuery, responseText)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in generateEnhancedResponse", e)
            withContext(Dispatchers.Main) {
                callback.onError("Error: ${e.message}")
            }
        }
    }

    // ==================== RAG RETRIEVAL ====================

    /**
     * Retrieve top 3 semantic chunks with profile priority
     */
    private suspend fun retrieveTopChunks(query: String): List<RAGChunk> = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = embeddingService.generateEmbedding(query)
            if (queryEmbedding.isEmpty()) {
                return@withContext emptyList()
            }

            val farmerId = getCurrentFarmerId()

            // Search all vectors
            val results = vectorDb.searchSimilar(
                queryEmbedding = queryEmbedding,
                topK = 10,
                minScore = MIN_SIMILARITY_SCORE
            )

            // Convert with boosting
            val chunks = results.map { result ->
                val metadata = result.metadata
                val type = metadata.optString("type", "general")
                val content = metadata.optString("content", "")
                val title = metadata.optString("title", "Knowledge")

                // Boost personalized chunks
                val score = if (result.farmerId == farmerId &&
                    (type == "farmer_profile" || type == "farmer_insights")) {
                    result.score * PROFILE_BOOST
                } else {
                    result.score
                }

                RAGChunk(
                    id = result.id,
                    type = type,
                    title = title,
                    content = content,
                    score = score,
                    isPersonalized = (result.farmerId == farmerId)
                )
            }

            // Ensure 1 profile chunk + fill rest
            val personalized = chunks.filter { it.isPersonalized }.sortedByDescending { it.score }
            val general = chunks.filter { !it.isPersonalized }.sortedByDescending { it.score }

            val final = mutableListOf<RAGChunk>()
            if (personalized.isNotEmpty()) {
                final.add(personalized.first()) // Mandatory profile chunk
                final.addAll(general.take(TOP_K - 1))
            } else {
                final.addAll(general.take(TOP_K))
            }

            final.sortedByDescending { it.score }.take(TOP_K)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving chunks", e)
            emptyList()
        }
    }

    /**
     * Build RAG context from chunks
     */
    private fun buildRAGContext(chunks: List<RAGChunk>): String {
        if (chunks.isEmpty()) return "No relevant information found."

        val sb = StringBuilder()
        sb.append("=== RELEVANT CONTEXT ===\n\n")

        chunks.forEachIndexed { index, chunk ->
            sb.append("${index + 1}. ")
            if (chunk.isPersonalized) sb.append("[FARMER INFO] ")
            sb.append("${chunk.title}\n")
            sb.append("${chunk.content}\n")
            sb.append("(Relevance: ${String.format(Locale.US, "%.2f", chunk.score)})\n\n")
        }

        return sb.toString()
    }

    /**
     * Build final prompt for Gemini
     */
    private fun buildPrompt(query: String, intent: QueryIntent, context: String): String {
        val season = contextManager.getCurrentSeason()
        val agriSeason = contextManager.getAgriculturalSeason()

        return """
            You are KrishiSakhi, an expert agricultural assistant for Indian farmers.
            
            SYSTEM INSTRUCTIONS:
            - Provide accurate, practical farming advice
            - Use the context to personalize responses
            - Reply in English only
            - if username is known, address them respectfully, else use "Dear Farmer"
            - Tailor advice to farmer's specific crops, soil, location if available
            - Keep responses concise (150-250 words)
            - Use simple language
            - Include specific numbers and timings
            
            QUERY TYPE: ${getIntentDescription(intent)}
            SEASON: $season ($agriSeason)
            
            $context
            
            USER QUESTION: $query
            
            ANSWER:
        """.trimIndent()
    }

    private fun getIntentDescription(intent: QueryIntent): String {
        return when (intent) {
            QueryIntent.WEATHER -> "Weather query"
            QueryIntent.PEST_DISEASE -> "Pest management"
            QueryIntent.CROP_CULTIVATION -> "Crop cultivation"
            QueryIntent.MARKET_PRICE -> "Market prices"
            QueryIntent.GOVT_SCHEME -> "Government schemes"
            QueryIntent.FERTILIZER -> "Fertilizer management"
            QueryIntent.IRRIGATION -> "Irrigation"
            QueryIntent.SOIL_HEALTH -> "Soil health"
            QueryIntent.GENERAL_FARMING -> "General farming"
            QueryIntent.UNKNOWN -> "General query"
        }
    }

    // ==================== INSIGHT EXTRACTION ====================

    private suspend fun extractAndStoreInsights(query: String, response: String) {
        try {
            Log.d(TAG, "Extracting insights...")

            val insights = insightsFileManager.extractNewInsightsFromUserQuery(query, response)

            if (insights.isEmpty()) {
                Log.d(TAG, "No new insights")
                return
            }

            var added = 0
            insights.forEach { insight ->
                if (insightsFileManager.appendInsight(insight)) added++
            }

            if (added > 0) {
                Log.d(TAG, "Added $added insights, updating vector DB...")
                insightsFileManager.updateVectorDB()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting insights", e)
        }
    }

    // ==================== PERSONALIZATION EMBEDDING ====================

    private suspend fun embedProfileFile(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Embedding profile...")

            val chunks = profileFileManager.getChunksForEmbedding()
            if (chunks.isEmpty()) return@withContext true

            deleteVectorsByType(farmerId, "farmer_profile")

            val vectors = chunks.mapIndexedNotNull { index, chunk ->
                val embedding = embeddingService.generateEmbedding(chunk)
                if (embedding.isEmpty()) return@mapIndexedNotNull null

                VectorInsertData(
                    id = "${farmerId}_profile_$index",
                    embedding = embedding,
                    metadata = mapOf(
                        "type" to "farmer_profile",
                        "title" to "Farmer Profile",
                        "content" to chunk,
                        "timestamp" to System.currentTimeMillis()
                    ),
                    farmerId = farmerId
                )
            }

            if (vectors.isNotEmpty()) {
                vectorDb.insertVectorsBatch(vectors)
                Log.d(TAG, "Embedded ${vectors.size} profile chunks")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding profile", e)
            false
        }
    }

    private suspend fun embedInsightsFile(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Embedding insights...")

            val content = insightsFileManager.readInsights()
            if (content.isEmpty()) return@withContext true

            deleteVectorsByType(farmerId, "farmer_insights")

            val chunks = chunkText(content)
            val vectors = chunks.mapIndexedNotNull { index, chunk ->
                val embedding = embeddingService.generateEmbedding(chunk)
                if (embedding.isEmpty()) return@mapIndexedNotNull null

                VectorInsertData(
                    id = "${farmerId}_insights_$index",
                    embedding = embedding,
                    metadata = mapOf(
                        "type" to "farmer_insights",
                        "title" to "Farmer Insights",
                        "content" to chunk,
                        "timestamp" to System.currentTimeMillis()
                    ),
                    farmerId = farmerId
                )
            }

            if (vectors.isNotEmpty()) {
                vectorDb.insertVectorsBatch(vectors)
                Log.d(TAG, "Embedded ${vectors.size} insights chunks")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error embedding insights", e)
            false
        }
    }

    private suspend fun deleteVectorsByType(farmerId: String, type: String) {
        try {
            val dao = com.krishisakhi.farmassistant.db.AppDatabase.getDatabase(context).vectorEntryDao()
            val vectors = dao.getVectorsByFarmerId(farmerId)
            vectors.filter { it.sourceType == type }.forEach { vectorDb.deleteVector(it.id) }
        } catch (@Suppress("SwallowedException") e: Exception) {
            Log.e(TAG, "Error deleting vectors", e)
        }
    }

    // ==================== UTILITIES ====================

    private fun chunkText(text: String, size: Int = 500, overlap: Int = 50): List<String> {
        if (text.length <= size) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val end = minOf(start + size, text.length)
            chunks.add(text.substring(start, end).trim())
            start += size - overlap
        }

        return chunks
    }

    private fun getCurrentFarmerId(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (@Suppress("SwallowedException") e: Exception) {
            null
        }
    }

    private data class RAGChunk(
        val id: String,
        val type: String,
        val title: String,
        val content: String,
        val score: Float,
        val isPersonalized: Boolean
    )

    // ==================== PUBLIC API ====================

    fun getInsightsFileManager() = insightsFileManager
    fun getProfileFileManager() = profileFileManager

    suspend fun reindexPersonalization(farmerId: String): Boolean {
        return embedProfileFile(farmerId) && embedInsightsFile(farmerId)
    }
}

