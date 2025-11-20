package com.krishisakhi.farmassistant.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility class for serializing and deserializing vector embeddings
 * Converts between FloatArray (used for embeddings) and ByteArray (stored in database)
 */
object VectorSerializer {

    /**
     * Convert FloatArray to ByteArray for database storage
     * Uses little-endian byte order for consistency
     *
     * @param floats The float array to serialize
     * @return ByteArray representation of the float array
     */
    fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4) // 4 bytes per float
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            buffer.putFloat(f)
        }
        return buffer.array()
    }

    /**
     * Convert ByteArray to FloatArray for vector operations
     * Uses little-endian byte order for consistency
     *
     * @param bytes The byte array to deserialize
     * @return FloatArray representation
     */
    fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4) // 4 bytes per float
        for (i in floats.indices) {
            floats[i] = buffer.getFloat()
        }
        return floats
    }

    /**
     * Calculate cosine similarity between two float arrays
     * Returns a value between -1 and 1, where 1 means identical direction
     *
     * @param vec1 First vector
     * @param vec2 Second vector
     * @return Cosine similarity score
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

