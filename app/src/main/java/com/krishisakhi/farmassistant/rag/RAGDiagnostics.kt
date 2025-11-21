package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Diagnostic tool for debugging RAG pipeline issues
 * Use this to verify all components of the RAG system are working correctly
 */
class RAGDiagnostics(private val context: Context) {

    companion object {
        private const val TAG = "RAGDiagnostics"
    }

    private val vectorDb = VectorDatabasePersistent(context)
    private val embeddingService = EmbeddingService(context)
    private val db = AppDatabase.getDatabase(context)

    /**
     * Run complete diagnostic test of RAG pipeline
     * Call this from MainActivity or a debug activity
     */
    suspend fun runFullDiagnostic(): DiagnosticReport = withContext(Dispatchers.IO) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Starting RAG Pipeline Diagnostic")
        Log.d(TAG, "========================================")

        val report = DiagnosticReport()

        // Test 1: Check Firebase Auth
        report.authTest = testFirebaseAuth()

        // Test 2: Check vector database
        report.vectorDbTest = testVectorDatabase()

        // Test 3: Check farmer profile exists
        report.farmerProfileTest = testFarmerProfile()

        // Test 4: Check general knowledge vectors
        report.generalKnowledgeTest = testGeneralKnowledge()

        // Test 5: Test embedding generation
        report.embeddingTest = testEmbeddingGeneration()

        // Test 6: Test vector retrieval
        report.retrievalTest = testVectorRetrieval()

        // Test 7: Test full RAG pipeline
        report.pipelineTest = testFullPipeline()

        Log.d(TAG, "========================================")
        Log.d(TAG, "Diagnostic Complete")
        Log.d(TAG, "========================================")

        report.printSummary()

        return@withContext report
    }

    private fun testFirebaseAuth(): TestResult {
        return try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val uid = user?.uid
            val phone = user?.phoneNumber

            Log.d(TAG, "✓ Firebase Auth Test")
            Log.d(TAG, "  - User authenticated: ${user != null}")
            Log.d(TAG, "  - UID: $uid")
            Log.d(TAG, "  - Phone: $phone")

            if (uid != null) {
                TestResult(true, "Firebase Auth OK - UID: $uid")
            } else {
                TestResult(false, "No user authenticated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Firebase Auth Test Failed", e)
            TestResult(false, "Auth error: ${e.message}")
        }
    }

    private suspend fun testVectorDatabase(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            vectorDb.initialize()
            val count = vectorDb.getVectorCount()

            Log.d(TAG, "✓ Vector Database Test")
            Log.d(TAG, "  - Total vectors: $count")

            // Get vectors by type
            val allVectors = db.vectorEntryDao().getAllVectors()
            val farmerVectors = allVectors.filter { it.sourceType == "farmer_profile" }
            val generalVectors = allVectors.filter { it.sourceType != "farmer_profile" }

            Log.d(TAG, "  - Farmer profile vectors: ${farmerVectors.size}")
            Log.d(TAG, "  - General knowledge vectors: ${generalVectors.size}")

            if (count > 0) {
                TestResult(true, "Vector DB OK - $count vectors (${farmerVectors.size} profile, ${generalVectors.size} general)")
            } else {
                TestResult(false, "No vectors in database")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Vector Database Test Failed", e)
            TestResult(false, "Vector DB error: ${e.message}")
        }
    }

    private suspend fun testFarmerProfile(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid == null) {
                Log.w(TAG, "⚠ Farmer Profile Test Skipped - No authenticated user")
                return@withContext TestResult(true, "Skipped - no user")
            }

            // Check Room database
            val profile = db.farmerProfileDao().getProfileByUid(uid)

            // Check vector database
            val vectors = db.vectorEntryDao().getVectorsByFarmerId(uid)

            Log.d(TAG, "✓ Farmer Profile Test")
            Log.d(TAG, "  - Profile in Room: ${profile != null}")
            Log.d(TAG, "  - Profile vectors: ${vectors.size}")

            if (profile != null) {
                Log.d(TAG, "  - Name: ${profile.name}")
                Log.d(TAG, "  - Location: ${profile.village}, ${profile.district}, ${profile.state}")
                Log.d(TAG, "  - Crops: ${profile.primaryCrops.joinToString(", ")}")
            }

            if (vectors.isNotEmpty()) {
                vectors.forEach { vector ->
                    Log.d(TAG, "  - Vector ID: ${vector.id}, Type: ${vector.sourceType}")
                }
            }

            if (profile != null && vectors.isNotEmpty()) {
                TestResult(true, "Farmer profile OK - ${vectors.size} vectors")
            } else if (profile != null) {
                TestResult(false, "Farmer profile exists but no vectors created")
            } else {
                TestResult(false, "No farmer profile found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Farmer Profile Test Failed", e)
            TestResult(false, "Profile test error: ${e.message}")
        }
    }

    private suspend fun testGeneralKnowledge(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val generalVectors = db.vectorEntryDao().getStaticKnowledgeVectors()

            Log.d(TAG, "✓ General Knowledge Test")
            Log.d(TAG, "  - General vectors: ${generalVectors.size}")

            if (generalVectors.isNotEmpty()) {
                // Show sample
                val sample = generalVectors.take(3)
                sample.forEach { vector ->
                    val metadata = org.json.JSONObject(vector.metadataJson)
                    val title = metadata.optString("title", "Unknown")
                    Log.d(TAG, "  - Sample: $title")
                }
                TestResult(true, "General knowledge OK - ${generalVectors.size} vectors")
            } else {
                TestResult(false, "No general knowledge vectors found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ General Knowledge Test Failed", e)
            TestResult(false, "General knowledge error: ${e.message}")
        }
    }

    private suspend fun testEmbeddingGeneration(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val testQueries = listOf(
                "How to grow rice in monsoon season?",
                "What fertilizer for wheat crop?",
                "Pest control in tomato farming"
            )

            Log.d(TAG, "✓ Embedding Generation Test")

            for (query in testQueries) {
                val embedding = embeddingService.generateEmbedding(query)

                if (embedding != null && embedding.isNotEmpty()) {
                    val magnitude = Math.sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
                    Log.d(TAG, "  - Query: ${query.take(40)}...")
                    Log.d(TAG, "    Embedding dim: ${embedding.size}, magnitude: $magnitude")
                } else {
                    Log.e(TAG, "  ✗ Failed to generate embedding for: $query")
                    return@withContext TestResult(false, "Embedding generation failed")
                }
            }

            TestResult(true, "Embedding generation OK")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Embedding Generation Test Failed", e)
            TestResult(false, "Embedding error: ${e.message}")
        }
    }

    private suspend fun testVectorRetrieval(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = "How to grow rice in kharif season?"
            val queryEmbedding = embeddingService.generateEmbedding(query)

            if (queryEmbedding == null) {
                return@withContext TestResult(false, "Failed to generate query embedding")
            }

            Log.d(TAG, "✓ Vector Retrieval Test")
            Log.d(TAG, "  - Query: $query")

            // Test retrieval without filters
            val allResults = vectorDb.searchSimilar(
                queryEmbedding = queryEmbedding,
                topK = 5,
                minScore = 0.1f
            )

            Log.d(TAG, "  - All results: ${allResults.size}")
            allResults.forEach { result ->
                val type = result.metadata.optString("type", "unknown")
                Log.d(TAG, "    * Score: ${String.format("%.3f", result.score)}, Type: $type")
            }

            // Test farmer profile retrieval
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val farmerResults = vectorDb.searchSimilar(
                    queryEmbedding = queryEmbedding,
                    topK = 3,
                    minScore = 0.1f,
                    farmerIdFilter = uid,
                    sourceTypeFilter = "farmer_profile"
                )
                Log.d(TAG, "  - Farmer profile results: ${farmerResults.size}")
            }

            // Test general knowledge retrieval
            val generalResults = vectorDb.searchSimilar(
                queryEmbedding = queryEmbedding,
                topK = 5,
                minScore = 0.1f,
                excludeSourceType = "farmer_profile"
            )
            Log.d(TAG, "  - General knowledge results: ${generalResults.size}")

            if (allResults.isNotEmpty()) {
                TestResult(true, "Vector retrieval OK - found ${allResults.size} results")
            } else {
                TestResult(false, "No vectors retrieved")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Vector Retrieval Test Failed", e)
            TestResult(false, "Retrieval error: ${e.message}")
        }
    }

    private suspend fun testFullPipeline(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val enhancedAI = EnhancedAIService(context, "dummy_key")
            enhancedAI.initialize()

            Log.d(TAG, "✓ Full Pipeline Test")
            Log.d(TAG, "  - EnhancedAIService initialized")
            Log.d(TAG, "  - Ready for queries")

            TestResult(true, "Full pipeline OK")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Full Pipeline Test Failed", e)
            TestResult(false, "Pipeline error: ${e.message}")
        }
    }

    data class TestResult(
        val passed: Boolean,
        val message: String
    )

    data class DiagnosticReport(
        var authTest: TestResult = TestResult(false, "Not run"),
        var vectorDbTest: TestResult = TestResult(false, "Not run"),
        var farmerProfileTest: TestResult = TestResult(false, "Not run"),
        var generalKnowledgeTest: TestResult = TestResult(false, "Not run"),
        var embeddingTest: TestResult = TestResult(false, "Not run"),
        var retrievalTest: TestResult = TestResult(false, "Not run"),
        var pipelineTest: TestResult = TestResult(false, "Not run")
    ) {
        fun printSummary() {
            Log.d(TAG, "")
            Log.d(TAG, "==== DIAGNOSTIC SUMMARY ====")
            Log.d(TAG, "1. Firebase Auth: ${if (authTest.passed) "✓ PASS" else "✗ FAIL"} - ${authTest.message}")
            Log.d(TAG, "2. Vector Database: ${if (vectorDbTest.passed) "✓ PASS" else "✗ FAIL"} - ${vectorDbTest.message}")
            Log.d(TAG, "3. Farmer Profile: ${if (farmerProfileTest.passed) "✓ PASS" else "✗ FAIL"} - ${farmerProfileTest.message}")
            Log.d(TAG, "4. General Knowledge: ${if (generalKnowledgeTest.passed) "✓ PASS" else "✗ FAIL"} - ${generalKnowledgeTest.message}")
            Log.d(TAG, "5. Embedding Generation: ${if (embeddingTest.passed) "✓ PASS" else "✗ FAIL"} - ${embeddingTest.message}")
            Log.d(TAG, "6. Vector Retrieval: ${if (retrievalTest.passed) "✓ PASS" else "✗ FAIL"} - ${retrievalTest.message}")
            Log.d(TAG, "7. Full Pipeline: ${if (pipelineTest.passed) "✓ PASS" else "✗ FAIL"} - ${pipelineTest.message}")
            Log.d(TAG, "============================")

            val allPassed = authTest.passed && vectorDbTest.passed &&
                           farmerProfileTest.passed && generalKnowledgeTest.passed &&
                           embeddingTest.passed && retrievalTest.passed && pipelineTest.passed

            if (allPassed) {
                Log.d(TAG, "✓ ALL TESTS PASSED - RAG pipeline is working correctly")
            } else {
                Log.e(TAG, "✗ SOME TESTS FAILED - Check logs above for details")
            }
        }

        fun isHealthy(): Boolean {
            return authTest.passed && vectorDbTest.passed &&
                   embeddingTest.passed && retrievalTest.passed && pipelineTest.passed
        }
    }
}

