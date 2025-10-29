package com.krishisakhi.farmassistant

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIService(private val apiKey: String) {

    companion object {
        private const val TAG = "GeminiAIService"
        // Change this model name to match your API key's supported model
        // Valid models: "gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro"
        // Note: "gemini-2.5-pro" does NOT exist - use 1.5 versions
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private val model = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048  // Increased from 1024 to allow longer responses
        }
    )

    interface AICallback {
        fun onSuccess(response: String)
        fun onError(error: String)
    }

    suspend fun generateResponse(userQuery: String, callback: AICallback) {
        try {
            Log.d(TAG, "Sending query to Gemini AI: $userQuery")

            // Add context for agricultural queries
            val contextualPrompt = """
                You are an agricultural assistant for farmers in India. 
                Answer the following farming question in a clear, concise manner (maximum 150 words).
                Provide the most important practical advice that farmers can easily understand.
                Focus on the key points. If the question is not about agriculture, politely redirect.
                
                Question: $userQuery
                
                Answer:
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                model.generateContent(contextualPrompt)
            }

            val responseText = response.text ?: "I couldn't generate a response. Please try again."
            Log.d(TAG, "Gemini AI Response: $responseText")

            withContext(Dispatchers.Main) {
                callback.onSuccess(responseText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI response", e)
            withContext(Dispatchers.Main) {
                callback.onError("AI Error: ${e.message}")
            }
        }
    }
}

