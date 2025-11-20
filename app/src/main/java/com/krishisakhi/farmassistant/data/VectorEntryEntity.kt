package com.krishisakhi.farmassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing vector embeddings persistently
 *
 * @param id Unique identifier for the vector entry (UUID or composite key)
 * @param farmerId Optional farmer ID (phone number) if this vector is farmer-specific
 * @param vectorBlob Serialized FloatArray embedding as ByteArray
 * @param metadataJson JSON string containing metadata (title, content, category, tags, timestamp, etc.)
 */
@Entity(tableName = "vector_entries")
data class VectorEntryEntity(
    @PrimaryKey
    val id: String,

    val farmerId: String? = null,

    // New column introduced in DB version 3 for faster filtering
    val sourceType: String? = null, // "general" or "farmer_profile"

    val vectorBlob: ByteArray,

    val metadataJson: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorEntryEntity

        if (id != other.id) return false
        if (farmerId != other.farmerId) return false
        if (sourceType != other.sourceType) return false
        if (!vectorBlob.contentEquals(other.vectorBlob)) return false
        if (metadataJson != other.metadataJson) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (farmerId?.hashCode() ?: 0)
        result = 31 * result + (sourceType?.hashCode() ?: 0)
        result = 31 * result + vectorBlob.contentHashCode()
        result = 31 * result + metadataJson.hashCode()
        return result
    }
}
