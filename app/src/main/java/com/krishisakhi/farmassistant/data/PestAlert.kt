package com.krishisakhi.farmassistant.data

data class PestAlert(
    val pestName: String,
    val severity: String,
    val cropType: String,
    val description: String,
    val symptoms: String,
    val treatment: String,
    val dateReported: String,
    val region: String
)
