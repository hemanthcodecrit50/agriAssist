package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages two personalization files for each farmer:
 * 1. farmer_profile.txt - User-editable free-form text
 * 2. farmer_insights.txt - AI-generated insights (append-only)
 */
class FarmerFileManager(private val context: Context) {

    companion object {
        private const val TAG = "FarmerFileManager"
        private const val PROFILE_FILE_NAME = "farmer_profile.txt"
        private const val INSIGHTS_FILE_NAME = "farmer_insights.txt"
        private const val ENCODING = "UTF-8"
    }

    /**
     * Get the farmer profile file for a specific farmer ID
     */
    private fun getProfileFile(farmerId: String): File {
        val dir = File(context.filesDir, "farmer_data/$farmerId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, PROFILE_FILE_NAME)
    }

    /**
     * Get the farmer insights file for a specific farmer ID
     */
    private fun getInsightsFile(farmerId: String): File {
        val dir = File(context.filesDir, "farmer_data/$farmerId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, INSIGHTS_FILE_NAME)
    }

    /**
     * Read farmer profile content (user-editable file)
     */
    suspend fun readProfile(farmerId: String): String = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile(farmerId)
            if (!file.exists()) {
                Log.d(TAG, "Profile file does not exist for farmer: $farmerId")
                return@withContext ""
            }

            FileInputStream(file).use { fis ->
                val content = fis.readBytes().toString(Charsets.UTF_8)
                Log.d(TAG, "Read profile for farmer $farmerId: ${content.length} bytes")
                content
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading profile for farmer: $farmerId", e)
            ""
        }
    }

    /**
     * Write farmer profile content (overwrites existing content)
     * This is called when user edits their profile
     */
    suspend fun writeProfile(farmerId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile(farmerId)
            FileOutputStream(file, false).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }
            Log.d(TAG, "Wrote profile for farmer $farmerId: ${content.length} bytes")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error writing profile for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Check if profile file exists and has content
     */
    suspend fun hasProfile(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile(farmerId)
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile existence for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Read farmer insights content (AI-generated file)
     */
    suspend fun readInsights(farmerId: String): String = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile(farmerId)
            if (!file.exists()) {
                Log.d(TAG, "Insights file does not exist for farmer: $farmerId")
                return@withContext ""
            }

            FileInputStream(file).use { fis ->
                val content = fis.readBytes().toString(Charsets.UTF_8)
                Log.d(TAG, "Read insights for farmer $farmerId: ${content.length} bytes")
                content
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading insights for farmer: $farmerId", e)
            ""
        }
    }

    /**
     * Append new insights to the insights file
     * Only the AI should call this method
     * Insights are added as structured bullet points
     */
    suspend fun appendInsight(farmerId: String, insight: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile(farmerId)

            // Read existing content to check for duplicates
            val existingContent = if (file.exists()) {
                FileInputStream(file).use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                ""
            }

            // Skip if this insight already exists (simple duplicate check)
            if (existingContent.contains(insight.trim())) {
                Log.d(TAG, "Insight already exists, skipping: ${insight.take(50)}")
                return@withContext true
            }

            // Append new insight with timestamp
            FileOutputStream(file, true).use { fos ->
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val formattedInsight = "• [$timestamp] $insight\n"
                fos.write(formattedInsight.toByteArray(Charsets.UTF_8))
            }

            Log.d(TAG, "Appended insight for farmer $farmerId: ${insight.take(50)}...")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error appending insight for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Check if insights file exists and has content
     */
    suspend fun hasInsights(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getInsightsFile(farmerId)
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking insights existence for farmer: $farmerId", e)
            false
        }
    }

    /**
     * Get both profile and insights content combined
     */
    suspend fun readAllPersonalizationData(farmerId: String): PersonalizationData = withContext(Dispatchers.IO) {
        PersonalizationData(
            farmerId = farmerId,
            profileContent = readProfile(farmerId),
            insightsContent = readInsights(farmerId)
        )
    }

    /**
     * Delete all personalization files for a farmer
     */
    suspend fun deleteAllFiles(farmerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "farmer_data/$farmerId")
            if (dir.exists()) {
                dir.deleteRecursively()
                Log.d(TAG, "Deleted all files for farmer: $farmerId")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting files for farmer: $farmerId", e)
            false
        }
    }
}

/**
 * Data class for combined personalization data
 */
data class PersonalizationData(
    val farmerId: String,
    val profileContent: String,
    val insightsContent: String
) {
    fun isEmpty(): Boolean = profileContent.isEmpty() && insightsContent.isEmpty()

    fun getCombinedContent(): String {
        val sb = StringBuilder()

        if (profileContent.isNotEmpty()) {
            sb.append("=== Farmer Profile ===\n")
            sb.append(profileContent)
            sb.append("\n\n")
        }

        if (insightsContent.isNotEmpty()) {
            sb.append("=== AI-Generated Insights ===\n")
            sb.append(insightsContent)
            sb.append("\n")
        }

        return sb.toString().trim()
    }
}

