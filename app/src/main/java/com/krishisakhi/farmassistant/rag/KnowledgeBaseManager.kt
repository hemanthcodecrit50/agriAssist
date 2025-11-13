package com.krishisakhi.farmassistant.rag

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages the knowledge base including initialization and updates
 */
class KnowledgeBaseManager(private val context: Context) {

    companion object {
        private const val TAG = "KnowledgeBaseManager"
        private const val PREFS_NAME = "knowledge_base_prefs"
        private const val KEY_INITIALIZED = "kb_initialized"
    }

    private val vectorDb = VectorDatabase(context)
    private val embeddingService = EmbeddingService(context)

    /**
     * Initialize knowledge base with sample agricultural data
     */
    suspend fun initializeKnowledgeBase(): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isInitialized = prefs.getBoolean(KEY_INITIALIZED, false)

            if (isInitialized) {
                val count = vectorDb.getDocumentCount()
                Log.d(TAG, "Knowledge base already initialized with $count documents")
                return@withContext true
            }

            Log.d(TAG, "Initializing knowledge base...")

            // Load sample documents
            val documents = getSampleDocuments()

            // Generate embeddings and insert documents
            for (doc in documents) {
                val embedding = embeddingService.generateEmbedding("${doc.title} ${doc.content}")
                val docWithEmbedding = doc.copy(embedding = embedding)
                vectorDb.insertDocument(docWithEmbedding)
            }

            // Mark as initialized
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()

            val count = vectorDb.getDocumentCount()
            Log.d(TAG, "Knowledge base initialized successfully with $count documents")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing knowledge base", e)
            false
        }
    }

    /**
     * Add a new document to the knowledge base
     */
    suspend fun addDocument(
        title: String,
        content: String,
        category: String,
        tags: List<String>
    ): Boolean {
        return try {
            val embedding = embeddingService.generateEmbedding("$title $content")
            val document = KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                category = category,
                tags = tags,
                embedding = embedding
            )
            vectorDb.insertDocument(document)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding document", e)
            false
        }
    }

    /**
     * Search for relevant documents
     */
    suspend fun search(
        query: String,
        topK: Int = 3,
        categoryFilter: String? = null
    ): List<SearchResult> {
        return try {
            val queryEmbedding = embeddingService.generateEmbedding(query)
            vectorDb.searchSimilar(queryEmbedding, topK, categoryFilter)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents", e)
            emptyList()
        }
    }

    /**
     * Get sample agricultural documents
     */
    private fun getSampleDocuments(): List<KnowledgeDocument> {
        return listOf(
            // Crop Cultivation
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Rice Cultivation Guide",
                content = """Rice cultivation requires flooded fields. Transplant 20-25 day old seedlings in rows 20x15 cm apart. 
                    Apply 120 kg N, 60 kg P2O5, and 40 kg K2O per hectare. Maintain 5-10 cm water depth during growth. 
                    Harvest when 80% of grains turn golden yellow.""".trimIndent(),
                category = "crop_cultivation",
                tags = listOf("rice", "paddy", "cultivation", "transplanting")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Wheat Growing Best Practices",
                content = """Sow wheat seeds in November using seed rate of 100-125 kg/ha. Row spacing should be 20-22 cm. 
                    Apply 120 kg N, 60 kg P2O5, 40 kg K2O per hectare. First irrigation at crown root stage (21 days), 
                    second at tillering, third at jointing, fourth at flowering. Harvest when moisture is 20-25%.""".trimIndent(),
                category = "crop_cultivation",
                tags = listOf("wheat", "gehu", "rabi", "cultivation")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Tomato Farming Techniques",
                content = """Tomato requires well-drained loamy soil with pH 6.0-7.0. Transplant 25-30 day old seedlings at 
                    60x45 cm spacing. Apply 150 kg N, 100 kg P2O5, 80 kg K2O per hectare. Stake plants for support. 
                    Water regularly but avoid waterlogging. Harvest when fruits are firm and fully colored.""".trimIndent(),
                category = "crop_cultivation",
                tags = listOf("tomato", "vegetable", "horticulture", "tamatar")
            ),

            // Pest and Disease
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Brown Plant Hopper in Rice",
                content = """Brown plant hopper (BPH) causes hopper burn in rice. Symptoms include yellowing and drying of plants. 
                    Control: Use resistant varieties, maintain proper spacing, avoid excessive nitrogen. Spray imidacloprid 
                    17.8% SL @ 100 ml/ha or thiamethoxam 25% WG @ 100 g/ha when population exceeds 10 hoppers per hill.""".trimIndent(),
                category = "pest_disease",
                tags = listOf("rice", "insect", "bph", "hopper", "pest")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Late Blight of Potato",
                content = """Late blight caused by Phytophthora infestans is devastating for potato. Water-soaked lesions appear 
                    on leaves and stems. Control: Use disease-free seed, earthing up, proper drainage. Spray mancozeb 75% WP 
                    @ 2.5 kg/ha or metalaxyl + mancozeb @ 2.5 kg/ha at 10-day intervals starting from disease appearance.""".trimIndent(),
                category = "pest_disease",
                tags = listOf("potato", "fungus", "blight", "disease", "aloo")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Fruit Borer in Tomato",
                content = """Fruit borer (Helicoverpa armigera) damages tomato fruits. Larvae bore into fruits leaving entry holes. 
                    Control: Remove damaged fruits, use pheromone traps @ 8/ha, spray neem oil 3% or Bt @ 1 kg/ha. 
                    Chemical control: Spinosad 45% SC @ 160 ml/ha or emamectin benzoate 5% SG @ 200 g/ha.""".trimIndent(),
                category = "pest_disease",
                tags = listOf("tomato", "insect", "borer", "pest", "fruit")
            ),

            // Fertilizer and Nutrients
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "NPK Fertilizer Application",
                content = """NPK stands for Nitrogen (N), Phosphorus (P), and Potassium (K). Apply nitrogen in splits - 1/3 at 
                    sowing, 1/3 at tillering, 1/3 at flowering. Apply full phosphorus and potassium as basal dose. 
                    For balanced nutrition, soil testing is recommended. General dose: 120:60:40 NPK kg/ha for cereals.""".trimIndent(),
                category = "fertilizer",
                tags = listOf("npk", "fertilizer", "nutrients", "khad")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Organic Manure Benefits",
                content = """Organic manure improves soil health and structure. Apply 10-15 tonnes FYM per hectare before sowing. 
                    Compost adds beneficial microorganisms. Vermicompost @ 2-3 tonnes/ha provides slow-release nutrients. 
                    Green manure crops like dhaincha, sunhemp add nitrogen and organic matter. Mix manure in top 15 cm soil.""".trimIndent(),
                category = "fertilizer",
                tags = listOf("organic", "manure", "fym", "compost", "jevik")
            ),

            // Irrigation
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Drip Irrigation System",
                content = """Drip irrigation saves 40-60% water compared to flood irrigation. Install drippers at 30-60 cm spacing 
                    depending on crop. Apply water daily for 1-2 hours. Suitable for vegetables, fruits, sugarcane. 
                    Advantages: Water saving, reduced weed growth, fertigation possible. Initial cost Rs 50,000-1,00,000 per acre.""".trimIndent(),
                category = "irrigation",
                tags = listOf("drip", "irrigation", "water", "sinchai", "saving")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Critical Irrigation Stages",
                content = """Crops need water most during critical stages. Wheat: CRI (21 days), tillering, jointing, flowering, 
                    grain filling. Rice: Transplanting, tillering, panicle initiation, flowering, grain filling. Cotton: 
                    Flowering and boll development. Insufficient water at these stages reduces yield significantly.""".trimIndent(),
                category = "irrigation",
                tags = listOf("irrigation", "critical", "stages", "water", "crop")
            ),

            // Soil Health
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Soil Testing Importance",
                content = """Soil testing determines nutrient status and pH. Collect samples from 0-15 cm depth from 10-15 spots, 
                    mix and send 500g to lab. Test for N, P, K, pH, organic carbon, micronutrients. Based on report, 
                    apply balanced fertilizers. Test soil every 2-3 years. Cost: Rs 200-500 per sample at government labs.""".trimIndent(),
                category = "soil_health",
                tags = listOf("soil", "testing", "nutrients", "mitti", "parikshan")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Soil pH Management",
                content = """Optimum pH for most crops is 6.5-7.5. Acidic soil (pH<6): Apply lime @ 5-10 quintals/ha. 
                    Alkaline soil (pH>8): Apply gypsum @ 10-15 quintals/ha or sulphur @ 500 kg/ha. Add organic matter 
                    to buffer pH changes. Check pH 6 months after amendment application.""".trimIndent(),
                category = "soil_health",
                tags = listOf("soil", "ph", "acidic", "alkaline", "lime")
            ),

            // Government Schemes
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "PM-KISAN Scheme Details",
                content = """PM-Kisan provides Rs 6000 per year to farmers in 3 installments of Rs 2000 each. Eligible: All 
                    landholding farmers. Register at pmkisan.gov.in with Aadhaar, bank account, land records. Amount 
                    credited directly to bank account. Check status online using Aadhaar or mobile number.""".trimIndent(),
                category = "govt_scheme",
                tags = listOf("pmkisan", "scheme", "subsidy", "government", "yojana")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Crop Insurance Scheme",
                content = """Pradhan Mantri Fasal Bima Yojana provides crop insurance. Premium: 2% for kharif, 1.5% for rabi, 
                    5% for horticultural crops. Covers yield loss due to natural calamities, pests, diseases. 
                    Enroll within 7 days of sowing at banks, CSCs, or online. Claims settled based on crop cutting experiments.""".trimIndent(),
                category = "govt_scheme",
                tags = listOf("insurance", "fasal", "bima", "crop", "yojana")
            ),

            // Weather and Climate
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Monsoon and Kharif Crops",
                content = """Southwest monsoon arrives in June and retreats by September. Kharif crops sown with monsoon onset. 
                    Major kharif crops: Rice, maize, cotton, soybean, groundnut, bajra, jowar. Require 50-100 cm rainfall. 
                    Sowing time: June-July. Harvesting: September-October. Plan sowing based on weather forecast.""".trimIndent(),
                category = "weather",
                tags = listOf("monsoon", "kharif", "rainfall", "season", "barish")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Winter Season Rabi Crops",
                content = """Rabi crops grown in winter season, sown in October-November, harvested in March-April. 
                    Major crops: Wheat, barley, mustard, chickpea, lentil, pea. Require cool growing period and irrigation. 
                    Temperature: 10-25°C optimal. Need 2-4 irrigations depending on rainfall. Frost sensitive at flowering.""".trimIndent(),
                category = "weather",
                tags = listOf("rabi", "winter", "season", "wheat", "sardi")
            ),

            // Market Prices
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Minimum Support Price System",
                content = """Government announces Minimum Support Price (MSP) for 23 crops before sowing season. MSP ensures 
                    minimum price for farmers. Major MSP crops: Paddy, wheat, pulses, oilseeds. Procurement by FCI and 
                    state agencies. Check MSP at agricoop.nic.in or eNAM portal. Farmers can sell to government at MSP.""".trimIndent(),
                category = "market_price",
                tags = listOf("msp", "price", "support", "government", "bhav")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Agricultural Marketing Tips",
                content = """Get best prices by: 1) Grading and cleaning produce, 2) Direct selling in mandi, 3) Using eNAM platform, 
                    4) Forming farmer groups for bulk selling, 5) Value addition (drying, processing), 6) Contract farming with 
                    companies. Check daily mandi prices on mobile apps. Avoid distress selling immediately after harvest.""".trimIndent(),
                category = "market_price",
                tags = listOf("marketing", "selling", "mandi", "price", "tips")
            ),

            // General Farming
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Crop Rotation Benefits",
                content = """Crop rotation improves soil health and breaks pest cycles. Recommended rotations: Rice-Wheat, 
                    Cotton-Wheat, Maize-Chickpea. Include legumes to add nitrogen. Alternate deep and shallow rooted crops. 
                    Benefits: Better soil structure, reduced pest/disease buildup, improved nutrient utilization, higher yields.""".trimIndent(),
                category = "general_farming",
                tags = listOf("rotation", "cropping", "soil", "health", "fasal")
            ),
            KnowledgeDocument(
                id = UUID.randomUUID().toString(),
                title = "Seed Treatment Importance",
                content = """Treat seeds before sowing to prevent seed-borne diseases. Methods: 1) Thiram or Captan @ 2-3 g/kg seed 
                    for fungal diseases, 2) Trichoderma @ 4-5 g/kg for biological control, 3) Imidacloprid @ 2 ml/kg for 
                    sucking pests. Dry treated seeds in shade before sowing. Seed treatment increases germination and vigor.""".trimIndent(),
                category = "general_farming",
                tags = listOf("seed", "treatment", "beej", "disease", "upchar")
            )
        )
    }

    /**
     * Reset knowledge base
     */
    suspend fun resetKnowledgeBase(): Boolean {
        return try {
            vectorDb.clearAll()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_INITIALIZED, false).apply()
            Log.d(TAG, "Knowledge base reset successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting knowledge base", e)
            false
        }
    }
}

