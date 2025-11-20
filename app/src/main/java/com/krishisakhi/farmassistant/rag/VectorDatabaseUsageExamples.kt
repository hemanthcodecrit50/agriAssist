package com.krishisakhi.farmassistant.rag

/**
 * Example usage of the Persistent Vector Database system
 *
 * This file demonstrates how to use the persistent vector database
 * for storing and searching knowledge vectors including farmer profiles
 */

// ============================================================================
// EXAMPLE 1: Initialize the persistent vector database at app startup
// ============================================================================

/*
class KrishiSakhiApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize in background
        CoroutineScope(Dispatchers.IO).launch {
            val vectorDb = VectorDatabasePersistent(applicationContext)
            vectorDb.initialize() // Loads all vectors from Room into memory

            val count = vectorDb.getVectorCount()
            Log.d("App", "Loaded $count vectors")
        }
    }
}
*/

// ============================================================================
// EXAMPLE 2: Initialize knowledge base with static agricultural knowledge
// ============================================================================

/*
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val knowledgeBase = KnowledgeBaseManager(this)

        lifecycleScope.launch {
            // This loads sample documents, embeds them, and stores in persistent DB
            val success = knowledgeBase.initializeKnowledgeBase()

            if (success) {
                Log.d("MainActivity", "Knowledge base ready!")
            }
        }
    }
}
*/

// ============================================================================
// EXAMPLE 3: Add farmer profile to vector database when registration completes
// ============================================================================

/*
class RegistrationActivity : AppCompatActivity() {
    private fun saveProfile(profile: FarmerProfile) {
        lifecycleScope.launch {
            // 1. Save to Room database
            withContext(Dispatchers.IO) {
                db.farmerProfileDao().insertProfile(profile)
            }

            // 2. Add farmer profile as vector entry
            val knowledgeBase = KnowledgeBaseManager(this@RegistrationActivity)

            val profileContent = """
                Farmer Profile:
                Name: ${profile.name}
                Location: ${profile.village}, ${profile.district}, ${profile.state}
                Total Land: ${profile.totalLandSize} hectares
                Soil Type: ${profile.soilType}
                Primary Crops: ${profile.primaryCrops.joinToString(", ")}
                Language: ${profile.languagePreference}
            """.trimIndent()

            val profileData = mapOf(
                "title" to "Farmer Profile: ${profile.name}",
                "content" to profileContent,
                "category" to "farmer_profile",
                "tags" to "profile,farmer,${profile.state}",
                "name" to profile.name,
                "location" to "${profile.village}, ${profile.district}, ${profile.state}",
                "landSize" to profile.totalLandSize.toString(),
                "soilType" to profile.soilType,
                "crops" to profile.primaryCrops.joinToString(",")
            )

            val success = knowledgeBase.addFarmerProfile(
                farmerId = profile.phoneNumber,
                profileContent = profileContent,
                profileData = profileData
            )

            if (success) {
                Log.d("Registration", "Farmer profile vector added successfully")
            }
        }
    }
}
*/

// ============================================================================
// EXAMPLE 4: Update farmer profile vector when profile is edited
// ============================================================================

/*
class ProfileActivity : AppCompatActivity() {
    private fun updateProfile(updatedProfile: FarmerProfile) {
        lifecycleScope.launch {
            // 1. Update Room database
            withContext(Dispatchers.IO) {
                db.farmerProfileDao().insertProfile(updatedProfile)
            }

            // 2. Update farmer profile vector
            val knowledgeBase = KnowledgeBaseManager(this@ProfileActivity)

            val profileContent = """
                Farmer Profile:
                Name: ${updatedProfile.name}
                Location: ${updatedProfile.village}, ${updatedProfile.district}, ${updatedProfile.state}
                Total Land: ${updatedProfile.totalLandSize} hectares
                Soil Type: ${updatedProfile.soilType}
                Primary Crops: ${updatedProfile.primaryCrops.joinToString(", ")}
                Language: ${updatedProfile.languagePreference}
            """.trimIndent()

            val profileData = mapOf(
                "title" to "Farmer Profile: ${updatedProfile.name}",
                "content" to profileContent,
                "category" to "farmer_profile",
                "tags" to "profile,farmer,${updatedProfile.state}",
                "name" to updatedProfile.name,
                "location" to "${updatedProfile.village}, ${updatedProfile.district}, ${updatedProfile.state}",
                "landSize" to updatedProfile.totalLandSize.toString(),
                "soilType" to updatedProfile.soilType,
                "crops" to updatedProfile.primaryCrops.joinToString(",")
            )

            val success = knowledgeBase.updateFarmerProfile(
                farmerId = updatedProfile.phoneNumber,
                profileContent = profileContent,
                profileData = profileData
            )

            if (success) {
                Log.d("Profile", "Farmer profile vector updated successfully")
            }
        }
    }
}
*/

// ============================================================================
// EXAMPLE 5: Search for relevant knowledge using RAG
// ============================================================================

/*
class EnhancedAIService(private val context: Context) {
    private val knowledgeBase = KnowledgeBaseManager(context)

    suspend fun processQuery(query: String, farmerId: String?): String {
        // Search for relevant vectors (both static knowledge AND farmer profile)
        val searchResults = knowledgeBase.search(
            query = query,
            topK = 5,
            categoryFilter = null, // Search all categories
            farmerIdFilter = null  // Search both static and farmer-specific vectors
        )

        // Build RAG prompt with context
        val contextText = searchResults.joinToString("\n\n") { result ->
            "${result.document.title}\n${result.snippet}\n(Relevance: ${result.score})"
        }

        val prompt = """
        You are an agricultural assistant for Indian farmers.

        Context from knowledge base:
        $contextText

        User Question: $query

        Provide a helpful answer based on the context above.
        """.trimIndent()

        // Send to Gemini API
        return geminiAPICall(prompt)
    }
}
*/

// ============================================================================
// EXAMPLE 6: Direct vector database operations
// ============================================================================

/*
class VectorOperationsExample {
    suspend fun examples(context: Context) {
        val vectorDb = VectorDatabasePersistent(context)

        // Initialize
        vectorDb.initialize()

        // Insert a single vector
        val embedding = FloatArray(768) { Random.nextFloat() }
        val metadata = mapOf(
            "title" to "Test Document",
            "content" to "This is test content",
            "category" to "test"
        )

        vectorDb.insertVector(
            id = "test_doc_1",
            embedding = embedding,
            metadata = metadata,
            farmerId = null
        )

        // Batch insert vectors
        val batchData = listOf(
            VectorInsertData(
                id = "doc_1",
                embedding = FloatArray(768) { Random.nextFloat() },
                metadata = mapOf("title" to "Doc 1"),
                farmerId = null
            ),
            VectorInsertData(
                id = "doc_2",
                embedding = FloatArray(768) { Random.nextFloat() },
                metadata = mapOf("title" to "Doc 2"),
                farmerId = "919876543210"
            )
        )

        vectorDb.insertVectorsBatch(batchData)

        // Search for similar vectors
        val queryEmbedding = FloatArray(768) { Random.nextFloat() }
        val results = vectorDb.searchSimilar(
            queryEmbedding = queryEmbedding,
            topK = 5,
            minScore = 0.3f,
            farmerIdFilter = null
        )

        results.forEach { result ->
            Log.d("Search", "ID: ${result.id}, Score: ${result.score}")
            Log.d("Search", "Title: ${result.metadata.optString("title")}")
        }

        // Delete a vector
        vectorDb.deleteVector("test_doc_1")

        // Delete all vectors for a farmer
        vectorDb.deleteVectorsByFarmerId("919876543210")

        // Get vector count
        val count = vectorDb.getVectorCount()
        Log.d("VectorDB", "Total vectors: $count")

        // Clear all vectors
        vectorDb.clearAll()
    }
}
*/

// ============================================================================
// EXAMPLE 7: Serialization utilities
// ============================================================================

/*
class SerializationExample {
    fun example() {
        // Convert FloatArray to ByteArray for database storage
        val embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val bytes = VectorSerializer.floatArrayToByteArray(embedding)

        // Convert ByteArray back to FloatArray
        val reconstructed = VectorSerializer.byteArrayToFloatArray(bytes)

        // Calculate cosine similarity
        val vec1 = floatArrayOf(1f, 0f, 0f)
        val vec2 = floatArrayOf(0f, 1f, 0f)
        val similarity = VectorSerializer.cosineSimilarity(vec1, vec2)

        Log.d("Similarity", "Cosine similarity: $similarity")
    }
}
*/

// ============================================================================
// ARCHITECTURE SUMMARY
// ============================================================================

/*
┌─────────────────────────────────────────────────────────────────┐
│                     KrishiSakhi Application                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ├─> KrishiSakhiApplication (initializes VectorDB at startup)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   KnowledgeBaseManager                           │
│  - initializeKnowledgeBase()                                    │
│  - addDocument()                                                │
│  - search()                                                     │
│  - addFarmerProfile()                                           │
│  - updateFarmerProfile()                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
┌────────────────────────────┐  ┌──────────────────────────────┐
│  VectorDatabasePersistent   │  │    EmbeddingService          │
│  - initialize()             │  │  - generateEmbedding()       │
│  - insertVector()           │  └──────────────────────────────┘
│  - searchSimilar()          │
│  - deleteVector()           │
└────────────────────────────┘
          │
          ├─> In-Memory Cache (for fast similarity search)
          │
          ▼
┌────────────────────────────┐
│   Room Database             │
│   - VectorEntryEntity       │
│   - VectorEntryDao          │
└────────────────────────────┘
          │
          ▼
┌────────────────────────────┐
│   SQLite Database File      │
│   (Persistent Storage)      │
└────────────────────────────┘

DATA FLOW:
1. App starts → KrishiSakhiApplication initializes VectorDatabasePersistent
2. VectorDatabasePersistent loads all vectors from Room into memory cache
3. KnowledgeBaseManager initializes with static agricultural knowledge
4. Static documents are embedded and stored in VectorEntryEntity
5. When farmer registers, their profile is embedded and stored with farmerId
6. When farmer updates profile, their vector is updated
7. When query comes, embedding is generated and compared with in-memory cache
8. Top K similar vectors are returned based on cosine similarity
9. Results include both static knowledge AND farmer-specific vectors
10. All changes persist across app restarts via Room database
*/

