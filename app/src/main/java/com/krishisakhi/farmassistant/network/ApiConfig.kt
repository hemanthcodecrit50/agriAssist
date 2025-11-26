package com.krishisakhi.farmassistant.network

import com.krishisakhi.farmassistant.BuildConfig

object ApiConfig {
    const val BASE_URL = BuildConfig.NGROK_URL
    const val PEST_ALERTS_ENDPOINT = "pestalerts"
    const val MARKET_PRICES_ENDPOINT = "marketprices"
    const val GOVT_SCHEMES_ENDPOINT = "govtschemes"
    const val NOTIFICATIONS_ENDPOINT = "notifications"
}
