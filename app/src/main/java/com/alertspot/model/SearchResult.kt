package com.alertspot.model

/** Search result returned by Geocoder lookups. */
data class SearchResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)
