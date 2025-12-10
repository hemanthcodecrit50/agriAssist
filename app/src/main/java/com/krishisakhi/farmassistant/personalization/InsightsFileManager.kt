package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.rag.EmbeddingService
import com.krishisakhi.farmassistant.rag.VectorDatabasePersistent
import com.krishisakhi.farmassistant.rag.VectorInsertData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages the farmer_insights.txt file for AI-generated insights
 * This file is append-only and stores learned information about the farmer
 *
 * Features:
 * - Automatic insight extraction from user queries
 * - Duplicate detection (exact match and similarity)
 * - Structured bullet-point format
 * - Timestamped entries
 * - Automatic vector DB re-embedding
 *
 * File Format:
 * ```
 * - [2025-12-10 14:30] Grows tomatoes on 5 acres
 * - [2025-12-10 15:45] Has drip irrigation system
 * - [2025-12-10 16:20] Faces pest issues in summer
 * ```
 *
 * Usage:
 * ```
 * val manager = InsightsFileManager(context, apiKey)
 * manager.createOrLoadInsightsFile()
 * val insights = manager.extractNewInsightsFromUserQuery(query, response)
 * insights.forEach { manager.appendInsight(it) }
 * manager.updateVectorDB()
 * ```
 */
class InsightsFileManager(
    private val context: Context,
    private val apiKey: String
) {

    companion object {
        private const val TAG = "InsightsFileManager"
        private const val INSIGHTS_FILE_NAME = "farmer_insights.txt"
        private const val ENCODING = "UTF-8"
        private const val MODEL_NAME = "gemini-2.0-flash-exp"

        // Chunking parameters for embedding
        private const val CHUNK_SIZE = 500
        private const val CHUNK_OVERLAP = 50

        // Similarity threshold for duplicate detection (0.0 to 1.0)
        private const val SIMILARITY_THRESHOLD = 0.85f
    }

    private val embeddingService = EmbeddingService(context)
    private val vectorDb = VectorDatabasePersistent(context)

    private val model = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.3f // Low temperature for factual extraction
            topK = 20
            topP = 0.9f
            maxOutputTokens = 500
        }
    )

    /**
     * Get the insights file reference
     */
    private fun getInsightsFile(): File {
        return File(context.filesDir, INSIGHTS_FILE_NAME)
    }

    /**
     * Get the absolute file path
     */
    fun getFilePath(): String {
        return getInsightsFile().absolutePath
    }

    /**
     * Create or load the insights file
     * Creates an empty file if it doesn't exist
     *
     * @return True if file is ready, false on error
     */
    suspend fun createOrLoadInsightsFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile()

            if (!file.exists()) {
                val created = file.createNewFile()
                if (created) {
                    Log.d(TAG, "Created new insights file at: ${file.absolutePath}")
                } else {
                    Log.e(TAG, "Failed to create insights file")
                    return@withContext false
                }
            } else {
                Log.d(TAG, "Insights file already exists at: ${file.absolutePath}")
            }

            // Verify file permissions
            if (!file.canRead() || !file.canWrite()) {
                Log.e(TAG, "Insights file permissions error")
                return@withContext false
            }

            true
        } catch (e: IOException) {
            Log.e(TAG, "Error creating/loading insights file", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing insights file", e)
            false
        }
    }

    /**
     * Read all insights from the file
     *
     * @return Insights content as UTF-8 string (never null)
     */
    suspend fun readInsights(): String = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile()

            if (!file.exists()) {
                Log.d(TAG, "Insights file does not exist, returning empty string")
                return@withContext ""
            }

            if (file.length() == 0L) {
                Log.d(TAG, "Insights file is empty")
                return@withContext ""
            }

            FileInputStream(file).use { fis ->
                val content = String(fis.readBytes(), Charsets.UTF_8)
                Log.d(TAG, "Read insights: ${content.lines().size} lines")
                content
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading insights file", e)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error reading insights", e)
            ""
        }
    }

    /**
     * Append a new insight to the file
     * Automatically adds timestamp and bullet point format
     * Checks for duplicates before appending
     *
     * Format: "- [2025-12-10 14:30] Insight text"
     *
     * @param text The insight text (without timestamp or bullet)
     * @return True if appended successfully, false if duplicate or error
     */
    suspend fun appendInsight(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                Log.w(TAG, "Cannot append empty insight")
                return@withContext false
            }

            // Clean the text
            val cleanedText = text.trim()
                .removePrefix("-")
                .removePrefix("•")
                .trim()

            if (cleanedText.isEmpty()) {
                Log.w(TAG, "Cleaned insight is empty")
                return@withContext false
            }

            // Check for duplicates
            if (isDuplicate(cleanedText)) {
                Log.d(TAG, "Insight is duplicate, skipping: ${cleanedText.take(50)}...")
                return@withContext false
            }

            val file = getInsightsFile()

            // Ensure file exists
            if (!file.exists()) {
                file.createNewFile()
            }

            // Format insight with timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val formattedInsight = "- [$timestamp] $cleanedText\n"

            // Append to file
            FileOutputStream(file, true).use { fos ->
                fos.write(formattedInsight.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            Log.d(TAG, "Appended insight: ${cleanedText.take(50)}...")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error appending insight", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error appending insight", e)
            false
        }
    }

    /**
     * Check if an insight is a duplicate
     * Uses both exact match and similarity comparison
     *
     * @param newInsight The insight to check
     * @return True if duplicate found, false otherwise
     */
    private suspend fun isDuplicate(newInsight: String): Boolean = withContext(Dispatchers.Default) {
        try {
            val existingInsights = readInsights()
            if (existingInsights.isEmpty()) {
                return@withContext false
            }

            // Extract insight text from each line (remove timestamp and bullet)
            val existingTexts = existingInsights.lines()
                .filter { it.isNotBlank() }
                .map { extractInsightText(it) }
                .filter { it.isNotEmpty() }

            // Check exact match (case-insensitive)
            val newLower = newInsight.lowercase()
            for (existing in existingTexts) {
                if (existing.lowercase() == newLower) {
                    Log.d(TAG, "Found exact match duplicate")
                    return@withContext true
                }
            }

            // Check similarity (Levenshtein distance)
            for (existing in existingTexts) {
                val similarity = calculateSimilarity(newInsight, existing)
                if (similarity >= SIMILARITY_THRESHOLD) {
                    Log.d(TAG, "Found similar duplicate (similarity: $similarity)")
                    return@withContext true
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking duplicate", e)
            false
        }
    }

    /**
     * Extract insight text from formatted line
     * Input: "- [2025-12-10 14:30] Grows tomatoes"
     * Output: "Grows tomatoes"
     */
    private fun extractInsightText(line: String): String {
        return line
            .removePrefix("-")
            .removePrefix("•")
            .replace(Regex("\\[.*?\\]"), "") // Remove timestamp
            .trim()
    }

    /**
     * Calculate similarity between two strings using normalized Levenshtein distance
     * Returns value between 0.0 (completely different) and 1.0 (identical)
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1.lowercase(), s2.lowercase())
        val maxLength = maxOf(s1.length, s2.length)

        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Extract new insights from user query and AI response
     * Uses Gemini AI to analyze the conversation and extract personal information
     *
     * @param query The user's question
     * @param aiResponse The AI's response
     * @return List of extracted insights (may be empty)
     */
    suspend fun extractNewInsightsFromUserQuery(
        query: String,
        aiResponse: String
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extracting insights from query: ${query.take(50)}...")

            // Read existing insights to avoid duplicates
            val existingInsights = readInsights()

            // Build prompt for insight extraction
            val prompt = buildInsightExtractionPrompt(query, aiResponse, existingInsights)

            // Generate insights using Gemini
            val response = model.generateContent(prompt)
            val responseText = response.text?.trim() ?: ""

            if (responseText.isEmpty() ||
                responseText.lowercase().contains("no new insight") ||
                responseText.lowercase().contains("no meaningful")) {
                Log.d(TAG, "No new insights extracted")
                return@withContext emptyList()
            }

            // Parse insights from response
            val insights = parseInsightsFromResponse(responseText)

            Log.d(TAG, "Extracted ${insights.size} insights")
            insights.forEach { Log.d(TAG, "  - ${it.take(50)}...") }

            insights
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting insights", e)
            emptyList()
        }
    }

    /**
     * Build prompt for Gemini to extract insights
     */
    private fun buildInsightExtractionPrompt(
        query: String,
        aiResponse: String,
        existingInsights: String
    ): String {
        return """
            You are an AI assistant that extracts meaningful personal information from farmer queries.
            
            TASK: Extract ONLY stable, meaningful personal information from the conversation that could be useful for future personalized responses.
            
            User Question: "$query"
            AI Response: "$aiResponse"
            
            Existing Insights (DO NOT REPEAT):
            $existingInsights
            
            RULES:
            1. Extract ONLY factual, stable information about the farmer (crops, land size, location, problems, preferences, etc.)
            2. Extract ONLY information explicitly mentioned or strongly implied
            3. Format each insight as a single concise sentence (no bullet symbols)
            4. One insight per line
            5. Be specific and actionable
            6. DO NOT repeat information already in existing insights
            7. DO NOT extract general farming questions or temporary queries
            8. If no meaningful personal information found, respond with exactly: "No new insights"
            
            EXAMPLES OF GOOD INSIGHTS:
            Grows tomatoes on 2 acres and faces frequent pest issues
            Has drip irrigation system installed in May 2024
            Located in drought-prone area of Maharashtra
            Interested in organic farming methods
            Faces water shortage during summer months
            Uses neem oil spray for pest control
            Plans to expand cultivation to 5 acres
            Attends agricultural workshops regularly
            
            EXAMPLES OF BAD INSIGHTS (do NOT extract):
            Asked about weather (too general)
            Wants to know fertilizer prices (temporary query)
            Curious about crop rotation (general interest, not personal)
            Needs help with farming (too vague)
            
            Extract insights (one per line, no numbering, no bullets) or respond with "No new insights":
        """.trimIndent()
    }

    /**
     * Parse insights from Gemini response
     */
    private fun parseInsightsFromResponse(responseText: String): List<String> {
        return responseText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { !it.lowercase().contains("no new insight") }
            .filter { !it.lowercase().contains("no meaningful") }
            .map { line ->
                // Remove numbering, bullets, etc.
                line.replace(Regex("^[0-9]+\\.\\s*"), "")
                    .replace(Regex("^[-•*]\\s*"), "")
                    .trim()
            }
            .filter { it.length >= 10 } // Minimum length check
            .take(5) // Maximum 5 insights per query
    }

    /**
     * Update vector database with insights
     * Deletes old insight vectors and re-embeds the complete insights file
     *
     * @return True if update successful, false on error
     */
    suspend fun updateVectorDB(): Boolean = withContext(Dispatchers.IO) {
        try {
            val farmerId = getCurrentFarmerId()
            if (farmerId == null) {
                Log.w(TAG, "Cannot update vector DB: no farmer logged in")
                return@withContext false
            }

            Log.d(TAG, "Updating vector DB for farmer: $farmerId")

            // Step 1: Delete old insight vectors
            deleteInsightVectors(farmerId)

            // Step 2: Read current insights
            val insightsContent = readInsights()
            if (insightsContent.isEmpty()) {
                Log.d(TAG, "No insights to embed")
                return@withContext true
            }

            // Step 3: Chunk the insights
            val chunks = chunkText(insightsContent)
            Log.d(TAG, "Generated ${chunks.size} chunks from insights")

            // Step 4: Generate embeddings for each chunk
            val vectorInsertData = chunks.mapIndexedNotNull { index, chunk ->
                val embedding = embeddingService.generateEmbedding(chunk)

                if (embedding == null || embedding.isEmpty()) {
                    Log.w(TAG, "Failed to generate embedding for chunk $index")
                    return@mapIndexedNotNull null
                }

                val metadata = mapOf(
                    "id" to "${farmerId}_insights_chunk_$index",
                    "type" to "farmer_insights",
                    "source_file" to "farmer_insights.txt",
                    "chunk_index" to index,
                    "content" to chunk,
                    "timestamp" to System.currentTimeMillis()
                )

                VectorInsertData(
                    id = "${farmerId}_insights_chunk_$index",
                    embedding = embedding,
                    metadata = metadata,
                    farmerId = farmerId
                )
            }

            // Step 5: Insert into vector database
            if (vectorInsertData.isNotEmpty()) {
                val success = vectorDb.insertVectorsBatch(vectorInsertData)
                if (success) {
                    Log.d(TAG, "Updated ${vectorInsertData.size} insight vectors in DB")
                } else {
                    Log.e(TAG, "Failed to insert insight vectors")
                }
                return@withContext success
            } else {
                Log.w(TAG, "No embeddings generated for insights")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vector DB", e)
            false
        }
    }

    /**
     * Delete existing insight vectors for a farmer
     */
    private suspend fun deleteInsightVectors(farmerId: String) = withContext(Dispatchers.IO) {
        try {
            val dao = com.krishisakhi.farmassistant.db.AppDatabase.getDatabase(context).vectorEntryDao()
            val allVectors = dao.getVectorsByFarmerId(farmerId)

            val insightVectorIds = allVectors
                .filter { it.sourceType == "farmer_insights" }
                .map { it.id }

            for (id in insightVectorIds) {
                vectorDb.deleteVector(id)
            }

            Log.d(TAG, "Deleted ${insightVectorIds.size} old insight vectors")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting insight vectors", e)
        }
    }

    /**
     * Chunk text for embedding
     */
    private fun chunkText(text: String): List<String> {
        if (text.length <= CHUNK_SIZE) {
            return listOf(text)
        }

        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < text.length) {
            val endIndex = minOf(startIndex + CHUNK_SIZE, text.length)
            var chunk = text.substring(startIndex, endIndex)

            // Try to end at line boundary
            if (endIndex < text.length) {
                val lastNewline = chunk.lastIndexOf('\n')
                if (lastNewline > CHUNK_SIZE - 100) {
                    chunk = chunk.substring(0, lastNewline)
                }
            }

            if (chunk.isNotBlank()) {
                chunks.add(chunk.trim())
            }

            startIndex += CHUNK_SIZE - CHUNK_OVERLAP
        }

        return chunks
    }

    /**
     * Get current farmer ID from Firebase Auth
     */
    private fun getCurrentFarmerId(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current farmer ID", e)
            null
        }
    }

    /**
     * Get insights statistics
     */
    suspend fun getInsightsStats(): InsightsStats = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile()
            val exists = file.exists()
            val content = if (exists) readInsights() else ""
            val lines = if (content.isNotBlank()) content.lines().filter { it.isNotBlank() } else emptyList()

            InsightsStats(
                exists = exists,
                fileSizeBytes = if (exists) file.length() else 0L,
                insightCount = lines.size,
                totalCharacters = content.length,
                filePath = file.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting insights stats", e)
            InsightsStats(false, 0L, 0, 0, getInsightsFile().absolutePath)
        }
    }

    /**
     * Check if insights file exists
     */
    suspend fun insightsExist(): Boolean = withContext(Dispatchers.IO) {
        try {
            getInsightsFile().exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking insights existence", e)
            false
        }
    }

    /**
     * Check if insights have content
     */
    suspend fun hasInsights(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile()
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking insights content", e)
            false
        }
    }

    /**
     * Clear all insights (use with caution)
     */
    suspend fun clearInsights(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile()
            if (file.exists()) {
                FileOutputStream(file, false).use { fos ->
                    fos.write(ByteArray(0))
                }
                Log.d(TAG, "Cleared all insights")
                true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing insights", e)
            false
        }
    }

    /**
     * Delete insights file (use with caution)
     */
    suspend fun deleteInsights(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile()
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Deleted insights file")
                }
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting insights", e)
            false
        }
    }
}

/**
 * Data class for insights statistics
 */
data class InsightsStats(
    val exists: Boolean,
    val fileSizeBytes: Long,
    val insightCount: Int,
    val totalCharacters: Int,
    val filePath: String
) {
    override fun toString(): String {
        return """
            Insights Statistics:
            - Exists: $exists
            - File Size: ${fileSizeBytes / 1024.0} KB
            - Insights: $insightCount
            - Characters: $totalCharacters
            - Path: $filePath
        """.trimIndent()
    }
}

