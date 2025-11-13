package com.krishisakhi.farmassistant.classifier

import android.content.Context
import android.util.Log

/**
 * Intent classifier for routing user queries
 * Uses rule-based keyword matching for lightweight classification
 */
class IntentClassifier(private val context: Context) {

    companion object {
        private const val TAG = "IntentClassifier"

        // Keyword patterns for each intent
        private val WEATHER_KEYWORDS = listOf(
            "weather", "rain", "temperature", "climate", "forecast", "mausam",
            "barish", "garmi", "sardi", "thand", "humidity", "wind"
        )

        private val PEST_KEYWORDS = listOf(
            "pest", "disease", "insect", "bug", "infection", "fungus", "virus",
            "keeda", "keet", "bimari", "rog", "attack", "damage", "害虫"
        )

        private val CROP_KEYWORDS = listOf(
            "crop", "cultivation", "growing", "planting", "sowing", "harvesting",
            "fasal", "kheti", "बोना", "ugana", "variety", "seed", "beej"
        )

        private val MARKET_KEYWORDS = listOf(
            "price", "market", "mandi", "rate", "cost", "sell", "buy",
            "bhav", "kimat", "selling", "purchase", "trading"
        )

        private val GOVT_SCHEME_KEYWORDS = listOf(
            "scheme", "subsidy", "loan", "government", "yojana", "sarkar",
            "pm", "kisan", "samman", "nidhi", "insurance", "bima"
        )

        private val FERTILIZER_KEYWORDS = listOf(
            "fertilizer", "nutrient", "npk", "urea", "compost", "manure",
            "khad", "urat", "poshan", "dawa", "organic", "chemical"
        )

        private val IRRIGATION_KEYWORDS = listOf(
            "water", "irrigation", "drip", "sprinkler", "pump", "pani",
            "sinchai", "tube", "well", "canal", "moisture", "watering"
        )

        private val SOIL_KEYWORDS = listOf(
            "soil", "mitti", "testing", "ph", "health", "quality",
            "bhumi", "zameen", "nutrients", "fertility", "erosion"
        )
    }

    /**
     * Classify user query into an intent
     */
    fun classify(query: String): IntentClassificationResult {
        val normalizedQuery = query.lowercase().trim()
        val words = normalizedQuery.split("\\s+".toRegex())

        // Score each intent
        val intentScores = mutableMapOf<QueryIntent, Float>()

        intentScores[QueryIntent.WEATHER] = calculateScore(words, WEATHER_KEYWORDS)
        intentScores[QueryIntent.PEST_DISEASE] = calculateScore(words, PEST_KEYWORDS)
        intentScores[QueryIntent.CROP_CULTIVATION] = calculateScore(words, CROP_KEYWORDS)
        intentScores[QueryIntent.MARKET_PRICE] = calculateScore(words, MARKET_KEYWORDS)
        intentScores[QueryIntent.GOVT_SCHEME] = calculateScore(words, GOVT_SCHEME_KEYWORDS)
        intentScores[QueryIntent.FERTILIZER] = calculateScore(words, FERTILIZER_KEYWORDS)
        intentScores[QueryIntent.IRRIGATION] = calculateScore(words, IRRIGATION_KEYWORDS)
        intentScores[QueryIntent.SOIL_HEALTH] = calculateScore(words, SOIL_KEYWORDS)

        // Check for general farming queries
        if ((intentScores.values.maxOrNull() ?: 0f) < 0.3f) {
            intentScores[QueryIntent.GENERAL_FARMING] = 0.5f
        }

        // Sort by score
        val sortedIntents = intentScores.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        val topIntent = sortedIntents.firstOrNull()
        val topScore = topIntent?.second ?: 0f

        val intent = if (topScore > 0.2f) {
            topIntent?.first ?: QueryIntent.UNKNOWN
        } else {
            QueryIntent.UNKNOWN
        }

        val alternatives = sortedIntents
            .drop(1)
            .take(2)
            .filter { it.second > 0.1f }

        Log.d(TAG, "Classified query '$query' as $intent with confidence $topScore")

        return IntentClassificationResult(
            intent = intent,
            confidence = topScore,
            alternativeIntents = alternatives
        )
    }

    /**
     * Calculate score based on keyword matches
     */
    private fun calculateScore(queryWords: List<String>, keywords: List<String>): Float {
        var matchCount = 0
        var partialMatchCount = 0

        for (word in queryWords) {
            // Exact match
            if (keywords.contains(word)) {
                matchCount++
            } else {
                // Partial match (word contains or is contained by keyword)
                for (keyword in keywords) {
                    if (word.contains(keyword) || keyword.contains(word)) {
                        if (word.length >= 3 && keyword.length >= 3) {
                            partialMatchCount++
                            break
                        }
                    }
                }
            }
        }

        // Calculate score: exact matches worth more than partial matches
        val score = (matchCount * 1.0f + partialMatchCount * 0.5f) / queryWords.size.toFloat()

        return score.coerceIn(0f, 1f)
    }

    /**
     * Get category filter for RAG search based on intent
     */
    fun getCategoryForIntent(intent: QueryIntent): String? {
        return when (intent) {
            QueryIntent.WEATHER -> "weather"
            QueryIntent.PEST_DISEASE -> "pest_disease"
            QueryIntent.CROP_CULTIVATION -> "crop_cultivation"
            QueryIntent.MARKET_PRICE -> "market_price"
            QueryIntent.GOVT_SCHEME -> "govt_scheme"
            QueryIntent.FERTILIZER -> "fertilizer"
            QueryIntent.IRRIGATION -> "irrigation"
            QueryIntent.SOIL_HEALTH -> "soil_health"
            QueryIntent.GENERAL_FARMING -> null // Search all categories
            QueryIntent.UNKNOWN -> null
        }
    }
}

