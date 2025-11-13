package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.krishisakhi.farmassistant.classifier.IntentClassifier
import com.krishisakhi.farmassistant.classifier.QueryIntent
import com.krishisakhi.farmassistant.utils.ContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Enhanced AI service that combines RAG, intent classification, and Gemini AI
 */
class EnhancedAIService(private val context: Context, private val apiKey: String) {

    companion object {
        private const val TAG = "EnhancedAIService"
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private val knowledgeBaseManager = KnowledgeBaseManager(context)
    private val intentClassifier = IntentClassifier(context)
    private val contextManager = ContextManager(context)

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
            knowledgeBaseManager.initializeKnowledgeBase()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Enhanced AI Service", e)
            false
        }
    }

    /**
     * Generate enhanced response using RAG and intent classification
     */
    suspend fun generateEnhancedResponse(userQuery: String, callback: EnhancedAICallback) {
        try {
            Log.d(TAG, "Processing query: $userQuery")

            // Step 1: Classify intent
            val intentResult = intentClassifier.classify(userQuery)
            val intent = intentResult.intent
            val confidence = intentResult.confidence

            Log.d(TAG, "Classified intent: $intent (confidence: $confidence)")

            // Step 2: Search knowledge base
            val categoryFilter = intentClassifier.getCategoryForIntent(intent)
            val searchResults = knowledgeBaseManager.search(
                query = userQuery,
                topK = 3,
                categoryFilter = categoryFilter
            )

            Log.d(TAG, "Found ${searchResults.size} relevant documents")

            // Step 3: Build context from search results
            val context = buildContext(searchResults)
            val sources = searchResults.map { it.document.title }

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
     * Build context from search results
     */
    private fun buildContext(searchResults: List<SearchResult>): String {
        if (searchResults.isEmpty()) {
            return "No specific information found in local knowledge base."
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Relevant information from knowledge base:\n\n")

        for ((index, result) in searchResults.withIndex()) {
            contextBuilder.append("${index + 1}. ${result.document.title}\n")
            contextBuilder.append("${result.document.content}\n")
            contextBuilder.append("(Relevance: ${String.format(Locale.US, "%.2f", result.score)})\n\n")
        }

        return contextBuilder.toString()
    }

    /**
     * Build enhanced prompt with RAG context
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
            1. Use the information from the knowledge base above to answer the question accurately
            2. Consider the current season and regional context when providing advice
            3. Provide practical, actionable advice that farmers can implement
            4. Keep the answer concise (150-200 words maximum)
            5. If the knowledge base information is relevant, incorporate it naturally
            6. If additional clarification is needed, provide it based on general agricultural knowledge
            7. Use simple language that farmers can understand
            8. Include specific numbers, dosages, or timings when relevant
            
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
