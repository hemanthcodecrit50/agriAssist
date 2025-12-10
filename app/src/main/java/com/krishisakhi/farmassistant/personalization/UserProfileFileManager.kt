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
 * Manages the farmer_profile.txt file for user-editable free-form text
 * This is a user-controlled file with no schema validation
 *
 * File Structure:
 * - Location: {app_internal_storage}/farmer_profile.txt
 * - Encoding: UTF-8
 * - Content: Free-form text written by the farmer
 * - Size: Typically 1-5 KB
 *
 * Usage:
 * ```
 * val manager = UserProfileFileManager(context)
 * manager.createOrLoadProfileFile()
 * manager.writeProfile("I grow tomatoes on 5 acres...")
 * val profile = manager.readProfile()
 * val chunks = manager.getChunksForEmbedding()
 * ```
 */
class UserProfileFileManager(private val context: Context) {

    companion object {
        private const val TAG = "UserProfileFileManager"
        private const val PROFILE_FILE_NAME = "farmer_profile.txt"
        private const val ENCODING = "UTF-8"

        // Chunking parameters for embedding generation
        private const val CHUNK_SIZE = 500 // Characters per chunk
        private const val CHUNK_OVERLAP = 50 // Overlap between consecutive chunks
    }

    /**
     * Get the profile file reference
     */
    private fun getProfileFile(): File {
        return File(context.filesDir, PROFILE_FILE_NAME)
    }

    /**
     * Get the absolute file path (for debugging/logging)
     */
    fun getFilePath(): String {
        return getProfileFile().absolutePath
    }

    /**
     * Create or load the profile file
     * Creates an empty file if it doesn't exist
     *
     * @return True if file was created/exists, false on error
     */
    suspend fun createOrLoadProfileFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()

            if (!file.exists()) {
                // Create new empty file
                val created = file.createNewFile()
                if (created) {
                    Log.d(TAG, "Created new profile file at: ${file.absolutePath}")
                } else {
                    Log.e(TAG, "Failed to create profile file")
                    return@withContext false
                }
            } else {
                Log.d(TAG, "Profile file already exists at: ${file.absolutePath}")
            }

            // Verify file is readable and writable
            if (!file.canRead()) {
                Log.e(TAG, "Profile file is not readable")
                return@withContext false
            }

            if (!file.canWrite()) {
                Log.e(TAG, "Profile file is not writable")
                return@withContext false
            }

            true
        } catch (e: IOException) {
            Log.e(TAG, "Error creating/loading profile file", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing profile file", e)
            false
        }
    }

    /**
     * Read the complete profile content
     * Returns empty string if file doesn't exist or is empty
     * Never returns null
     *
     * @return Profile content as UTF-8 string (never null)
     */
    suspend fun readProfile(): String = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()

            if (!file.exists()) {
                Log.d(TAG, "Profile file does not exist, returning empty string")
                return@withContext ""
            }

            if (file.length() == 0L) {
                Log.d(TAG, "Profile file is empty")
                return@withContext ""
            }

            // Read file content with UTF-8 encoding
            FileInputStream(file).use { fis ->
                val bytes = fis.readBytes()
                val content = String(bytes, Charsets.UTF_8)

                Log.d(TAG, "Read profile: ${content.length} characters")
                content
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading profile file", e)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error reading profile", e)
            ""
        }
    }

    /**
     * Write profile content (overwrites existing content)
     * This is the main method for saving user's profile
     *
     * @param text The profile text to write (can be empty to clear)
     * @return True if write was successful, false on error
     */
    suspend fun writeProfile(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            // Write content with UTF-8 encoding (overwrite mode)
            FileOutputStream(file, false).use { fos ->
                fos.write(text.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            Log.d(TAG, "Wrote profile: ${text.length} characters to ${file.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error writing profile file", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error writing profile", e)
            false
        }
    }

    /**
     * Append text to existing profile content
     * Useful for adding sections without overwriting
     *
     * @param text The text to append
     * @return True if append was successful, false on error
     */
    suspend fun appendProfile(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            // If file doesn't exist, create it first
            if (!file.exists()) {
                file.createNewFile()
            }

            // Append content with UTF-8 encoding
            FileOutputStream(file, true).use { fos ->
                fos.write(text.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            Log.d(TAG, "Appended ${text.length} characters to profile")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error appending to profile file", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error appending to profile", e)
            false
        }
    }

    /**
     * Get profile content split into chunks for embedding generation
     * Uses overlapping chunks to preserve context at boundaries
     *
     * Algorithm:
     * - Split text into chunks of ~500 characters
     * - Add 50 character overlap between chunks
     * - Preserve word boundaries when possible
     *
     * Example:
     * ```
     * Input: "I grow tomatoes on 5 acres..." (1200 chars)
     * Output: [
     *   "I grow tomatoes on 5 acres..." (500 chars),
     *   "...5 acres in Pune. My soil..." (500 chars, 50 overlap),
     *   "...soil is black cotton..." (200 chars, 50 overlap)
     * ]
     * ```
     *
     * @return List of text chunks (empty list if no content)
     */
    suspend fun getChunksForEmbedding(): List<String> = withContext(Dispatchers.IO) {
        try {
            val content = readProfile()

            if (content.isEmpty()) {
                Log.d(TAG, "No profile content to chunk")
                return@withContext emptyList()
            }

            // If content is smaller than chunk size, return as single chunk
            if (content.length <= CHUNK_SIZE) {
                Log.d(TAG, "Profile fits in single chunk: ${content.length} characters")
                return@withContext listOf(content)
            }

            // Split into overlapping chunks
            val chunks = mutableListOf<String>()
            var startIndex = 0

            while (startIndex < content.length) {
                val endIndex = minOf(startIndex + CHUNK_SIZE, content.length)

                // Extract chunk
                var chunk = content.substring(startIndex, endIndex)

                // Try to end at word boundary (if not at end of content)
                if (endIndex < content.length) {
                    val lastSpaceIndex = chunk.lastIndexOf(' ')
                    if (lastSpaceIndex > CHUNK_SIZE - 50) { // Only if we're near the end
                        chunk = chunk.substring(0, lastSpaceIndex)
                    }
                }

                // Add chunk if not empty
                if (chunk.isNotBlank()) {
                    chunks.add(chunk.trim())
                }

                // Move to next chunk with overlap
                startIndex += CHUNK_SIZE - CHUNK_OVERLAP
            }

            Log.d(TAG, "Split profile into ${chunks.size} chunks for embedding")
            chunks
        } catch (e: Exception) {
            Log.e(TAG, "Error chunking profile for embedding", e)
            emptyList()
        }
    }

    /**
     * Check if profile file exists
     *
     * @return True if file exists, false otherwise
     */
    suspend fun profileExists(): Boolean = withContext(Dispatchers.IO) {
        try {
            getProfileFile().exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile existence", e)
            false
        }
    }

    /**
     * Check if profile has content (not empty)
     *
     * @return True if file exists and has content, false otherwise
     */
    suspend fun hasContent(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile content", e)
            false
        }
    }

    /**
     * Get profile file size in bytes
     *
     * @return File size in bytes, 0 if file doesn't exist
     */
    suspend fun getFileSize(): Long = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            0L
        }
    }

    /**
     * Delete the profile file
     * USE WITH CAUTION - This permanently deletes user data
     *
     * @return True if file was deleted or didn't exist, false on error
     */
    suspend fun deleteProfile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()

            if (!file.exists()) {
                Log.d(TAG, "Profile file doesn't exist, nothing to delete")
                return@withContext true
            }

            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted profile file")
            } else {
                Log.e(TAG, "Failed to delete profile file")
            }

            deleted
        } catch (e: IOException) {
            Log.e(TAG, "Error deleting profile file", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error deleting profile", e)
            false
        }
    }

    /**
     * Clear profile content (write empty string)
     * Keeps the file but removes all content
     *
     * @return True if cleared successfully, false on error
     */
    suspend fun clearProfile(): Boolean = withContext(Dispatchers.IO) {
        writeProfile("")
    }

    /**
     * Get profile statistics for debugging/analytics
     *
     * @return ProfileStats object with file information
     */
    suspend fun getProfileStats(): ProfileStats = withContext(Dispatchers.IO) {
        try {
            val file = getProfileFile()
            val exists = file.exists()
            val size = if (exists) file.length() else 0L
            val content = if (exists) readProfile() else ""
            val characterCount = content.length
            val wordCount = if (content.isNotBlank()) content.split("\\s+".toRegex()).size else 0
            val lineCount = if (content.isNotBlank()) content.lines().size else 0
            val chunks = if (exists) getChunksForEmbedding().size else 0

            ProfileStats(
                exists = exists,
                fileSizeBytes = size,
                characterCount = characterCount,
                wordCount = wordCount,
                lineCount = lineCount,
                chunkCount = chunks,
                filePath = file.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile stats", e)
            ProfileStats(
                exists = false,
                fileSizeBytes = 0L,
                characterCount = 0,
                wordCount = 0,
                lineCount = 0,
                chunkCount = 0,
                filePath = getProfileFile().absolutePath
            )
        }
    }

    /**
     * Validate profile content (basic checks)
     *
     * @return ValidationResult with status and messages
     */
    suspend fun validateProfile(): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val content = readProfile()
            val warnings = mutableListOf<String>()

            // Check if empty
            if (content.isEmpty()) {
                return@withContext ValidationResult(
                    isValid = true,
                    warnings = listOf("Profile is empty"),
                    errors = emptyList()
                )
            }

            // Check size
            if (content.length < 20) {
                warnings.add("Profile is very short (${content.length} characters)")
            }

            if (content.length > 5000) {
                warnings.add("Profile is very long (${content.length} characters), consider shortening")
            }

            // Check for potentially problematic characters
            val controlChars = content.count { it.isISOControl() && it != '\n' && it != '\r' }
            if (controlChars > 0) {
                warnings.add("Profile contains $controlChars control characters")
            }

            ValidationResult(
                isValid = true,
                warnings = warnings,
                errors = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error validating profile", e)
            ValidationResult(
                isValid = false,
                warnings = emptyList(),
                errors = listOf("Error validating profile: ${e.message}")
            )
        }
    }
}

/**
 * Data class for profile statistics
 */
data class ProfileStats(
    val exists: Boolean,
    val fileSizeBytes: Long,
    val characterCount: Int,
    val wordCount: Int,
    val lineCount: Int,
    val chunkCount: Int,
    val filePath: String
) {
    override fun toString(): String {
        return """
            Profile Statistics:
            - Exists: $exists
            - File Size: ${fileSizeBytes / 1024.0} KB
            - Characters: $characterCount
            - Words: $wordCount
            - Lines: $lineCount
            - Chunks: $chunkCount
            - Path: $filePath
        """.trimIndent()
    }
}

/**
 * Data class for validation results
 */
data class ValidationResult(
    val isValid: Boolean,
    val warnings: List<String>,
    val errors: List<String>
) {
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    fun hasErrors(): Boolean = errors.isNotEmpty()

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Validation Result: ${if (isValid) "VALID" else "INVALID"}\n")

        if (errors.isNotEmpty()) {
            sb.append("Errors:\n")
            errors.forEach { sb.append("  - $it\n") }
        }

        if (warnings.isNotEmpty()) {
            sb.append("Warnings:\n")
            warnings.forEach { sb.append("  - $it\n") }
        }

        return sb.toString()
    }
}

