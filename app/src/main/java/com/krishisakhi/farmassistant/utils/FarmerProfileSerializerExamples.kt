package com.krishisakhi.farmassistant.utils

import com.krishisakhi.farmassistant.data.FarmerProfile

/**
 * Usage examples for FarmerProfileSerializer
 * Demonstrates different serialization methods for various use cases
 */
object FarmerProfileSerializerExamples {

    /**
     * Example farmer profile for demonstration
     */
    private val sampleProfile = FarmerProfile(
        phoneNumber = "919876543210",
        name = "Ram Singh",
        state = "Punjab",
        district = "Patiala",
        village = "Samana",
        totalLandSize = 5.5,
        soilType = "Loamy",
        primaryCrops = listOf("Wheat", "Rice", "Cotton"),
        languagePreference = "Hindi"
    )

    /**
     * EXAMPLE 1: Basic Natural Language Conversion
     * Use this for simple, compact embeddings
     */
    fun example1_BasicNaturalLanguage() {
        val text = FarmerProfileSerializer.toNaturalLanguage(sampleProfile)

        println("=== Basic Natural Language ===")
        println(text)
        println()

        // Output:
        // Farmer Profile: Ram Singh. Located in Samana village, Patiala district, Punjab state.
        // Owns 5.50 hectares (medium farm) of agricultural land. Soil type is loamy.
        // Primary crops grown are Wheat, Rice, and Cotton. Preferred language is Hindi.
    }

    /**
     * EXAMPLE 2: Detailed Natural Language
     * Use this for more comprehensive embeddings with richer context
     */
    fun example2_DetailedNaturalLanguage() {
        val text = FarmerProfileSerializer.toDetailedNaturalLanguage(sampleProfile)

        println("=== Detailed Natural Language ===")
        println(text)
        println()

        // Output includes structured information and contextual summary
    }

    /**
     * EXAMPLE 3: Structured Text Format
     * Use this for key-value style metadata
     */
    fun example3_StructuredText() {
        val text = FarmerProfileSerializer.toStructuredText(sampleProfile)

        println("=== Structured Text ===")
        println(text)
        println()

        // Output:
        // Name: Ram Singh | Location: Samana, Patiala, Punjab | Land: 5.50 hectares (medium farm) |
        // Soil: Loamy | Crops: Wheat, Rice, Cotton | Language: Hindi
    }

    /**
     * EXAMPLE 4: Search-Optimized Text
     * Use this for enhanced semantic search with keywords
     */
    fun example4_SearchOptimizedText() {
        val text = FarmerProfileSerializer.toSearchOptimizedText(sampleProfile)

        println("=== Search-Optimized Text ===")
        println(text)
        println()

        // Output includes natural language + search keywords
    }

    /**
     * EXAMPLE 5: Contextual Description
     * Use this for personalized RAG context
     */
    fun example5_ContextualDescription() {
        val text = FarmerProfileSerializer.toContextualDescription(sampleProfile)

        println("=== Contextual Description ===")
        println(text)
        println()

        // Output includes farming context and regional information
    }

    /**
     * EXAMPLE 6: Embedding Summary (RECOMMENDED for Vector Database)
     * Use this for comprehensive vector embeddings
     */
    fun example6_EmbeddingSummary() {
        val text = FarmerProfileSerializer.toEmbeddingSummary(sampleProfile)

        println("=== Embedding Summary (RECOMMENDED) ===")
        println(text)
        println()

        // Output includes all information in a well-structured format
    }
}

/**
 * INTEGRATION EXAMPLE: Using with KnowledgeBaseManager
 */
class FarmerProfileVectorIntegrationExample {

    /**
     * Add farmer profile to vector database
     */
    suspend fun addFarmerProfileToVectorDB(
        profile: FarmerProfile,
        knowledgeBase: com.krishisakhi.farmassistant.rag.KnowledgeBaseManager
    ) {
        // RECOMMENDED: Use toEmbeddingSummary for comprehensive context
        val profileContent = FarmerProfileSerializer.toEmbeddingSummary(profile)

        // Create metadata map
        val profileData = mapOf(
            "title" to "Farmer Profile: ${profile.name}",
            "content" to profileContent,
            "category" to "farmer_profile",
            "tags" to "profile,farmer,${profile.state},${profile.district},${profile.soilType},${profile.primaryCrops.joinToString(",")}",
            "name" to profile.name,
            "location" to "${profile.village}, ${profile.district}, ${profile.state}",
            "landSize" to profile.totalLandSize.toString(),
            "soilType" to profile.soilType,
            "crops" to profile.primaryCrops.joinToString(","),
            "language" to profile.languagePreference
        )

        // Add to knowledge base
        val success = knowledgeBase.addFarmerProfile(
            farmerId = profile.phoneNumber,
            profileContent = profileContent,
            profileData = profileData
        )

        if (success) {
            println("✓ Farmer profile vector added successfully")
        } else {
            println("✗ Failed to add farmer profile vector")
        }
    }

    /**
     * Update farmer profile in vector database
     */
    suspend fun updateFarmerProfileInVectorDB(
        profile: FarmerProfile,
        knowledgeBase: com.krishisakhi.farmassistant.rag.KnowledgeBaseManager
    ) {
        // Generate updated content
        val profileContent = FarmerProfileSerializer.toEmbeddingSummary(profile)

        // Create updated metadata
        val profileData = mapOf(
            "title" to "Farmer Profile: ${profile.name}",
            "content" to profileContent,
            "category" to "farmer_profile",
            "tags" to "profile,farmer,${profile.state},${profile.district},${profile.soilType},${profile.primaryCrops.joinToString(",")}",
            "name" to profile.name,
            "location" to "${profile.village}, ${profile.district}, ${profile.state}",
            "landSize" to profile.totalLandSize.toString(),
            "soilType" to profile.soilType,
            "crops" to profile.primaryCrops.joinToString(","),
            "language" to profile.languagePreference,
            "updated" to System.currentTimeMillis().toString()
        )

        // Update in knowledge base
        val success = knowledgeBase.updateFarmerProfile(
            farmerId = profile.phoneNumber,
            profileContent = profileContent,
            profileData = profileData
        )

        if (success) {
            println("✓ Farmer profile vector updated successfully")
        } else {
            println("✗ Failed to update farmer profile vector")
        }
    }
}

/**
 * USAGE IN REGISTRATIONACTIVITY
 */
/*
class RegistrationActivity : AppCompatActivity() {

    private fun saveProfile(profile: FarmerProfile) {
        lifecycleScope.launch {
            // 1. Save to Room database
            withContext(Dispatchers.IO) {
                db.farmerProfileDao().insertProfile(profile)
            }

            // 2. Create farmer profile vector using serializer
            val knowledgeBase = KnowledgeBaseManager(this@RegistrationActivity)

            // Use FarmerProfileSerializer to generate content
            val profileContent = FarmerProfileSerializer.toEmbeddingSummary(profile)

            val profileData = mapOf(
                "title" to "Farmer Profile: ${profile.name}",
                "content" to profileContent,
                "category" to "farmer_profile",
                "tags" to "profile,farmer,${profile.state}",
                "name" to profile.name,
                "location" to "${profile.village}, ${profile.district}, ${profile.state}",
                "landSize" to profile.totalLandSize.toString(),
                "soilType" to profile.soilType,
                "crops" to profile.primaryCrops.joinToString(",")
            )

            withContext(Dispatchers.IO) {
                knowledgeBase.addFarmerProfile(
                    farmerId = profile.phoneNumber,
                    profileContent = profileContent,
                    profileData = profileData
                )
            }

            // Navigate to MainActivity
            val intent = Intent(this@RegistrationActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }
}
*/

/**
 * USAGE IN PROFILEACTIVITY
 */
/*
class ProfileActivity : AppCompatActivity() {

    private fun updateProfile(updatedProfile: FarmerProfile) {
        lifecycleScope.launch {
            // 1. Update Room database
            withContext(Dispatchers.IO) {
                db.farmerProfileDao().insertProfile(updatedProfile)
            }

            // 2. Update farmer profile vector using serializer
            val knowledgeBase = KnowledgeBaseManager(this@ProfileActivity)

            // Use FarmerProfileSerializer to generate updated content
            val profileContent = FarmerProfileSerializer.toEmbeddingSummary(updatedProfile)

            val profileData = mapOf(
                "title" to "Farmer Profile: ${updatedProfile.name}",
                "content" to profileContent,
                "category" to "farmer_profile",
                "tags" to "profile,farmer,${updatedProfile.state}",
                "name" to updatedProfile.name,
                "location" to "${updatedProfile.village}, ${updatedProfile.district}, ${updatedProfile.state}",
                "landSize" to updatedProfile.totalLandSize.toString(),
                "soilType" to updatedProfile.soilType,
                "crops" to updatedProfile.primaryCrops.joinToString(",")
            )

            withContext(Dispatchers.IO) {
                knowledgeBase.updateFarmerProfile(
                    farmerId = updatedProfile.phoneNumber,
                    profileContent = profileContent,
                    profileData = profileData
                )
            }

            Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        }
    }
}
*/

/**
 * OUTPUT EXAMPLES
 */
object OutputExamples {

    const val BASIC_OUTPUT = """
Farmer Profile: Ram Singh. Located in Samana village, Patiala district, Punjab state. Owns 5.50 hectares (medium farm) of agricultural land. Soil type is loamy. Primary crops grown are Wheat, Rice, and Cotton. Preferred language is Hindi.
    """

    const val DETAILED_OUTPUT = """
Farmer Profile Information:

Name: Ram Singh
Location: Samana, Patiala, Punjab
Total Land Size: 5.50 hectares (medium farm)
Soil Type: Loamy
Primary Crops: Wheat, Rice, Cotton
Language Preference: Hindi

This farmer cultivates Wheat, Rice, and Cotton on 5.50 hectares (medium farm) of loamy soil in Patiala, Punjab.
    """

    const val EMBEDDING_SUMMARY_OUTPUT = """
=== Farmer Profile: Ram Singh ===

Farmer Profile: Ram Singh. Located in Samana village, Patiala district, Punjab state. Owns 5.50 hectares (medium farm) of agricultural land. Soil type is loamy. Primary crops grown are Wheat, Rice, and Cotton. Preferred language is Hindi.

This is a farmer profile for Ram Singh, who practices agriculture in Patiala district of Punjab. The farmer operates a medium-scale farm with 5.50 hectares (medium farm). The agricultural land has loamy soil, which is suitable for growing Wheat, Rice, and Cotton. The farmer practices crop diversification with multiple crops. Being located in Punjab, the farmer follows regional agricultural practices and seasonal patterns. The farmer prefers to communicate in Hindi.

--- Profile Details ---
Full Name: Ram Singh
Region: Samana, Patiala, Punjab
Farm Size: 5.5 hectares
Soil Composition: Loamy
Cultivated Crops: Wheat, Rice, Cotton
Communication Language: Hindi
    """
}

