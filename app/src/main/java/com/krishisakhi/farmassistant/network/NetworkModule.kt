package com.krishisakhi.farmassistant.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krishisakhi.farmassistant.data.GovtScheme
import com.krishisakhi.farmassistant.data.MarketPrice
import com.krishisakhi.farmassistant.data.PestAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchPestAlerts(): List<PestAlert> = withContext(Dispatchers.IO) {
        val url = ApiConfig.BASE_URL + ApiConfig.PEST_ALERTS_ENDPOINT
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${'$'}{resp.code} - ${'$'}{resp.message}")
            val body = resp.body?.string() ?: throw Exception("Empty response body")
            val listType = object : TypeToken<List<PestAlert>>() {}.type
            return@withContext gson.fromJson<List<PestAlert>>(body, listType)
        }
    }

    suspend fun fetchMarketPrices(): List<MarketPrice> = withContext(Dispatchers.IO) {
        val url = ApiConfig.BASE_URL + ApiConfig.MARKET_PRICES_ENDPOINT
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${'$'}{resp.code} - ${'$'}{resp.message}")
            val body = resp.body?.string() ?: throw Exception("Empty response body")
            val listType = object : TypeToken<List<MarketPrice>>() {}.type
            return@withContext gson.fromJson<List<MarketPrice>>(body, listType)
        }
    }

    suspend fun fetchGovtSchemes(): List<GovtScheme> = withContext(Dispatchers.IO) {
        val url = ApiConfig.BASE_URL + ApiConfig.GOVT_SCHEMES_ENDPOINT
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${'$'}{resp.code} - ${'$'}{resp.message}")
            val body = resp.body?.string() ?: throw Exception("Empty response body")
            val listType = object : TypeToken<List<GovtScheme>>() {}.type
            return@withContext gson.fromJson<List<GovtScheme>>(body, listType)
        }
    }
}
