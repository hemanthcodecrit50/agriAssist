package com.krishisakhi.farmassistant.personalization

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch

/**
 * Complete usage examples for UserProfileFileManager
 * Demonstrates all functionality with real-world scenarios
 */
class UserProfileFileManagerExamples {

    companion object {
        private const val TAG = "ProfileExamples"
    }

    /**
     * Example 1: Create and initialize profile file
     */
    fun example1_CreateProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Create or load file
            val success = manager.createOrLoadProfileFile()

            if (success) {
                Log.d(TAG, "Profile file ready at: ${manager.getFilePath()}")
            } else {
                Log.e(TAG, "Failed to create profile file")
            }
        }
    }

    /**
     * Example 2: Write complete profile (overwrites existing)
     */
    fun example2_WriteProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            val profileText = """
                Name: Ramesh Kumar
                Location: Pune, Maharashtra
                Village: Khed
                
                Farm Details:
                - Total Land: 5 acres
                - Soil Type: Black cotton soil
                - Water Source: Borewell
                
                Crops:
                I grow tomatoes, onions, and wheat. During kharif season, I focus on 
                tomatoes and onions. In rabi season, I cultivate wheat.
                
                Challenges:
                - Water shortage in summer months
                - Pest problems in tomato crop
                - Need better market access
                
                Goals:
                - Learn organic farming methods
                - Install drip irrigation
                - Increase yield by 20%
            """.trimIndent()

            val success = manager.writeProfile(profileText)

            if (success) {
                Log.d(TAG, "Profile saved successfully")

                // Verify
                val stats = manager.getProfileStats()
                Log.d(TAG, stats.toString())
            }
        }
    }

    /**
     * Example 3: Read profile content
     */
    fun example3_ReadProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Check if profile exists
            if (!manager.hasContent()) {
                Log.d(TAG, "No profile content available")
                return@launch
            }

            // Read complete profile
            val content = manager.readProfile()
            Log.d(TAG, "Profile Content:\n$content")

            // Get metadata
            val stats = manager.getProfileStats()
            Log.d(TAG, "Characters: ${stats.characterCount}")
            Log.d(TAG, "Words: ${stats.wordCount}")
            Log.d(TAG, "Lines: ${stats.lineCount}")
        }
    }

    /**
     * Example 4: Append to existing profile
     */
    fun example4_AppendProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Add new section
            val additionalInfo = """
                
                Recent Updates (December 2025):
                - Installed drip irrigation system
                - Started vermicompost production
                - Attended organic farming workshop
            """.trimIndent()

            val success = manager.appendProfile(additionalInfo)

            if (success) {
                Log.d(TAG, "Additional info appended")

                // Read updated profile
                val updated = manager.readProfile()
                Log.d(TAG, "Updated Profile:\n$updated")
            }
        }
    }

    /**
     * Example 5: Get chunks for embedding
     */
    fun example5_GetChunks(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Get chunks optimized for embedding
            val chunks = manager.getChunksForEmbedding()

            Log.d(TAG, "Profile split into ${chunks.size} chunks:")
            chunks.forEachIndexed { index, chunk ->
                Log.d(TAG, "Chunk ${index + 1} (${chunk.length} chars):")
                Log.d(TAG, chunk.take(100) + "...")
            }

            // These chunks are ready for embedding generation
            // Example: Generate embeddings for RAG
            /*
            chunks.forEach { chunk ->
                val embedding = embeddingService.generateEmbedding(chunk)
                vectorDb.insertVector(embedding, metadata)
            }
            */
        }
    }

    /**
     * Example 6: Validate profile content
     */
    fun example6_ValidateProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Validate profile
            val validation = manager.validateProfile()

            Log.d(TAG, validation.toString())

            if (validation.hasWarnings()) {
                Log.w(TAG, "Profile has warnings:")
                validation.warnings.forEach { Log.w(TAG, "  - $it") }
            }

            if (validation.hasErrors()) {
                Log.e(TAG, "Profile has errors:")
                validation.errors.forEach { Log.e(TAG, "  - $it") }
            }
        }
    }

    /**
     * Example 7: Clear profile content
     */
    fun example7_ClearProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Clear content (keeps file)
            val success = manager.clearProfile()

            if (success) {
                Log.d(TAG, "Profile cleared")

                // Verify
                val hasContent = manager.hasContent()
                Log.d(TAG, "Has content after clear: $hasContent") // Should be false
            }
        }
    }

    /**
     * Example 8: Delete profile file
     */
    fun example8_DeleteProfile(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Delete file completely
            val success = manager.deleteProfile()

            if (success) {
                Log.d(TAG, "Profile file deleted")

                // Verify
                val exists = manager.profileExists()
                Log.d(TAG, "File exists after delete: $exists") // Should be false
            }
        }
    }

    /**
     * Example 9: Complete workflow - Create, Write, Read, Chunk
     */
    fun example9_CompleteWorkflow(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Step 1: Create file
            Log.d(TAG, "Step 1: Creating profile file...")
            if (!manager.createOrLoadProfileFile()) {
                Log.e(TAG, "Failed to create file")
                return@launch
            }

            // Step 2: Write initial content
            Log.d(TAG, "Step 2: Writing profile...")
            val profileText = """
                I am a farmer from Maharashtra. I have 5 acres of land where I grow 
                tomatoes, onions, and wheat. My soil is black cotton soil, which is 
                good for cotton and other crops. I face water shortage problems during 
                summer. I want to learn about organic farming and drip irrigation.
            """.trimIndent()

            if (!manager.writeProfile(profileText)) {
                Log.e(TAG, "Failed to write profile")
                return@launch
            }

            // Step 3: Validate
            Log.d(TAG, "Step 3: Validating...")
            val validation = manager.validateProfile()
            if (!validation.isValid) {
                Log.e(TAG, "Validation failed: ${validation.errors}")
                return@launch
            }

            // Step 4: Get statistics
            Log.d(TAG, "Step 4: Getting statistics...")
            val stats = manager.getProfileStats()
            Log.d(TAG, stats.toString())

            // Step 5: Get chunks for embedding
            Log.d(TAG, "Step 5: Chunking for embeddings...")
            val chunks = manager.getChunksForEmbedding()
            Log.d(TAG, "Generated ${chunks.size} chunks")

            // Step 6: Append more info
            Log.d(TAG, "Step 6: Appending additional info...")
            manager.appendProfile("\n\nI recently attended a workshop on sustainable farming.")

            // Step 7: Read final content
            Log.d(TAG, "Step 7: Reading final profile...")
            val finalContent = manager.readProfile()
            Log.d(TAG, "Final profile length: ${finalContent.length} characters")

            Log.d(TAG, "✅ Complete workflow finished successfully!")
        }
    }

    /**
     * Example 10: Handle edge cases
     */
    fun example10_EdgeCases(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val manager = UserProfileFileManager(context)

            // Test 1: Read non-existent file
            Log.d(TAG, "Test 1: Reading non-existent file")
            val emptyRead = manager.readProfile()
            Log.d(TAG, "Result: '$emptyRead' (should be empty string)")

            // Test 2: Write empty string
            Log.d(TAG, "Test 2: Writing empty string")
            manager.writeProfile("")
            val afterEmpty = manager.hasContent()
            Log.d(TAG, "Has content: $afterEmpty (should be false)")

            // Test 3: Write very long text
            Log.d(TAG, "Test 3: Writing long text")
            val longText = "A".repeat(10000)
            manager.writeProfile(longText)
            val chunks = manager.getChunksForEmbedding()
            Log.d(TAG, "Long text split into ${chunks.size} chunks")

            // Test 4: Write special characters
            Log.d(TAG, "Test 4: Special characters")
            val specialText = "Test with émojis 🌾🚜 and spëcial çharacters"
            manager.writeProfile(specialText)
            val readBack = manager.readProfile()
            Log.d(TAG, "Special chars preserved: ${specialText == readBack}")

            // Test 5: Multiple rapid writes
            Log.d(TAG, "Test 5: Rapid writes")
            repeat(5) { i ->
                manager.writeProfile("Write $i")
            }
            val finalRead = manager.readProfile()
            Log.d(TAG, "Final content after rapid writes: '$finalRead'")
        }
    }
}

/**
 * Quick reference for common operations
 */
object UserProfileQuickReference {

    /**
     * Initialize and create profile file
     */
    suspend fun initialize(context: Context): Boolean {
        val manager = UserProfileFileManager(context)
        return manager.createOrLoadProfileFile()
    }

    /**
     * Save user's profile text
     */
    suspend fun saveProfile(context: Context, text: String): Boolean {
        val manager = UserProfileFileManager(context)
        return manager.writeProfile(text)
    }

    /**
     * Load user's profile text
     */
    suspend fun loadProfile(context: Context): String {
        val manager = UserProfileFileManager(context)
        return manager.readProfile()
    }

    /**
     * Get chunks ready for embedding generation
     */
    suspend fun getChunksForEmbedding(context: Context): List<String> {
        val manager = UserProfileFileManager(context)
        return manager.getChunksForEmbedding()
    }

    /**
     * Check if profile has content
     */
    suspend fun hasProfile(context: Context): Boolean {
        val manager = UserProfileFileManager(context)
        return manager.hasContent()
    }

    /**
     * Get profile statistics
     */
    suspend fun getStats(context: Context): ProfileStats {
        val manager = UserProfileFileManager(context)
        return manager.getProfileStats()
    }

    /**
     * Delete profile
     */
    suspend fun deleteProfile(context: Context): Boolean {
        val manager = UserProfileFileManager(context)
        return manager.deleteProfile()
    }
}

/**
 * Integration with existing PersonalizationEmbeddingManager
 */
class ProfileEmbeddingIntegration {

    /**
     * Example: Index profile into vector database using UserProfileFileManager
     */
    suspend fun indexUserProfile(
        context: Context,
        farmerId: String,
        embeddingService: com.krishisakhi.farmassistant.rag.EmbeddingService,
        vectorDb: com.krishisakhi.farmassistant.rag.VectorDatabasePersistent
    ): Boolean {
        return try {
            // Step 1: Get chunks from profile
            val manager = UserProfileFileManager(context)
            val chunks = manager.getChunksForEmbedding()

            if (chunks.isEmpty()) {
                Log.d(TAG, "No profile content to index")
                return true
            }

            // Step 2: Generate embeddings for each chunk
            val vectorInsertData = chunks.mapIndexedNotNull { index, chunk ->
                val embedding = embeddingService.generateEmbedding(chunk)

                if (embedding == null || embedding.isEmpty()) {
                    Log.w(TAG, "Failed to generate embedding for chunk $index")
                    return@mapIndexedNotNull null
                }

                // Create metadata
                val metadata = mapOf(
                    "id" to "${farmerId}_profile_chunk_$index",
                    "type" to "farmer_profile",
                    "source_file" to "farmer_profile.txt",
                    "chunk_index" to index,
                    "content" to chunk,
                    "timestamp" to System.currentTimeMillis()
                )

                com.krishisakhi.farmassistant.rag.VectorInsertData(
                    id = "${farmerId}_profile_chunk_$index",
                    embedding = embedding,
                    metadata = metadata,
                    farmerId = farmerId
                )
            }

            // Step 3: Insert into vector database
            if (vectorInsertData.isNotEmpty()) {
                vectorDb.insertVectorsBatch(vectorInsertData)
                Log.d(TAG, "Indexed ${vectorInsertData.size} profile chunks for farmer: $farmerId")
                true
            } else {
                Log.w(TAG, "No embeddings generated for profile")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing user profile", e)
            false
        }
    }

    companion object {
        private const val TAG = "ProfileEmbeddingIntegration"
    }
}

