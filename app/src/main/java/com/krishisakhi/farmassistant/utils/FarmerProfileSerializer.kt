package com.krishisakhi.farmassistant.utils

import com.krishisakhi.farmassistant.data.FarmerProfile

/**
 * Serializer for converting FarmerProfile objects into natural language text
 * suitable for vector embeddings and semantic search.
 *
 * The output is designed to be:
 * - Compact and concise
 * - Semantically meaningful
 * - Optimized for embedding generation
 * - Easy for LLMs to understand and retrieve relevant information
 */
object FarmerProfileSerializer {

    /**
     * Convert FarmerProfile to natural language text
     *
     * @param profile The farmer profile to serialize
     * @return A compact natural language description
     */
    fun toNaturalLanguage(profile: FarmerProfile): String {
        return buildString {
            append("Farmer Profile: ")
            append(profile.name)
            append(". ")

            // Location information
            append("Located in ")
            append(profile.village)
            append(" village, ")
            append(profile.district)
            append(" district, ")
            append(profile.state)
            append(" state. ")

            // Land information
            append("Owns ")
            append(formatLandSize(profile.totalLandSize))
            append(" of agricultural land. ")

            // Soil information
            append("Soil type is ")
            append(profile.soilType.lowercase())
            append(". ")

            // Crops information
            if (profile.primaryCrops.isNotEmpty()) {
                append("Primary crops grown are ")
                append(formatCropsList(profile.primaryCrops))
                append(". ")
            }

            // Language preference
            append("Preferred language is ")
            append(profile.languagePreference)
            append(".")
        }.trim()
    }

    /**
     * Convert FarmerProfile to a more detailed natural language text
     * Includes all fields with richer context
     *
     * @param profile The farmer profile to serialize
     * @return A detailed natural language description
     */
    fun toDetailedNaturalLanguage(profile: FarmerProfile): String {
        return buildString {
            appendLine("Farmer Profile Information:")
            appendLine()

            // Name
            appendLine("Name: ${profile.name}")

            // Location
            appendLine("Location: ${profile.village}, ${profile.district}, ${profile.state}")

            // Land details
            appendLine("Total Land Size: ${formatLandSize(profile.totalLandSize)}")
            appendLine("Soil Type: ${profile.soilType}")

            // Crops
            if (profile.primaryCrops.isNotEmpty()) {
                appendLine("Primary Crops: ${profile.primaryCrops.joinToString(", ")}")
            }

            // Language
            appendLine("Language Preference: ${profile.languagePreference}")

            // Contextual summary
            appendLine()
            append("This farmer cultivates ")
            append(formatCropsList(profile.primaryCrops))
            append(" on ")
            append(formatLandSize(profile.totalLandSize))
            append(" of ")
            append(profile.soilType.lowercase())
            append(" soil in ")
            append("${profile.district}, ${profile.state}.")
        }.trim()
    }

    /**
     * Convert FarmerProfile to a structured key-value format
     * Useful for metadata or structured embeddings
     *
     * @param profile The farmer profile to serialize
     * @return A structured text representation
     */
    fun toStructuredText(profile: FarmerProfile): String {
        return buildString {
            append("Name: ${profile.name} | ")
            append("Location: ${profile.village}, ${profile.district}, ${profile.state} | ")
            append("Land: ${formatLandSize(profile.totalLandSize)} | ")
            append("Soil: ${profile.soilType} | ")
            append("Crops: ${profile.primaryCrops.joinToString(", ")} | ")
            append("Language: ${profile.languagePreference}")
        }.trim()
    }

    /**
     * Generate a search-optimized text representation
     * Includes keywords and phrases useful for semantic search
     *
     * @param profile The farmer profile to serialize
     * @return A search-optimized text representation
     */
    fun toSearchOptimizedText(profile: FarmerProfile): String {
        return buildString {
            // Primary description
            append(toNaturalLanguage(profile))
            append(" ")

            // Add search keywords
            append("Keywords: ")
            append("farmer, ")
            append("${profile.state.lowercase()}, ")
            append("${profile.district.lowercase()}, ")
            append("${profile.soilType.lowercase()} soil, ")
            append(profile.primaryCrops.joinToString(", ") { it.lowercase() })
            append(", agriculture, ")
            append("${formatLandSize(profile.totalLandSize).lowercase()}")
            append(".")
        }.trim()
    }

    /**
     * Generate contextual farming information based on profile
     * Useful for personalized RAG context
     *
     * @param profile The farmer profile to serialize
     * @return Contextual farming information
     */
    fun toContextualDescription(profile: FarmerProfile): String {
        return buildString {
            append("This is a farmer profile for ${profile.name}, ")
            append("who practices agriculture in ${profile.district} district of ${profile.state}. ")

            // Land size context
            val landCategory = when {
                profile.totalLandSize < 2.0 -> "small-scale"
                profile.totalLandSize < 5.0 -> "medium-scale"
                else -> "large-scale"
            }
            append("The farmer operates a $landCategory farm with ${formatLandSize(profile.totalLandSize)}. ")

            // Soil context
            append("The agricultural land has ${profile.soilType.lowercase()} soil, ")
            append("which is suitable for growing ${formatCropsList(profile.primaryCrops)}. ")

            // Crop context
            if (profile.primaryCrops.size > 1) {
                append("The farmer practices crop diversification with multiple crops. ")
            }

            // Location context
            append("Being located in ${profile.state}, ")
            append("the farmer follows regional agricultural practices and seasonal patterns. ")

            // Communication
            append("The farmer prefers to communicate in ${profile.languagePreference}.")
        }.trim()
    }

    // Helper functions

    /**
     * Format land size with appropriate units and descriptive text
     */
    private fun formatLandSize(landSize: Double): String {
        return when {
            landSize < 0.5 -> String.format("%.2f hectares (small plot)", landSize)
            landSize < 1.0 -> String.format("%.2f hectares (marginal farm)", landSize)
            landSize < 2.0 -> String.format("%.2f hectares (small farm)", landSize)
            landSize < 5.0 -> String.format("%.2f hectares (medium farm)", landSize)
            landSize < 10.0 -> String.format("%.2f hectares (large farm)", landSize)
            else -> String.format("%.2f hectares (very large farm)", landSize)
        }
    }

    /**
     * Format crops list in natural language
     */
    private fun formatCropsList(crops: List<String>): String {
        return when (crops.size) {
            0 -> "various crops"
            1 -> crops[0]
            2 -> "${crops[0]} and ${crops[1]}"
            else -> {
                val allButLast = crops.dropLast(1).joinToString(", ")
                "$allButLast, and ${crops.last()}"
            }
        }
    }

    /**
     * Create a comprehensive profile summary combining all information
     * This is the recommended method for vector embeddings
     *
     * @param profile The farmer profile to serialize
     * @return A comprehensive summary suitable for embeddings
     */
    fun toEmbeddingSummary(profile: FarmerProfile): String {
        return buildString {
            // Title
            appendLine("=== Farmer Profile: ${profile.name} ===")
            appendLine()

            // Basic information
            appendLine(toNaturalLanguage(profile))
            appendLine()

            // Contextual information
            appendLine(toContextualDescription(profile))
            appendLine()

            // Structured data for precise retrieval
            appendLine("--- Profile Details ---")
            appendLine("Full Name: ${profile.name}")
            appendLine("Region: ${profile.village}, ${profile.district}, ${profile.state}")
            appendLine("Farm Size: ${profile.totalLandSize} hectares")
            appendLine("Soil Composition: ${profile.soilType}")
            appendLine("Cultivated Crops: ${profile.primaryCrops.joinToString(", ")}")
            appendLine("Communication Language: ${profile.languagePreference}")
        }.trim()
    }
}

