package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates AI insights from user queries and conversations
 * Extracts meaningful, stable personal information to append to farmer_insights.txt
 */
class InsightGenerationService(private val context: Context, private val apiKey: String) {

    companion object {
        private const val TAG = "InsightGenerationService"
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private val model = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.3f // Lower temperature for more factual extraction
            topK = 20
            topP = 0.9f
            maxOutputTokens = 500
        }
    )

    private val fileManager = FarmerFileManager(context)

    /**
     * Extract insights from a user query and optionally append to farmer_insights.txt
     * This is called after each user query
     *
     * @param farmerId The farmer's unique ID
     * @param userQuery The user's question
     * @param aiResponse The AI's response (optional, for better context)
     * @return The extracted insight text (empty if no meaningful insight found)
     */
    suspend fun extractAndStoreInsight(
        farmerId: String,
        userQuery: String,
        aiResponse: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extracting insight from query: ${userQuery.take(50)}...")

            // Read existing insights to avoid duplication
            val existingInsights = fileManager.readInsights(farmerId)

            // Build prompt for insight extraction
            val prompt = buildInsightExtractionPrompt(userQuery, aiResponse, existingInsights)

            // Generate insight
            val response = model.generateContent(prompt)
            val insightText = response.text?.trim() ?: ""

            // Check if insight is meaningful
            if (insightText.isBlank() ||
                insightText.lowercase().contains("no new insight") ||
                insightText.lowercase().contains("no meaningful") ||
                insightText.length < 10) {
                Log.d(TAG, "No meaningful insight extracted from query")
                return@withContext null
            }

            // Clean up the insight
            val cleanedInsight = cleanInsightText(insightText)

            // Append to file
            val success = fileManager.appendInsight(farmerId, cleanedInsight)
            if (success) {
                Log.d(TAG, "Stored new insight: ${cleanedInsight.take(50)}...")
                return@withContext cleanedInsight
            } else {
                Log.e(TAG, "Failed to store insight")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting insight", e)
            null
        }
    }

    /**
     * Build prompt for extracting meaningful insights
     */
    private fun buildInsightExtractionPrompt(
        userQuery: String,
        aiResponse: String?,
        existingInsights: String
    ): String {
        return """
            You are an AI assistant that extracts meaningful personal information from farmer queries.
            
            TASK: Extract ONLY stable, meaningful personal information from the user's question that could be useful for future personalized responses.
            
            User Question: "$userQuery"
            ${if (aiResponse != null) "AI Response: \"$aiResponse\"" else ""}
            
            Existing Insights (DO NOT REPEAT):
            $existingInsights
            
            RULES:
            1. Extract ONLY factual, stable information about the farmer (crops they grow, land size, location, problems they face, etc.)
            2. Extract ONLY information explicitly mentioned or strongly implied in the question
            3. Format as a single concise bullet point (no bullet symbol, just the text)
            4. Be specific and actionable
            5. DO NOT repeat information already in existing insights
            6. DO NOT extract general farming questions or temporary queries
            7. If no meaningful personal information is found, respond with exactly: "No new insight"
            
            EXAMPLES OF GOOD INSIGHTS:
            - "Grows tomatoes on 2 acres and faces frequent pest issues"
            - "Has drip irrigation system installed in May 2024"
            - "Located in drought-prone area of Maharashtra"
            - "Interested in organic farming methods"
            - "Faces water shortage during summer months"
            
            EXAMPLES OF BAD INSIGHTS (do NOT extract):
            - "Asked about weather" (too general)
            - "Wants to know fertilizer prices" (temporary query)
            - "Curious about crop rotation" (general interest, not personal)
            
            Extract ONE concise insight or respond with "No new insight":
        """.trimIndent()
    }

    /**
     * Clean and normalize insight text
     */
    private fun cleanInsightText(text: String): String {
        return text
            .replace("- ", "") // Remove bullet points
            .replace("• ", "") // Remove bullet points
            .replace("* ", "") // Remove asterisks
            .replace(Regex("^[0-9]+\\.\\s*"), "") // Remove numbered lists
            .trim()
    }

    /**
     * Generate a comprehensive summary of all insights for a farmer
     * This can be called periodically to consolidate insights
     */
    suspend fun generateInsightSummary(farmerId: String): String? = withContext(Dispatchers.IO) {
        try {
            val insights = fileManager.readInsights(farmerId)
            if (insights.isEmpty()) {
                return@withContext null
            }

            val prompt = """
                Summarize the following farmer insights into 3-5 key points about this farmer:
                
                $insights
                
                Format each point as a concise bullet point (one line each).
                Focus on stable, actionable information.
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating insight summary", e)
            null
        }
    }
}

