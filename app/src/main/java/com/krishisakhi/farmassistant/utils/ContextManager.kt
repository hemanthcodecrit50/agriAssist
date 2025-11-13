package com.krishisakhi.farmassistant.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

/**
 * Context manager for location and seasonal awareness
 * Provides agricultural context for enhanced recommendations
 */
class ContextManager(private val context: Context) {

    companion object {
        private const val TAG = "ContextManager"
    }

    /**
     * Get current season based on date
     */
    fun getCurrentSeason(): Season {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Season.WINTER
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SUMMER
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER -> Season.MONSOON
            else -> Season.POST_MONSOON
        }
    }

    /**
     * Get agricultural season (Kharif/Rabi)
     */
    fun getAgriculturalSeason(): AgriculturalSeason {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER -> AgriculturalSeason.KHARIF
            Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH -> AgriculturalSeason.RABI
            else -> AgriculturalSeason.ZAID
        }
    }

    /**
     * Get current location (requires permission)
     */
    fun getCurrentLocation(): Location? {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Location permission not granted")
                return null
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            return null
        }
    }

    /**
     * Get region based on location
     */
    fun getRegion(location: Location?): Region {
        if (location == null) return Region.UNKNOWN

        val latitude = location.latitude
        val longitude = location.longitude

        return when {
            // North India
            latitude > 28 && longitude < 80 -> Region.NORTH_INDIA
            // South India
            latitude < 16 -> Region.SOUTH_INDIA
            // East India
            longitude > 85 -> Region.EAST_INDIA
            // West India
            longitude < 75 -> Region.WEST_INDIA
            // Central India
            else -> Region.CENTRAL_INDIA
        }
    }

    /**
     * Get context summary for RAG enhancement
     */
    fun getContextSummary(): String {
        val season = getCurrentSeason()
        val agriSeason = getAgriculturalSeason()
        val location = getCurrentLocation()
        val region = getRegion(location)

        return buildString {
            append("Current Context:\n")
            append("Season: $season\n")
            append("Agricultural Season: $agriSeason\n")
            append("Region: $region\n")
            if (location != null) {
                append("Location: ${String.format(Locale.US, "%.2f", location.latitude)}, ${String.format(Locale.US, "%.2f", location.longitude)}\n")
            }
        }
    }

    /**
     * Get recommended crops for current season and region
     */
    fun getSeasonalCrops(): List<String> {
        val agriSeason = getAgriculturalSeason()
        val location = getCurrentLocation()
        val region = getRegion(location)

        return when (agriSeason) {
            AgriculturalSeason.KHARIF -> when (region) {
                Region.NORTH_INDIA -> listOf("Rice", "Cotton", "Maize", "Bajra", "Jowar")
                Region.SOUTH_INDIA -> listOf("Rice", "Groundnut", "Cotton", "Maize")
                Region.EAST_INDIA -> listOf("Rice", "Jute", "Maize")
                Region.WEST_INDIA -> listOf("Cotton", "Groundnut", "Soybean", "Bajra")
                Region.CENTRAL_INDIA -> listOf("Soybean", "Cotton", "Maize", "Rice")
                Region.UNKNOWN -> listOf("Rice", "Cotton", "Maize")
            }
            AgriculturalSeason.RABI -> when (region) {
                Region.NORTH_INDIA -> listOf("Wheat", "Mustard", "Barley", "Chickpea")
                Region.SOUTH_INDIA -> listOf("Rice", "Groundnut", "Sunflower")
                Region.EAST_INDIA -> listOf("Wheat", "Mustard", "Chickpea")
                Region.WEST_INDIA -> listOf("Wheat", "Chickpea", "Mustard")
                Region.CENTRAL_INDIA -> listOf("Wheat", "Chickpea", "Lentil")
                Region.UNKNOWN -> listOf("Wheat", "Chickpea", "Mustard")
            }
            AgriculturalSeason.ZAID -> listOf("Watermelon", "Cucumber", "Muskmelon", "Vegetables")
        }
    }

    enum class Season {
        WINTER, SUMMER, MONSOON, POST_MONSOON
    }

    enum class AgriculturalSeason {
        KHARIF,     // June-September (Monsoon crops)
        RABI,       // October-March (Winter crops)
        ZAID        // April-May (Summer crops)
    }

    enum class Region {
        NORTH_INDIA,
        SOUTH_INDIA,
        EAST_INDIA,
        WEST_INDIA,
        CENTRAL_INDIA,
        UNKNOWN
    }
}

