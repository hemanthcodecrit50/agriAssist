package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch

/**
 * Complete usage examples for InsightsFileManager
 * Demonstrates automatic insight extraction and vector DB integration
 */
class InsightsFileManagerExamples {

    companion object {
        private const val TAG = "InsightsExamples"
    }

    /**
     * Example 1: Initialize insights file
     */
    fun example1_InitializeInsights(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Create or load file
            val success = manager.createOrLoadInsightsFile()

            if (success) {
                Log.d(TAG, "Insights file ready at: ${manager.getFilePath()}")
            } else {
                Log.e(TAG, "Failed to create insights file")
            }
        }
    }

    /**
     * Example 2: Extract insights from user query
     */
    fun example2_ExtractInsights(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            val userQuery = "I grow tomatoes on 5 acres in Pune. I face pest problems in summer."
            val aiResponse = "Here's how to control tomato pests..."

            // Extract insights
            val insights = manager.extractNewInsightsFromUserQuery(userQuery, aiResponse)

            Log.d(TAG, "Extracted ${insights.size} insights:")
            insights.forEach { insight ->
                Log.d(TAG, "  - $insight")
            }

            // Expected output:
            // - Grows tomatoes on 5 acres in Pune
            // - Faces pest problems during summer months
        }
    }

    /**
     * Example 3: Append insight manually
     */
    fun example3_AppendInsight(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Append insight (will be formatted with timestamp)
            val insight = "Interested in organic farming methods"
            val success = manager.appendInsight(insight)

            if (success) {
                Log.d(TAG, "Insight appended successfully")

                // File now contains:
                // - [2025-12-10 14:30] Interested in organic farming methods
            } else {
                Log.d(TAG, "Insight not added (duplicate or error)")
            }
        }
    }

    /**
     * Example 4: Read all insights
     */
    fun example4_ReadInsights(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Read all insights
            val insights = manager.readInsights()

            if (insights.isNotEmpty()) {
                Log.d(TAG, "Farmer Insights:\n$insights")
            } else {
                Log.d(TAG, "No insights yet")
            }
        }
    }

    /**
     * Example 5: Update vector database
     */
    fun example5_UpdateVectorDB(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Update vector DB with latest insights
            val success = manager.updateVectorDB()

            if (success) {
                Log.d(TAG, "Vector DB updated with insights")
            } else {
                Log.e(TAG, "Failed to update vector DB")
            }
        }
    }

    /**
     * Example 6: Complete workflow - Extract, Append, Update
     */
    fun example6_CompleteWorkflow(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Step 1: Initialize
            Log.d(TAG, "Step 1: Initializing...")
            manager.createOrLoadInsightsFile()

            // Step 2: Extract insights from conversation
            Log.d(TAG, "Step 2: Extracting insights...")
            val query = "I grow wheat and rice on 10 acres. I need help with irrigation."
            val response = "Here are irrigation recommendations..."

            val newInsights = manager.extractNewInsightsFromUserQuery(query, response)

            // Step 3: Append each insight
            Log.d(TAG, "Step 3: Appending insights...")
            var addedCount = 0
            newInsights.forEach { insight ->
                val success = manager.appendInsight(insight)
                if (success) addedCount++
            }

            Log.d(TAG, "Added $addedCount insights")

            // Step 4: Update vector database
            if (addedCount > 0) {
                Log.d(TAG, "Step 4: Updating vector DB...")
                manager.updateVectorDB()
            }

            // Step 5: Read final insights
            Log.d(TAG, "Step 5: Reading final insights...")
            val allInsights = manager.readInsights()
            Log.d(TAG, "Total insights:\n$allInsights")
        }
    }

    /**
     * Example 7: Get insights statistics
     */
    fun example7_GetStatistics(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            val stats = manager.getInsightsStats()

            Log.d(TAG, """
                Insights Statistics:
                - Exists: ${stats.exists}
                - Count: ${stats.insightCount}
                - Size: ${stats.fileSizeBytes / 1024.0} KB
                - Path: ${stats.filePath}
            """.trimIndent())
        }
    }

    /**
     * Example 8: Duplicate detection test
     */
    fun example8_DuplicateDetection(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Append first insight
            val insight1 = "Grows tomatoes on 5 acres"
            val success1 = manager.appendInsight(insight1)
            Log.d(TAG, "First append: $success1") // Should be true

            // Try to append exact duplicate
            val success2 = manager.appendInsight(insight1)
            Log.d(TAG, "Exact duplicate: $success2") // Should be false

            // Try to append similar insight
            val insight2 = "Grows tomatoes on five acres"
            val success3 = manager.appendInsight(insight2)
            Log.d(TAG, "Similar insight: $success3") // Should be false (85% similar)

            // Append different insight
            val insight3 = "Faces water shortage in summer"
            val success4 = manager.appendInsight(insight3)
            Log.d(TAG, "Different insight: $success4") // Should be true
        }
    }

    /**
     * Example 9: Integration with EnhancedAIService
     */
    fun example9_IntegrationWithAIService(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val aiService = com.krishisakhi.farmassistant.rag.EnhancedAIService(context, apiKey)

            // Initialize
            aiService.initialize()

            // Process query - insights are extracted automatically!
            val userQuery = "I have 3 acres of land and want to grow organic vegetables"

            aiService.generateEnhancedResponse(
                userQuery,
                object : com.krishisakhi.farmassistant.rag.EnhancedAIService.EnhancedAICallback {
                    override fun onSuccess(
                        response: String,
                        intent: com.krishisakhi.farmassistant.classifier.QueryIntent,
                        sources: List<String>
                    ) {
                        Log.d(TAG, "Response: $response")
                        // Insights are being extracted in background automatically!
                    }

                    override fun onError(error: String) {
                        Log.e(TAG, "Error: $error")
                    }
                }
            )

            // Wait a few seconds, then check insights file
            kotlinx.coroutines.delay(5000)

            val insightsManager = aiService.getInsightsFileManager()
            val insights = insightsManager.readInsights()
            Log.d(TAG, "Auto-extracted insights:\n$insights")
        }
    }

    /**
     * Example 10: Clear insights (for testing)
     */
    fun example10_ClearInsights(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        apiKey: String
    ) {
        lifecycleScope.launch {
            val manager = InsightsFileManager(context, apiKey)

            // Clear all insights
            val success = manager.clearInsights()

            if (success) {
                Log.d(TAG, "All insights cleared")

                // Verify
                val hasInsights = manager.hasInsights()
                Log.d(TAG, "Has insights after clear: $hasInsights") // Should be false
            }
        }
    }
}

/**
 * Quick reference for common operations
 */
object InsightsQuickReference {

    /**
     * Initialize insights file
     */
    suspend fun initialize(context: Context, apiKey: String): Boolean {
        val manager = InsightsFileManager(context, apiKey)
        return manager.createOrLoadInsightsFile()
    }

    /**
     * Extract and store insights automatically
     */
    suspend fun extractAndStore(
        context: Context,
        apiKey: String,
        query: String,
        response: String
    ): Int {
        val manager = InsightsFileManager(context, apiKey)

        // Extract insights
        val insights = manager.extractNewInsightsFromUserQuery(query, response)

        // Append each insight
        var count = 0
        insights.forEach { insight ->
            val success = manager.appendInsight(insight)
            if (success) count++
        }

        // Update vector DB if any insights added
        if (count > 0) {
            manager.updateVectorDB()
        }

        return count
    }

    /**
     * Read all insights
     */
    suspend fun readAll(context: Context, apiKey: String): String {
        val manager = InsightsFileManager(context, apiKey)
        return manager.readInsights()
    }

    /**
     * Get insights count
     */
    suspend fun getCount(context: Context, apiKey: String): Int {
        val manager = InsightsFileManager(context, apiKey)
        val stats = manager.getInsightsStats()
        return stats.insightCount
    }

    /**
     * Clear all insights
     */
    suspend fun clearAll(context: Context, apiKey: String): Boolean {
        val manager = InsightsFileManager(context, apiKey)
        return manager.clearInsights()
    }
}

/**
 * Testing utilities
 */
class InsightsTestingUtils(
    private val context: Context,
    private val apiKey: String
) {
    private val manager = InsightsFileManager(context, apiKey)

    /**
     * Add sample insights for testing
     */
    suspend fun addSampleInsights() {
        val samples = listOf(
            "Grows tomatoes on 5 acres in Pune",
            "Has black cotton soil",
            "Faces water shortage in summer",
            "Interested in organic farming",
            "Uses drip irrigation system",
            "Attends agricultural workshops",
            "Plans to expand cultivation",
            "Faces pest issues with tomatoes"
        )

        samples.forEach { insight ->
            manager.appendInsight(insight)
        }

        Log.d("InsightsTestingUtils", "Added ${samples.size} sample insights")
    }

    /**
     * Test duplicate detection
     */
    suspend fun testDuplicateDetection() {
        val testCases = listOf(
            "Grows tomatoes" to "Grows tomatoes", // Exact match
            "Grows tomatoes" to "Grows tomato", // Very similar
            "Grows tomatoes" to "Grows potatoes", // Different
            "5 acres" to "five acres", // Semantic match
        )

        testCases.forEach { (insight1, insight2) ->
            manager.clearInsights()

            val success1 = manager.appendInsight(insight1)
            val success2 = manager.appendInsight(insight2)

            Log.d("InsightsTestingUtils",
                "Test: '$insight1' vs '$insight2' => $success1, $success2")
        }
    }

    /**
     * Test insight extraction quality
     */
    suspend fun testExtractionQuality() {
        val testQueries = mapOf(
            "I grow wheat on 10 acres" to listOf("Grows wheat on 10 acres"),
            "I'm from Pune, Maharashtra" to listOf("Located in Pune, Maharashtra"),
            "I have pest problems" to listOf("Faces pest issues"),
            "What is the weather?" to emptyList() // Should not extract
        )

        testQueries.forEach { (query, expected) ->
            val insights = manager.extractNewInsightsFromUserQuery(query, "Response...")

            Log.d("InsightsTestingUtils",
                "Query: '$query' => Extracted: $insights (Expected: $expected)")
        }
    }
}

