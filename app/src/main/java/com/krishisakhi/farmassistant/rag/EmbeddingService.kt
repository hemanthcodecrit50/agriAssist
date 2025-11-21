package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Service for generating embeddings from text
 * Uses a lightweight hashing-based approach for demo purposes
 * In production, this would use TensorFlow Lite with MobileBERT
 */
class EmbeddingService(private val context: Context) {

    companion object {
        private const val TAG = "EmbeddingService"
        private const val EMBEDDING_DIM = 128 // Reduced dimension for simplicity
    }

    /**
     * Generate embedding vector for given text
     * Uses a lightweight hashing approach with TF-IDF inspired weighting
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        try {
            // Preprocess text
            val processedText = preprocessText(text)

            // Generate embedding using multiple hash functions
            val embedding = FloatArray(EMBEDDING_DIM)

            // Split into words and generate features
            val words = processedText.split("\\s+".toRegex())
            val wordFrequency = words.groupingBy { it }.eachCount()

            // Important agricultural terms get higher weight
            val importantTerms = setOf(
                "rice", "wheat", "cotton", "maize", "potato", "tomato",
                "pest", "disease", "fertilizer", "irrigation", "soil",
                "kharif", "rabi", "monsoon", "winter", "seed", "harvest",
                "yield", "crop", "farming", "cultivation", "nutrient"
            )

            // Generate embedding using word hashing and frequency weighting
            for ((word, freq) in wordFrequency) {
                if (word.isBlank() || word.length < 2) continue

                // Multiple hash positions for better distribution
                val hash1 = hashToIndex(word, 0)
                val hash2 = hashToIndex(word, 1)
                val hash3 = hashToIndex(word, 2)

                // Weight by frequency (simple TF-IDF approximation)
                var weight = (1.0 + Math.log(freq.toDouble())).toFloat()

                // Boost weight for important agricultural terms
                if (importantTerms.contains(word)) {
                    weight *= 1.5f
                }

                embedding[hash1] += weight
                embedding[hash2] += weight * 0.7f
                embedding[hash3] += weight * 0.5f
            }

            // Normalize the vector
            normalizeVector(embedding)

            Log.d(TAG, "Generated embedding for text: ${text.take(50)}...")
            embedding

        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            FloatArray(EMBEDDING_DIM) // Return zero vector on error
        }
    }

    // Agricultural term synonyms for better semantic understanding
    private val synonymMap = mapOf(
        "paddy" to "rice",
        "gehu" to "wheat",
        "dhan" to "rice",
        "makka" to "maize",
        "corn" to "maize",
        "tamatar" to "tomato",
        "aloo" to "potato",
        "kapas" to "cotton",
        "sarson" to "mustard",
        "chana" to "chickpea",
        "gram" to "chickpea",
        "insect" to "pest",
        "bug" to "pest",
        "disease" to "pest",
        "kharif" to "monsoon",
        "rabi" to "winter",
        "cultivation" to "farming",
        "crop" to "farming",
        "fertilizer" to "nutrient",
        "manure" to "nutrient",
        "pesticide" to "chemical",
        "herbicide" to "chemical"
    )

    /**
     * Preprocess text for embedding generation
     * Expands synonyms for better agricultural term matching
     */
    private fun preprocessText(text: String): String {
        var processed = text.lowercase()
            .replace("[^a-zA-Z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        // Expand synonyms
        val words = processed.split(" ")
        val expandedWords = mutableListOf<String>()
        for (word in words) {
            expandedWords.add(word)
            // Add synonym if exists
            synonymMap[word]?.let { synonym ->
                expandedWords.add(synonym)
            }
        }

        return expandedWords.joinToString(" ")
    }

    /**
     * Hash word to index position using MD5
     */
    private fun hashToIndex(word: String, seed: Int): Int {
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest("$word$seed".toByteArray())
        val intValue = ((hash[0].toInt() and 0xFF) shl 24) or
                      ((hash[1].toInt() and 0xFF) shl 16) or
                      ((hash[2].toInt() and 0xFF) shl 8) or
                      (hash[3].toInt() and 0xFF)
        return Math.abs(intValue) % EMBEDDING_DIM
    }

    /**
     * Normalize vector to unit length
     */
    private fun normalizeVector(vector: FloatArray) {
        val magnitude = Math.sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (magnitude > 0) {
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
    }

    /**
     * Calculate cosine similarity between two embedding vectors
     */
    fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        val magnitude = Math.sqrt((norm1 * norm2).toDouble()).toFloat()
        return if (magnitude > 0) dotProduct / magnitude else 0f
    }
}

