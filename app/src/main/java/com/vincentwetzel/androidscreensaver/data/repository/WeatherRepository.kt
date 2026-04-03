package com.vincentwetzel.androidscreensaver.data.repository

import android.location.Location
import com.vincentwetzel.androidscreensaver.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Weather data class
 */
data class WeatherData(
    val temperature: Float,
    val condition: String,
    val conditionCode: Int,
    val humidity: Int,
    val windSpeed: Float,
    val precipitationChance: Int,
    val feelsLike: Float,
    val isDaytime: Boolean
)

/**
 * Open-Meteo Weather Repository
 * Free weather API - no API key required
 * https://open-meteo.com/
 */
@Singleton
class WeatherRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Get current weather for a location
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return WeatherData or null if failed
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=$latitude&longitude=$longitude" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                        "precipitation_probability,weather_code,wind_speed_10m,is_day" +
                        "&temperature_unit=celsius" +
                        "&wind_speed_unit=ms"

                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    val json = JSONObject(response.body?.string() ?: return@withContext null)
                    val current = json.getJSONObject("current")

                    WeatherData(
                        temperature = current.getDouble("temperature_2m").toFloat(),
                        condition = getWeatherCondition(current.getInt("weather_code")),
                        conditionCode = current.getInt("weather_code"),
                        humidity = current.getInt("relative_humidity_2m"),
                        windSpeed = current.getDouble("wind_speed_10m").toFloat(),
                        precipitationChance = current.getInt("precipitation_probability"),
                        feelsLike = current.getDouble("apparent_temperature").toFloat(),
                        isDaytime = current.getInt("is_day") == 1
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Convert WMO weather code to human-readable condition
     */
    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Unknown"
        }
    }

    /**
     * Check if weather API is accessible
     */
    suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.open-meteo.com")
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
