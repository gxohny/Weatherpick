package edu.sungshin.weatherpick.network

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val weather: List<Weather>, // 날씨 상태
    val main: Main,             // 기온
    val name: String,           // 지역 이름
    val dt: Long,               // 데이터 측정 시간 (Unix Time)
    val sys: Sys,               // 국가 코드
    val timezone: Long          // 시차 (초 단위)
)

// 세부 데이터 1: 날씨 상태
data class Weather(
    val id: Int,          // 날씨 코드
    val main: String,     // 날씨 대분류 (Rain, Clear)
    val description: String, // 상세 설명 (light rain)
    val icon: String      // 날씨 아이콘 이름 (10d 등)
)

// 세부 데이터 2: 메인 정보
data class Main(
    val temp: Double,     // 현재 기온
    val humidity: Int,    // 습도
    @SerializedName("feels_like")
    val feelsLike: Double // 체감 온도
)

// 세부 데이터 3: 시스템 정보
data class Sys(
    val country: String   // 국가 코드 (KR)
)
