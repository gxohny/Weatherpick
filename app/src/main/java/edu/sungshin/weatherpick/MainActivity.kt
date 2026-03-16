package edu.sungshin.weatherpick

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import edu.sungshin.weatherpick.network.RetrofitClient
import edu.sungshin.weatherpick.network.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermissionAndFetchWeather()
    }

    private fun checkPermissionAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        } else {
            goToRegionSelect()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callWeatherApi(location.latitude, location.longitude)
            } else {
                goToRegionSelect()
            }
        }
    }

    private fun callWeatherApi(lat: Double, lon: Double) {
        val apiKey = "b7db4d9e2cb3cb8d3717d4c4ffed7aac"

        RetrofitClient.service.getWeather(lat, lon, apiKey)
            .enqueue(object : Callback<WeatherResponse> {

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data == null) {
                            handleError("데이터 없음")
                            return
                        }

                        val temp = data.main.temp
                        val region = data.name

                        // 랜덤 스타일 설정
                        val randomStyle = listOf(
                            "유머러스하게", "시적으로", "친근하게", "간결하게", "격려하는",
                            "철학적으로", "동화처럼, "노래 가사처럼", "광고 카피처럼", "속담처럼"
                        ).random()

                        GeminiClient.requestSentence(temp, region, randomStyle) { aiSentence ->
                            runOnUiThread {
                                goToMyPage(
                                    sentence = aiSentence ?: "날씨 정보를 가져왔어요.",
                                    region = region,
                                    temp = "${temp.roundToInt()}°C"
                                )
                            }
                        }

                    } else {
                        handleError("API 응답 실패")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    handleError("네트워크 오류")
                }
            })
    }

    // 에러 발생 시 처리
    private fun handleError(msg: String) {
        GeminiClient.requestSentence(20.0, "서울", "친근하게") { aiSentence ->
            runOnUiThread {
                goToMyPage(
                    sentence = aiSentence ?: "연결 상태를 확인해주세요.",
                    region = "서울",
                    temp = "20°C"
                )
            }
        }
    }

    private fun goToRegionSelect() {
        val intent = Intent(this, RegionSelectActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToMyPage(sentence: String, region: String, temp: String) {
        val intent = Intent(this, MyPageActivity::class.java)

        intent.putExtra("ai_sentence", sentence)
        intent.putExtra("selected_region", region)
        intent.putExtra("selected_temperature", temp)


        startActivity(intent)
        finish()
    }
}