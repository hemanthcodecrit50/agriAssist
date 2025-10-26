package com.krishisakhi.farmassistant.data

data class GovtScheme(
    val schemeName: String,
    val authority: String,
    val category: String,
    val description: String,
    val eligibility: List<String>,
    val benefits: String,
    val deadline: String,
    val status: String
)
