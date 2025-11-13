package com.krishisakhi.farmassistant.rag

/**
 * Represents a document in the knowledge base
 */
data class KnowledgeDocument(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String>,
    val embedding: FloatArray? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KnowledgeDocument

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}

/**
 * Represents a search result with relevance score
 */
data class SearchResult(
    val document: KnowledgeDocument,
    val score: Float,
    val matchedSnippet: String
)

