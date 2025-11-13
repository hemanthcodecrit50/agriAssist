package com.krishisakhi.farmassistant.utils

import android.content.Context
import android.util.Log
import com.krishisakhi.farmassistant.rag.KnowledgeDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for loading agricultural data from various sources
 */
class DataLoader(private val context: Context) {

    companion object {
        private const val TAG = "DataLoader"
    }

    /**
     * Load knowledge documents from JSON file in assets
     * Format: [{"id": "...", "title": "...", "content": "...", "category": "...", "tags": [...]}]
     */
    fun loadDocumentsFromAssets(filename: String): List<KnowledgeDocument> {
        try {
            val jsonString = readAssetFile(filename)
            val jsonArray = JSONArray(jsonString)
            val documents = mutableListOf<KnowledgeDocument>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val document = parseDocumentFromJson(jsonObject)
                documents.add(document)
            }

            Log.d(TAG, "Loaded ${documents.size} documents from $filename")
            return documents

        } catch (e: Exception) {
            Log.e(TAG, "Error loading documents from assets: $filename", e)
            return emptyList()
        }
    }

    /**
     * Parse a single document from JSON object
     */
    private fun parseDocumentFromJson(jsonObject: JSONObject): KnowledgeDocument {
        val id = jsonObject.getString("id")
        val title = jsonObject.getString("title")
        val content = jsonObject.getString("content")
        val category = jsonObject.getString("category")

        val tagsArray = jsonObject.optJSONArray("tags")
        val tags = mutableListOf<String>()
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }

        return KnowledgeDocument(
            id = id,
            title = title,
            content = content,
            category = category,
            tags = tags,
            embedding = null, // Will be generated later
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Read file from assets folder
     */
    private fun readAssetFile(filename: String): String {
        return context.assets.open(filename).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    }

    /**
     * Create sample JSON structure for agricultural documents
     * Returns a JSON string that can be saved to assets
     */
    fun generateSampleJsonStructure(): String {
        val jsonArray = JSONArray()

        // Sample document 1
        val doc1 = JSONObject().apply {
            put("id", "doc_001")
            put("title", "Rice Cultivation in Kharif Season")
            put("content", "Rice is the main kharif crop. Sow in June-July when monsoon arrives. Use 25-30 day old seedlings. Plant at 20x15 cm spacing. Apply 120 kg N, 60 kg P2O5, 40 kg K2O per hectare.")
            put("category", "crop_cultivation")
            put("tags", JSONArray(listOf("rice", "kharif", "monsoon", "paddy")))
        }
        jsonArray.put(doc1)

        // Sample document 2
        val doc2 = JSONObject().apply {
            put("id", "doc_002")
            put("title", "Pest Control in Cotton")
            put("content", "Cotton bollworm is a major pest. Monitor fields regularly. Use pheromone traps @ 8/hectare. Spray neem oil 3% or Bt @ 1 kg/ha. Chemical control: Spinosad 45% SC @ 160 ml/ha.")
            put("category", "pest_disease")
            put("tags", JSONArray(listOf("cotton", "bollworm", "pest", "control")))
        }
        jsonArray.put(doc2)

        return jsonArray.toString(2) // Pretty print with 2-space indentation
    }

    /**
     * Validate document structure
     */
    fun isValidDocument(doc: KnowledgeDocument): Boolean {
        return doc.id.isNotBlank() &&
                doc.title.isNotBlank() &&
                doc.content.isNotBlank() &&
                doc.category.isNotBlank()
    }

    /**
     * Get statistics about loaded documents
     */
    fun getDocumentStats(documents: List<KnowledgeDocument>): DocumentStats {
        val categoryCounts = documents.groupingBy { it.category }.eachCount()
        val avgContentLength = documents.map { it.content.length }.average()
        val totalTags = documents.flatMap { it.tags }.toSet().size

        return DocumentStats(
            totalDocuments = documents.size,
            categoryCounts = categoryCounts,
            averageContentLength = avgContentLength.toInt(),
            uniqueTags = totalTags
        )
    }

    data class DocumentStats(
        val totalDocuments: Int,
        val categoryCounts: Map<String, Int>,
        val averageContentLength: Int,
        val uniqueTags: Int
    )
}

