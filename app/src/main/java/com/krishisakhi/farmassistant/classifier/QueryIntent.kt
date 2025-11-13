package com.krishisakhi.farmassistant.classifier

/**
 * Represents different types of user intents
 */
enum class QueryIntent {
    WEATHER,           // Weather related queries
    PEST_DISEASE,      // Pest and disease information
    CROP_CULTIVATION,  // Crop growing techniques
    MARKET_PRICE,      // Market price queries
    GOVT_SCHEME,       // Government schemes
    FERTILIZER,        // Fertilizer and nutrients
    IRRIGATION,        // Irrigation and water management
    SOIL_HEALTH,       // Soil testing and health
    GENERAL_FARMING,   // General farming questions
    UNKNOWN            // Cannot classify
}

/**
 * Result of intent classification
 */
data class IntentClassificationResult(
    val intent: QueryIntent,
    val confidence: Float,
    val alternativeIntents: List<Pair<QueryIntent, Float>> = emptyList()
)

