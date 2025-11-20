package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.classifier.IntentClassifier
import com.krishisakhi.farmassistant.classifier.QueryIntent
import com.krishisakhi.farmassistant.utils.ContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/**
 * Enhanced AI service that combines RAG, intent classification, and Gemini AI
 * with multi-source vector retrieval and personalized context prioritization
 */
class EnhancedAIService(private val context: Context, private val apiKey: String) {

    companion object {
        private const val TAG = "EnhancedAIService"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val DEFAULT_TOP_K = 5
        private const val MIN_SIMILARITY_SCORE = 0.3f
    }

    private val knowledgeBaseManager = KnowledgeBaseManager(context)
    private val intentClassifier = IntentClassifier(context)
    private val contextManager = ContextManager(context)
    private val vectorDb = VectorDatabasePersistent(context)
    private val embeddingService = EmbeddingService(context)

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

    interface EnhancedAICallback {
        fun onSuccess(response: String, intent: QueryIntent, sources: List<String>)
        fun onError(error: String)
    }

    /**
     * Initialize the knowledge base (call this once at app startup)
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing Enhanced AI Service...")

            // Initialize vector database
            vectorDb.initialize()

            // Initialize knowledge base
            knowledgeBaseManager.initializeKnowledgeBase()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Enhanced AI Service", e)
            false
        }
    }

    /**
     * Generate enhanced response using RAG and intent classification
     * with multi-source vector retrieval
     */
    suspend fun generateEnhancedResponse(userQuery: String, callback: EnhancedAICallback) {
        try {
            Log.d(TAG, "Processing query: $userQuery")

            // Step 1: Classify intent
            val intentResult = intentClassifier.classify(userQuery)
            val intent = intentResult.intent
            val confidence = intentResult.confidence

            Log.d(TAG, "Classified intent: $intent (confidence: $confidence)")

            // Step 2: Retrieve and merge vectors from multiple sources
            val mergedResults = retrieveAndMergeVectors(userQuery)

            Log.d(TAG, "Retrieved ${mergedResults.size} total vectors (personalized + general)")

            // Step 3: Build context from merged results (personalized first)
            val context = buildEnhancedContext(mergedResults)
            val sources = mergedResults.map { it.title }

            // Step 4: Generate response with Gemini using RAG context
            val enhancedPrompt = buildEnhancedPrompt(userQuery, intent, context)

            Log.d(TAG, "Sending enhanced prompt to Gemini AI")

            val response = withContext(Dispatchers.IO) {
                model.generateContent(enhancedPrompt)
            }

            val responseText = response.text ?: "I couldn't generate a response. Please try again."
            Log.d(TAG, "Received response from Gemini AI")

            withContext(Dispatchers.Main) {
                callback.onSuccess(responseText, intent, sources)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating enhanced response", e)
            withContext(Dispatchers.Main) {
                callback.onError("Error: ${e.message}")
            }
        }
    }

    /**
     * Retrieve and merge vectors from multiple sources:
     * 1. Farmer profile vectors (personalized)
     * 2. General knowledge vectors
     * Returns merged results sorted by similarity score
     */
    private suspend fun retrieveAndMergeVectors(query: String): List<EnhancedSearchResult> = withContext(Dispatchers.IO) {
        try {
            // Generate query embedding once
            val queryEmbedding = embeddingService.generateEmbedding(query)

            if (queryEmbedding == null) {
                Log.e(TAG, "Failed to generate query embedding")
                return@withContext emptyList()
            }

            // Get current farmer's phone number
            val farmerId = getCurrentFarmerId()

            // Parallel retrieval from different sources
            val personalizedResultsDeferred = async {
                retrievePersonalizedVectors(queryEmbedding, farmerId)
            }

            val generalResultsDeferred = async {
                retrieveGeneralVectors(queryEmbedding)
            }

            // Wait for both results
            val personalizedResults = personalizedResultsDeferred.await()
            val generalResults = generalResultsDeferred.await()

            Log.d(TAG, "Found ${personalizedResults.size} personalized vectors")
            Log.d(TAG, "Found ${generalResults.size} general knowledge vectors")

            // Merge and sort by similarity score (personalized get slight boost)
            mergeAndSortResults(personalizedResults, generalResults)

        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving and merging vectors", e)
            emptyList()
        }
    }

    /**
     * Retrieve personalized farmer profile vectors
     */
    private suspend fun retrievePersonalizedVectors(
        queryEmbedding: FloatArray,
        farmerId: String?
    ): List<EnhancedSearchResult> = withContext(Dispatchers.IO) {
        try {
            if (farmerId == null) {
                Log.d(TAG, "No farmer ID available, skipping personalized vectors")
                return@withContext emptyList()
            }

            // Search for farmer-specific vectors
            val results = vectorDb.searchSimilar(
                queryEmbedding = queryEmbedding,
                topK = 2, // Get top 2 personalized results
                minScore = MIN_SIMILARITY_SCORE,
                farmerIdFilter = farmerId
            )

            // Convert to EnhancedSearchResult
            results.mapNotNull { result ->
                try {
                    val metadata = result.metadata
                    EnhancedSearchResult(
                        id = result.id,
                        title = metadata.optString("name", "Farmer Profile"),
                        content = extractContentFromMetadata(metadata),
                        type = "farmer_profile",
                        score = result.score,
                        farmerId = result.farmerId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing personalized vector result", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving personalized vectors", e)
            emptyList()
        }
    }

    /**
     * Retrieve general knowledge vectors
     */
    private suspend fun retrieveGeneralVectors(
        queryEmbedding: FloatArray
    ): List<EnhancedSearchResult> = withContext(Dispatchers.IO) {
        try {
            // Search for general knowledge vectors (farmerId = null)
            val results = vectorDb.searchSimilar(
                queryEmbedding = queryEmbedding,
                topK = DEFAULT_TOP_K, // Get top 5 general results
                minScore = MIN_SIMILARITY_SCORE,
                farmerIdFilter = null // No filter to get all including general
            )

            // Filter to only general knowledge (where type != farmer_profile)
            // and convert to EnhancedSearchResult
            results.mapNotNull { result ->
                try {
                    val metadata = result.metadata
                    val type = metadata.optString("type", "general")

                    // Skip farmer profile vectors (we get those separately)
                    if (type == "farmer_profile") {
                        return@mapNotNull null
                    }

                    EnhancedSearchResult(
                        id = result.id,
                        title = metadata.optString("title", "Knowledge Entry"),
                        content = metadata.optString("content", ""),
                        type = type,
                        score = result.score,
                        farmerId = null
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing general vector result", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving general vectors", e)
            emptyList()
        }
    }

    /**
     * Merge and sort results by similarity score
     * Personalized results get a slight boost to prioritize them
     */
    private fun mergeAndSortResults(
        personalizedResults: List<EnhancedSearchResult>,
        generalResults: List<EnhancedSearchResult>
    ): List<EnhancedSearchResult> {
        // Give personalized results a 10% boost in score for prioritization
        val boostedPersonalized = personalizedResults.map { result ->
            result.copy(score = result.score * 1.1f)
        }

        // Combine all results
        val allResults = boostedPersonalized + generalResults

        // Sort by score descending and limit to top K
        return allResults
            .sortedByDescending { it.score }
            .take(DEFAULT_TOP_K)
    }

    /**
     * Get current farmer's ID (normalized phone number)
     */
    private fun getCurrentFarmerId(): String? {
        return try {
            val auth = FirebaseAuth.getInstance()
            val phoneRaw = auth.currentUser?.phoneNumber
            normalizePhoneToDigits(phoneRaw)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current farmer ID", e)
            null
        }
    }

    /**
     * Normalize phone number to digits-only format
     */
    private fun normalizePhoneToDigits(phone: String?): String? {
        if (phone == null) return null
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length >= 11 -> digits
            else -> null
        }
    }

    /**
     * Extract content from farmer profile metadata
     */
    private fun extractContentFromMetadata(metadata: JSONObject): String {
        return buildString {
            append("Farmer: ${metadata.optString("name", "Unknown")}\n")
            append("Location: ${metadata.optString("village", "")}, ")
            append("${metadata.optString("district", "")}, ")
            append("${metadata.optString("state", "")}\n")
            append("Land Size: ${metadata.optString("landSize", "")} hectares\n")
            append("Soil Type: ${metadata.optString("soilType", "")}\n")
            append("Primary Crops: ${metadata.optString("crops", "").replace(",", ", ")}\n")
            append("Language: ${metadata.optString("language", "")}")
        }
    }

    /**
     * Data class for enhanced search results
     */
    private data class EnhancedSearchResult(
        val id: String,
        val title: String,
        val content: String,
        val type: String,
        val score: Float,
        val farmerId: String?
    )

    /**
     * Build enhanced context from merged search results
     * Places personalized context FIRST, then general knowledge
     */
    private fun buildEnhancedContext(searchResults: List<EnhancedSearchResult>): String {
        if (searchResults.isEmpty()) {
            return "No specific information found in knowledge base."
        }

        val contextBuilder = StringBuilder()

        // Separate personalized and general results
        val personalizedResults = searchResults.filter { it.type == "farmer_profile" }
        val generalResults = searchResults.filter { it.type != "farmer_profile" }

        // Add personalized context FIRST
        if (personalizedResults.isNotEmpty()) {
            contextBuilder.append("=== FARMER PROFILE (Personalized Context) ===\n\n")

            for ((index, result) in personalizedResults.withIndex()) {
                contextBuilder.append("${index + 1}. ${result.title}\n")
                contextBuilder.append("${result.content}\n")
                contextBuilder.append("(Relevance: ${String.format(Locale.US, "%.2f", result.score)})\n\n")
            }
        }

        // Add general knowledge context SECOND
        if (generalResults.isNotEmpty()) {
            contextBuilder.append("=== GENERAL AGRICULTURAL KNOWLEDGE ===\n\n")

            for ((index, result) in generalResults.withIndex()) {
                contextBuilder.append("${index + 1}. ${result.title}\n")
                contextBuilder.append("${result.content}\n")
                contextBuilder.append("(Relevance: ${String.format(Locale.US, "%.2f", result.score)})\n\n")
            }
        }

        return contextBuilder.toString()
    }

    /**
     * Build enhanced prompt with RAG context
     * Prioritizes personalized farmer context for tailored responses
     */
    private fun buildEnhancedPrompt(query: String, intent: QueryIntent, context: String): String {
        val intentDescription = getIntentDescription(intent)

        // Add seasonal and regional context
        val season = contextManager.getCurrentSeason()
        val agriSeason = contextManager.getAgriculturalSeason()
        val seasonalCrops = contextManager.getSeasonalCrops()

        val contextualInfo = buildString {
            append("Current Season: $season\n")
            append("Agricultural Season: $agriSeason\n")
            append("Seasonal Crops: ${seasonalCrops.joinToString(", ")}\n\n")
        }

        return """
            You are KrishiSakhi, an expert agricultural assistant for Indian farmers.
            
            Query Type: $intentDescription
            
            $contextualInfo
            
            $context
            
            User Question: $query
            
            Instructions:
            1. PRIORITIZE personalized farmer profile information when available - tailor your response to their specific crops, soil type, land size, and location
            2. Use general agricultural knowledge to supplement and provide comprehensive advice
            3. Consider the current season and regional context when providing recommendations
            4. Provide practical, actionable advice that the farmer can implement immediately
            5. Keep the answer concise but informative (150-250 words)
            6. If the farmer's profile shows specific crops, focus advice on those crops
            7. If the farmer's location is mentioned, consider regional agricultural practices
            8. Use simple language appropriate for the farmer's language preference
            9. Include specific numbers, dosages, timings, or measurements when relevant
            10. If personalized context is available but general knowledge is more relevant for this query, blend both appropriately
            
            Answer:
        """.trimIndent()
    }

    /**
     * Get description for intent type
     */
    private fun getIntentDescription(intent: QueryIntent): String {
        return when (intent) {
            QueryIntent.WEATHER -> "Weather and climate related query"
            QueryIntent.PEST_DISEASE -> "Pest and disease management query"
            QueryIntent.CROP_CULTIVATION -> "Crop cultivation and growing techniques"
            QueryIntent.MARKET_PRICE -> "Market prices and selling information"
            QueryIntent.GOVT_SCHEME -> "Government schemes and subsidies"
            QueryIntent.FERTILIZER -> "Fertilizer and nutrient management"
            QueryIntent.IRRIGATION -> "Irrigation and water management"
            QueryIntent.SOIL_HEALTH -> "Soil health and testing"
            QueryIntent.GENERAL_FARMING -> "General farming question"
            QueryIntent.UNKNOWN -> "General agricultural query"
        }
    }

    /**
     * Add new knowledge to the knowledge base
     */
    suspend fun addKnowledge(
        title: String,
        content: String,
        category: String,
        tags: List<String>
    ): Boolean {
        return knowledgeBaseManager.addDocument(title, content, category, tags)
    }

    /**
     * Search knowledge base directly
     */
    suspend fun searchKnowledge(query: String, topK: Int = 5): List<SearchResult> {
        return knowledgeBaseManager.search(query, topK)
    }
}
