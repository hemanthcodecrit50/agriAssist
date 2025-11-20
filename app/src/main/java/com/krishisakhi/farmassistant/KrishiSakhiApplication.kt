package com.krishisakhi.farmassistant

import android.app.Application
import android.util.Log
import com.krishisakhi.farmassistant.rag.VectorDatabasePersistent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for app-wide initialization
 * Initializes the persistent vector database at startup
 */
class KrishiSakhiApplication : Application() {

    companion object {
        private const val TAG = "KrishiSakhiApplication"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "KrishiSakhi Application starting...")

        // Initialize persistent vector database in background
        applicationScope.launch {
            try {
                val vectorDb = VectorDatabasePersistent(applicationContext)
                val success = vectorDb.initialize()

                if (success) {
                    val count = vectorDb.getVectorCount()
                    Log.d(TAG, "Persistent vector database initialized with $count vectors")
                } else {
                    Log.e(TAG, "Failed to initialize persistent vector database")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing persistent vector database", e)
            }
        }
    }
}

