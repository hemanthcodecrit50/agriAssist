package com.krishisakhi.farmassistant.data

data class MarketPrice(
    val commodityName: String,
    val currentPrice: Float,
    val previousPrice: Float,
    val unit: String,
    val marketName:String,
    val category:String,
    val lastUpdated:String
)
