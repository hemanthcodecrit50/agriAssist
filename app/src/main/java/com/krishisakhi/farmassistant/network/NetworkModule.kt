package com.krishisakhi.farmassistant.network

import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krishisakhi.farmassistant.data.GovtScheme
import com.krishisakhi.farmassistant.data.MarketPrice
import com.krishisakhi.farmassistant.data.PestAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        val url = ApiConfig.BASE_URL.trimEnd('/') + "/" + ApiConfig.PEST_ALERTS_ENDPOINT.trimStart('/')
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${'$'}{resp.code} - ${'$'}{resp.message}")
            val body = resp.body?.string() ?: throw Exception("Empty response body")
            val listType = object : TypeToken<List<PestAlert>>() {}.type
            return@withContext gson.fromJson<List<PestAlert>>(body, listType)
        }
    }

    // Accept optional commodity and state query params
    suspend fun fetchMarketPrices(commodity: String? = null, state: String? = null): List<MarketPrice> = withContext(Dispatchers.IO) {
        val base = ApiConfig.BASE_URL.trimEnd('/')
        val baseUrl = base.toHttpUrlOrNull() ?: throw Exception("Invalid BASE_URL: ${'$'}{ApiConfig.BASE_URL}")
        val urlBuilder = baseUrl.newBuilder()
            .addPathSegment(ApiConfig.MARKET_PRICES_ENDPOINT.trimStart('/'))

        if (!commodity.isNullOrBlank()) urlBuilder.addQueryParameter("commodity", commodity)
        if (!state.isNullOrBlank()) urlBuilder.addQueryParameter("state", state)

        val url = urlBuilder.build().toString()
        println(url)
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${'$'}{resp.code} - ${'$'}{resp.message}")
            val body = resp.body?.string() ?: throw Exception("Empty response body")
            val listType = object : TypeToken<List<MarketPrice>>() {}.type
            return@withContext gson.fromJson<List<MarketPrice>>(body, listType)
        }
    }

    suspend fun fetchGovtSchemes(): List<GovtScheme> = withContext(Dispatchers.IO) {
        val url = ApiConfig.BASE_URL.trimEnd('/') + "/" + ApiConfig.GOVT_SCHEMES_ENDPOINT.trimStart('/')
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${'$'}{resp.code} - ${'$'}{resp.message}")
            val body = resp.body?.string() ?: throw Exception("Empty response body")
            val listType = object : TypeToken<List<GovtScheme>>() {}.type
            return@withContext gson.fromJson<List<GovtScheme>>(body, listType)
        }
    }
}
