package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.rag.EnhancedAIService
import kotlinx.coroutines.launch

/**
 * Usage examples for the two-file personalization system
 * This file demonstrates how to use the personalization features
 */
class PersonalizationUsageExamples {

    companion object {
        private const val TAG = "PersonalizationExamples"
    }

    /**
     * Example 1: Initialize personalization for a new farmer
     * Call this when a farmer first signs up or logs in
     */
    fun example1_InitializePersonalization(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val initializer = PersonalizationInitializer(context)

            // Initialize for current logged-in farmer
            val success = initializer.initializeForCurrentFarmer()

            if (success) {
                Log.d(TAG, "Personalization initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize personalization")
            }
        }
    }

    /**
     * Example 2: Edit farmer profile
     * Let the user write free-form text about themselves
     */
    fun example2_EditFarmerProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val fileManager = FarmerFileManager(context)
            val embeddingManager = PersonalizationEmbeddingManager(context)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // User types their profile content
            val profileContent = """
                I am Ramesh Kumar from Pune, Maharashtra.
                I grow tomatoes, onions, and wheat on 5 acres.
                My soil is black cotton soil.
                I face water shortage problems in summer.
                I want to learn organic farming methods.
            """.trimIndent()

            // Save profile
            val saved = fileManager.writeProfile(farmerId, profileContent)

            if (saved) {
                Log.d(TAG, "Profile saved successfully")

                // Re-embed both files (profile + insights)
                val indexed = embeddingManager.indexPersonalizationFiles(farmerId)

                if (indexed) {
                    Log.d(TAG, "Profile indexed into vector database")
                } else {
                    Log.e(TAG, "Failed to index profile")
                }
            }
        }
    }

    /**
     * Example 3: Read farmer profile
     * Display the user's profile text
     */
    fun example3_ReadFarmerProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val fileManager = FarmerFileManager(context)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Read profile content
            val profileContent = fileManager.readProfile(farmerId)

            if (profileContent.isNotEmpty()) {
                Log.d(TAG, "Farmer Profile:\n$profileContent")
            } else {
                Log.d(TAG, "No profile found")
            }
        }
    }

    /**
     * Example 4: View AI-generated insights
     * Display insights that the AI has learned about the farmer
     */
    fun example4_ViewInsights(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val fileManager = FarmerFileManager(context)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Read insights content
            val insightsContent = fileManager.readInsights(farmerId)

            if (insightsContent.isNotEmpty()) {
                Log.d(TAG, "AI-Generated Insights:\n$insightsContent")
            } else {
                Log.d(TAG, "No insights yet. Keep asking questions!")
            }
        }
    }

    /**
     * Example 5: Use Enhanced AI Service with personalization
     * Process user query with automatic insight extraction
     */
    fun example5_QueryWithPersonalization(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val enhancedAIService = EnhancedAIService(context, apiKey)

            // Initialize service
            enhancedAIService.initialize()

            // Process user query (stateless, no conversation history)
            val userQuery = "How do I control tomato pests?"

            enhancedAIService.generateEnhancedResponse(
                userQuery,
                object : EnhancedAIService.EnhancedAICallback {
                    override fun onSuccess(
                        response: String,
                        intent: com.krishisakhi.farmassistant.classifier.QueryIntent,
                        sources: List<String>
                    ) {
                        Log.d(TAG, "AI Response: $response")
                        Log.d(TAG, "Intent: $intent")
                        Log.d(TAG, "Sources: $sources")

                        // Insight extraction happens automatically in the background
                        // Check farmer_insights.txt after a few seconds to see new insights
                    }

                    override fun onError(error: String) {
                        Log.e(TAG, "Error: $error")
                    }
                }
            )
        }
    }

    /**
     * Example 6: Manually extract insight from a query
     * This happens automatically in EnhancedAIService, but you can also do it manually
     */
    fun example6_ManualInsightExtraction(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val insightGenerator = InsightGenerationService(context, apiKey)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            val userQuery = "I grow wheat and rice on 10 acres"
            val aiResponse = "Here's advice for wheat and rice cultivation..."

            // Extract and store insight
            val insight = insightGenerator.extractAndStoreInsight(
                farmerId = farmerId,
                userQuery = userQuery,
                aiResponse = aiResponse
            )

            if (insight != null) {
                Log.d(TAG, "New insight extracted: $insight")

                // Re-embed insights file
                val embeddingManager = PersonalizationEmbeddingManager(context)
                embeddingManager.updateInsightsEmbeddings(farmerId)
            } else {
                Log.d(TAG, "No meaningful insight extracted")
            }
        }
    }

    /**
     * Example 7: Get combined personalization data
     * Read both profile and insights together
     */
    fun example7_GetCombinedData(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val fileManager = FarmerFileManager(context)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Get both files
            val data = fileManager.readAllPersonalizationData(farmerId)

            if (!data.isEmpty()) {
                val combined = data.getCombinedContent()
                Log.d(TAG, "Combined Personalization Data:\n$combined")
            } else {
                Log.d(TAG, "No personalization data found")
            }
        }
    }

    /**
     * Example 8: Re-index personalization files
     * Call this if you manually edit files or need to refresh embeddings
     */
    fun example8_ReindexFiles(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val embeddingManager = PersonalizationEmbeddingManager(context)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Re-index both files
            val success = embeddingManager.indexPersonalizationFiles(farmerId)

            if (success) {
                Log.d(TAG, "Files re-indexed successfully")
            } else {
                Log.e(TAG, "Failed to re-index files")
            }
        }
    }

    /**
     * Example 9: Delete all personalization data
     * Use with caution - this removes all files and vectors
     */
    fun example9_DeleteAllData(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val fileManager = FarmerFileManager(context)
            val embeddingManager = PersonalizationEmbeddingManager(context)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Delete vectors
            embeddingManager.deletePersonalizationVectors(farmerId)

            // Delete files
            fileManager.deleteAllFiles(farmerId)

            Log.d(TAG, "All personalization data deleted")
        }
    }

    /**
     * Example 10: Generate insight summary
     * Consolidate all insights into a brief summary
     */
    fun example10_GenerateInsightSummary(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val insightGenerator = InsightGenerationService(context, apiKey)
            val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Generate summary
            val summary = insightGenerator.generateInsightSummary(farmerId)

            if (summary != null) {
                Log.d(TAG, "Insight Summary:\n$summary")
            } else {
                Log.d(TAG, "No insights to summarize")
            }
        }
    }
}

/**
 * Quick reference for common operations
 */
object PersonalizationQuickReference {

    /**
     * Initialize personalization (call once on login/signup)
     */
    suspend fun initialize(context: Context): Boolean {
        return PersonalizationInitializer(context).initializeForCurrentFarmer()
    }

    /**
     * Save farmer profile
     */
    suspend fun saveProfile(context: Context, content: String): Boolean {
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val saved = FarmerFileManager(context).writeProfile(farmerId, content)
        if (saved) {
            PersonalizationEmbeddingManager(context).indexPersonalizationFiles(farmerId)
        }
        return saved
    }

    /**
     * Read farmer profile
     */
    suspend fun readProfile(context: Context): String {
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return ""
        return FarmerFileManager(context).readProfile(farmerId)
    }

    /**
     * Read AI insights
     */
    suspend fun readInsights(context: Context): String {
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return ""
        return FarmerFileManager(context).readInsights(farmerId)
    }

    /**
     * Process query with personalization (use EnhancedAIService)
     */
    suspend fun processQuery(
        context: Context,
        apiKey: String,
        query: String,
        callback: EnhancedAIService.EnhancedAICallback
    ) {
        val service = EnhancedAIService(context, apiKey)
        service.initialize()
        service.generateEnhancedResponse(query, callback)
    }
}

