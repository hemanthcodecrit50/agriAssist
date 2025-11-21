package com.krishisakhi.farmassistant.network

import com.krishisakhi.farmassistant.data.GovtScheme
import com.krishisakhi.farmassistant.data.MarketPrice
import com.krishisakhi.farmassistant.data.PestAlert
import retrofit2.http.GET

interface ApiService {
    @GET(ApiConfig.PEST_ALERTS_ENDPOINT)
    suspend fun getPestAlerts(): List<PestAlert>

    @GET(ApiConfig.MARKET_PRICES_ENDPOINT)
    suspend fun getMarketPrices(): List<MarketPrice>

    @GET(ApiConfig.GOVT_SCHEMES_ENDPOINT)
    suspend fun getGovtSchemes(): List<GovtScheme>
}

