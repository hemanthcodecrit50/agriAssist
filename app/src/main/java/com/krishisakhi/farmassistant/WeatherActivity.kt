package com.krishisakhi.farmassistant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class WeatherActivity : AppCompatActivity() {

    private lateinit var locationInput: EditText
    private lateinit var searchButton: Button
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

    // OpenWeatherMap API Key - Replace with your actual API key
    private val API_KEY = "YOUR_API_KEY"
    private val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Weather Information"

        initializeViews()
        setupClickListeners()

        // Load default weather for Bengaluru
        fetchWeatherData("Bengaluru")
    }

    private fun initializeViews() {
        locationInput = findViewById(R.id.locationInput)
        searchButton = findViewById(R.id.searchButton)
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
    }

    private fun fetchWeatherData(city: String) {
        showLoading()

        val url = "$BASE_URL?q=${city}&units=metric&appid=${API_KEY}"
        println(url)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                parseWeatherData(response)
                showWeatherData()
            },
            { error ->
                showError()
                Toast.makeText(
                    this,
                    "Error: ${error.message ?: "Unable to fetch weather data"}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
            val windSpeed = wind.getDouble("speed")

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
            windSpeedText.text = "${windSpeed.toInt()} km/h"
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
}