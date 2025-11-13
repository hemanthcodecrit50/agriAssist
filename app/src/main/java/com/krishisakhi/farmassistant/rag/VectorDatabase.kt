package com.krishisakhi.farmassistant.rag

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vector database for storing and searching knowledge documents
 * Uses SQLite for storage with custom vector search
 */
class VectorDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "VectorDatabase"
        private const val DATABASE_NAME = "krishi_knowledge.db"
        private const val DATABASE_VERSION = 1

        // Table names
        private const val TABLE_DOCUMENTS = "documents"

        // Column names
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_CONTENT = "content"
        private const val COL_CATEGORY = "category"
        private const val COL_TAGS = "tags"
        private const val COL_EMBEDDING = "embedding"
        private const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_DOCUMENTS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_TITLE TEXT NOT NULL,
                $COL_CONTENT TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_TAGS TEXT,
                $COL_EMBEDDING BLOB,
                $COL_TIMESTAMP INTEGER
            )
        """.trimIndent()

        db.execSQL(createTable)

        // Create index on category for faster filtering
        db.execSQL("CREATE INDEX idx_category ON $TABLE_DOCUMENTS($COL_CATEGORY)")

        Log.d(TAG, "Database created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCUMENTS")
        onCreate(db)
    }

    /**
     * Insert a document into the database
     */
    suspend fun insertDocument(document: KnowledgeDocument): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_ID, document.id)
                put(COL_TITLE, document.title)
                put(COL_CONTENT, document.content)
                put(COL_CATEGORY, document.category)
                put(COL_TAGS, document.tags.joinToString(","))
                put(COL_EMBEDDING, document.embedding?.let { floatArrayToByteArray(it) })
                put(COL_TIMESTAMP, document.timestamp)
            }

            val result = db.insertWithOnConflict(TABLE_DOCUMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "Inserted document: ${document.id}, result: $result")
            result != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting document", e)
            false
        }
    }

    /**
     * Search for similar documents using vector similarity
     */
    suspend fun searchSimilar(
        queryEmbedding: FloatArray,
        topK: Int = 5,
        categoryFilter: String? = null,
        minScore: Float = 0.3f
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val db = readableDatabase
            val results = mutableListOf<SearchResult>()

            // Build query with optional category filter
            val selection = if (categoryFilter != null) "$COL_CATEGORY = ?" else null
            val selectionArgs = if (categoryFilter != null) arrayOf(categoryFilter) else null

            val cursor = db.query(
                TABLE_DOCUMENTS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_ID))
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                    val content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT))
                    val category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))
                    val tagsStr = it.getString(it.getColumnIndexOrThrow(COL_TAGS))
                    val embeddingBytes = it.getBlob(it.getColumnIndexOrThrow(COL_EMBEDDING))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))

                    val embedding = embeddingBytes?.let { bytes -> byteArrayToFloatArray(bytes) }

                    if (embedding != null) {
                        val similarity = cosineSimilarity(queryEmbedding, embedding)

                        if (similarity >= minScore) {
                            val tags = tagsStr.split(",").filter { tag -> tag.isNotBlank() }
                            val document = KnowledgeDocument(
                                id = id,
                                title = title,
                                content = content,
                                category = category,
                                tags = tags,
                                embedding = embedding,
                                timestamp = timestamp
                            )

                            // Extract relevant snippet
                            val snippet = extractRelevantSnippet(content, 150)

                            results.add(SearchResult(document, similarity, snippet))
                        }
                    }
                }
            }

            // Sort by score and return top K
            results.sortedByDescending { it.score }.take(topK)

        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents", e)
            emptyList()
        }
    }

    /**
     * Get document by ID
     */
    suspend fun getDocument(id: String): KnowledgeDocument? = withContext(Dispatchers.IO) {
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_DOCUMENTS,
                null,
                "$COL_ID = ?",
                arrayOf(id),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                    val content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT))
                    val category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))
                    val tagsStr = it.getString(it.getColumnIndexOrThrow(COL_TAGS))
                    val embeddingBytes = it.getBlob(it.getColumnIndexOrThrow(COL_EMBEDDING))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))

                    val tags = tagsStr.split(",").filter { tag -> tag.isNotBlank() }
                    val embedding = embeddingBytes?.let { bytes -> byteArrayToFloatArray(bytes) }

                    KnowledgeDocument(id, title, content, category, tags, embedding, timestamp)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting document", e)
            null
        }
    }

    /**
     * Get all documents by category
     */
    suspend fun getDocumentsByCategory(category: String): List<KnowledgeDocument> = withContext(Dispatchers.IO) {
        try {
            val documents = mutableListOf<KnowledgeDocument>()
            val db = readableDatabase

            val cursor = db.query(
                TABLE_DOCUMENTS,
                null,
                "$COL_CATEGORY = ?",
                arrayOf(category),
                null,
                null,
                "$COL_TIMESTAMP DESC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_ID))
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                    val content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT))
                    val tagsStr = it.getString(it.getColumnIndexOrThrow(COL_TAGS))
                    val embeddingBytes = it.getBlob(it.getColumnIndexOrThrow(COL_EMBEDDING))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))

                    val tags = tagsStr.split(",").filter { tag -> tag.isNotBlank() }
                    val embedding = embeddingBytes?.let { bytes -> byteArrayToFloatArray(bytes) }

                    documents.add(KnowledgeDocument(id, title, content, category, tags, embedding, timestamp))
                }
            }

            documents
        } catch (e: Exception) {
            Log.e(TAG, "Error getting documents by category", e)
            emptyList()
        }
    }

    /**
     * Get count of documents
     */
    suspend fun getDocumentCount(): Int = withContext(Dispatchers.IO) {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_DOCUMENTS", null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting document count", e)
            0
        }
    }

    /**
     * Clear all documents
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = writableDatabase
            db.delete(TABLE_DOCUMENTS, null, null)
            Log.d(TAG, "Cleared all documents")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing documents", e)
            false
        }
    }

    // Helper functions

    private fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            buffer.putFloat(f)
        }
        return buffer.array()
    }

    private fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) {
            floats[i] = buffer.getFloat()
        }
        return floats
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
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

    private fun extractRelevantSnippet(content: String, maxLength: Int): String {
        return if (content.length <= maxLength) {
            content
        } else {
            content.substring(0, maxLength) + "..."
        }
    }
}

