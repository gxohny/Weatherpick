package edu.sungshin.weatherpick.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("data/2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,      // 위도
        @Query("lon") lon: Double,      // 경도
        @Query("appid") apiKey: String, // API Key
        @Query("units") units: String = "metric", // 섭씨 온도
        @Query("lang") lang: String = "kr"        // 한국어 응답 요청
    ): Call<WeatherResponse>
}