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

            // Generate embedding using word hashing and frequency weighting
            for ((word, freq) in wordFrequency) {
                if (word.isBlank()) continue

                // Multiple hash positions for better distribution
                val hash1 = hashToIndex(word, 0)
                val hash2 = hashToIndex(word, 1)
                val hash3 = hashToIndex(word, 2)

                // Weight by frequency (simple TF-IDF approximation)
                val weight = (1.0 + Math.log(freq.toDouble())).toFloat()

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

    /**
     * Preprocess text for embedding generation
     */
    private fun preprocessText(text: String): String {
        return text.lowercase()
            .replace("[^a-zA-Z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
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

