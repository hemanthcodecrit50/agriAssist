package com.krishisakhi.farmassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONObject
import java.util.Locale

class WeatherActivity : AppCompatActivity() {

    private lateinit var locationInput: EditText
    private lateinit var searchButton: Button
    private lateinit var currentLocationButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var weatherDataContainer: LinearLayout
    private lateinit var errorMessageText: TextView

    // Weather data views
    private lateinit var cityNameText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var weatherDescriptionText: TextView
    private lateinit var feelsLikeText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windSpeedText: TextView
    private lateinit var pressureText: TextView
    private lateinit var visibilityText: TextView

    // Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // OpenWeatherMap API Key from BuildConfig (secure)
    private val API_KEY = BuildConfig.OPENWEATHER_API_KEY
    private val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Weather Information"

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        initializeViews()
        setupClickListeners()

        // Load default weather for Bengaluru
        fetchWeatherData("Bengaluru")
    }

    private fun initializeViews() {
        locationInput = findViewById(R.id.locationInput)
        searchButton = findViewById(R.id.searchButton)
        currentLocationButton = findViewById(R.id.currentLocationButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        weatherDataContainer = findViewById(R.id.weatherDataContainer)
        errorMessageText = findViewById(R.id.errorMessageText)

        cityNameText = findViewById(R.id.cityNameText)
        temperatureText = findViewById(R.id.temperatureText)
        weatherDescriptionText = findViewById(R.id.weatherDescriptionText)
        feelsLikeText = findViewById(R.id.feelsLikeText)
        humidityText = findViewById(R.id.humidityText)
        windSpeedText = findViewById(R.id.windSpeedText)
        pressureText = findViewById(R.id.pressureText)
        visibilityText = findViewById(R.id.visibilityText)
    }

    private fun setupClickListeners() {
        searchButton.setOnClickListener {
            val city = locationInput.text.toString().trim()
            if (city.isNotEmpty()) {
                fetchWeatherData(city)
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        currentLocationButton.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun fetchWeatherData(city: String) {
        showLoading()

        val url = "$BASE_URL?q=${city}&units=metric&appid=${API_KEY}"
        println("Weather API URL: $url")

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                println("Weather API Response: $response")
                parseWeatherData(response)
                showWeatherData()
            },
            { error ->
                showError()
                val errorMessage = when {
                    error.networkResponse != null -> {
                        val statusCode = error.networkResponse.statusCode
                        val responseData = error.networkResponse.data?.let { String(it) }
                        println("API Error - Status: $statusCode, Response: $responseData")
                        when (statusCode) {
                            401 -> "Invalid API key. Please check your configuration."
                            404 -> "City not found. Please check the city name."
                            429 -> "Too many requests. Please try again later."
                            else -> "Server error (Code: $statusCode). Please try again."
                        }
                    }
                    error.cause != null -> {
                        println("Network Error: ${error.cause?.message}")
                        "Network error: ${error.cause?.message}"
                    }
                    else -> {
                        println("Unknown Error: ${error.message}")
                        "Unable to fetch weather data. Check your internet connection."
                    }
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )

        // Set retry policy with increased timeout
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            30000, // 30 seconds timeout
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

    private fun parseWeatherData(response: JSONObject) {
        try {
            // City name
            val cityName = response.getString("name")

            // Main weather data
            val main = response.getJSONObject("main")
            val temp = main.getDouble("temp")
            val feelsLike = main.getDouble("feels_like")
            val humidity = main.getInt("humidity")
            val pressure = main.getInt("pressure")

            // Weather description
            val weatherArray = response.getJSONArray("weather")
            val weather = weatherArray.getJSONObject(0)
            val description = weather.getString("description")

            // Wind data
            val wind = response.getJSONObject("wind")
            val windSpeedMps = wind.getDouble("speed")
            val windSpeedKmh = windSpeedMps * 3.6 // Convert m/s to km/h

            // Visibility
            val visibility = response.getInt("visibility") / 1000 // Convert to km

            // Update UI
            cityNameText.text = cityName
            temperatureText.text = "${temp.toInt()}°C"
            weatherDescriptionText.text = description.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            feelsLikeText.text = "Feels like ${feelsLike.toInt()}°C"
            humidityText.text = "$humidity%"
            windSpeedText.text = "${windSpeedKmh.toInt()} km/h"
            pressureText.text = "$pressure hPa"
            visibilityText.text = "$visibility km"

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
        weatherDataContainer.visibility = View.GONE
        errorMessageText.visibility = View.GONE
    }

    private fun showWeatherData() {
        loadingIndicator.visibility = View.GONE
        weatherDataContainer.visibility = View.VISIBLE
        errorMessageText.visibility = View.GONE
    }

    private fun showError() {
        loadingIndicator.visibility = View.GONE
        weatherDataContainer.visibility = View.GONE
        errorMessageText.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getCurrentLocation() {
        // Check if location permissions are granted
        if (checkLocationPermissions()) {
            // Check if location services are enabled
            if (!isLocationEnabled()) {
                showLocationSettingsAlert()
                return
            }
            fetchCurrentLocation()
        } else {
            requestLocationPermissions()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        println("GPS Enabled: $gpsEnabled, Network Enabled: $networkEnabled")
        return gpsEnabled || networkEnabled
    }

    private fun showLocationSettingsAlert() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Location Services Disabled")
            .setMessage("Location services are turned off. Please enable GPS or Network location to use this feature.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchCurrentLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required to get current location",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun fetchCurrentLocation() {
        println("fetchCurrentLocation: Started")

        if (!checkLocationPermissions()) {
            println("fetchCurrentLocation: Permission not granted")
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        println("fetchCurrentLocation: Permissions granted, fetching location...")
        showLoading()
        Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show()

        try {
            // Try with high accuracy first, then fall back to balanced power
            tryFetchLocationWithPriority(Priority.PRIORITY_HIGH_ACCURACY)
        } catch (e: SecurityException) {
            println("fetchCurrentLocation: SecurityException caught")
            e.printStackTrace()
            showError()
            Toast.makeText(this, "Location permission error: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            println("fetchCurrentLocation: General exception caught")
            e.printStackTrace()
            showError()
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryFetchLocationWithPriority(priority: Int) {
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            priority,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            println("Location fetch: Success callback triggered with priority $priority")
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                println("Location fetch: Got location - Lat: $latitude, Lon: $longitude")

                // Get city name from coordinates using Geocoder
                getCityNameFromCoordinates(latitude, longitude)

                // Fetch weather using coordinates
                fetchWeatherByCoordinates(latitude, longitude)
            } else {
                println("Location fetch: Location object is null, trying fallback...")
                // Try with lower priority if high accuracy returned null
                if (priority == Priority.PRIORITY_HIGH_ACCURACY) {
                    tryFetchLocationWithPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                } else {
                    // If both attempts fail, try getting last known location
                    tryGetLastKnownLocation()
                }
            }
        }.addOnFailureListener { exception ->
            println("Location fetch: Failure callback triggered")
            println("Location fetch: Exception: ${exception.message}")
            exception.printStackTrace()

            // Try with lower priority if high accuracy failed
            if (priority == Priority.PRIORITY_HIGH_ACCURACY) {
                println("Location fetch: Retrying with balanced power accuracy...")
                tryFetchLocationWithPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            } else {
                // If both attempts fail, try getting last known location
                tryGetLastKnownLocation()
            }
        }
    }

    private fun tryGetLastKnownLocation() {
        println("Trying to get last known location...")
        try {
            if (!checkLocationPermissions()) {
                showError()
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
                return
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    println("Got last known location - Lat: $latitude, Lon: $longitude")

                    getCityNameFromCoordinates(latitude, longitude)
                    fetchWeatherByCoordinates(latitude, longitude)
                } else {
                    println("Last known location is also null")
                    showError()
                    showManualLocationDialog()
                }
            }.addOnFailureListener { exception ->
                println("Last known location failed: ${exception.message}")
                showError()
                showManualLocationDialog()
            }
        } catch (e: SecurityException) {
            println("Security exception in last known location: ${e.message}")
            showError()
            Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showManualLocationDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Unable to Get Location")
            .setMessage("Cannot detect your location automatically. This might be because:\n\n" +
                    "• Location services are disabled\n" +
                    "• GPS needs time to acquire a signal\n" +
                    "• No location history on this device\n\n" +
                    "Would you like to enter your city name manually?")
            .setPositiveButton("Enter Manually") { dialog, _ ->
                dialog.dismiss()
                locationInput.requestFocus()
                Toast.makeText(this, "Please type your city name and tap Search", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Try Again") { dialog, _ ->
                dialog.dismiss()
                getCurrentLocation()
            }
            .setNeutralButton("Enable GPS") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .show()
    }

    private fun getCityNameFromCoordinates(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
                if (!cityName.isNullOrEmpty()) {
                    locationInput.setText(cityName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue with coordinates-based weather fetch even if geocoding fails
        }
    }

    private fun fetchWeatherByCoordinates(latitude: Double, longitude: Double) {
        val url = "$BASE_URL?lat=$latitude&lon=$longitude&units=metric&appid=$API_KEY"
        println("Weather API URL (Coordinates): $url")

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                println("Weather API Response: $response")
                parseWeatherData(response)
                showWeatherData()
            },
            { error ->
                showError()
                val errorMessage = when {
                    error.networkResponse != null -> {
                        val statusCode = error.networkResponse.statusCode
                        val responseData = error.networkResponse.data?.let { String(it) }
                        println("API Error - Status: $statusCode, Response: $responseData")
                        when (statusCode) {
                            401 -> "Invalid API key. Please check your configuration."
                            404 -> "Location not found. Please try again."
                            429 -> "Too many requests. Please try again later."
                            else -> "Server error (Code: $statusCode). Please try again."
                        }
                    }
                    error.cause != null -> {
                        println("Network Error: ${error.cause?.message}")
                        "Network error: ${error.cause?.message}"
                    }
                    else -> {
                        println("Unknown Error: ${error.message}")
                        "Unable to fetch weather data. Check your internet connection."
                    }
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )

        // Set retry policy with increased timeout
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            30000, // 30 seconds timeout
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
}