package edu.sungshin.weatherpick

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.widget.FrameLayout
import edu.sungshin.weatherpick.network.RetrofitClient
import edu.sungshin.weatherpick.network.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi


class RegionSelectActivity : AppCompatActivity() {

    companion object {
        private const val WEATHER_API_KEY = "b7db4d9e2cb3cb8d3717d4c4ffed7aac"
        private const val LOCATION_REQUEST_CODE = 101
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var tvTodayDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvSentence: TextView
    private lateinit var btnRefresh: ImageView
    private lateinit var btnStar: ImageButton

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentSentence: String = ""
    private var currentLocationName: String = "서울특별시"

    private val MY_PAGE_ACTIVITY = MyPageActivity::class.java

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.region_select)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupBottomSheet()
        setupButtons()
        setupTodayInfo()

        // OS 위치 권한 팝업 요청
        checkLocationPermission()
    }

    private fun initViews() {
        val bottomSheet = findViewById<FrameLayout>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        tvTodayDate = findViewById(R.id.tv_today_date)
        tvLocation = findViewById(R.id.tv_location)
        tvTemperature = findViewById(R.id.tv_temperature)
        tvSentence = findViewById(R.id.tv_sentence)
        btnRefresh = findViewById(R.id.btn_refresh_sentence)
        btnStar = findViewById(R.id.btn_save_sentence)
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior.peekHeight = dpToPx(60)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun setupTodayInfo() {
        val df = SimpleDateFormat("yy.MM.dd", Locale.KOREA)
        tvTodayDate.text = df.format(Date())
        tvTemperature.text = "-"
        tvSentence.text = "지역을 선택하거나 위치를 허용하세요."
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupButtons() {
        val btnSeoul = findViewById<Button>(R.id.btn_seoul)
        val btnGyeonggi = findViewById<Button>(R.id.btn_gyeonggi)
        val btnGangwon = findViewById<Button>(R.id.btn_gangwon)
        val btnGyeongsang = findViewById<Button>(R.id.btn_gyeongsang)
        val btnJeolla = findViewById<Button>(R.id.btn_jeolla)
        val btnChungcheong = findViewById<Button>(R.id.btn_chungcheong)
        val btnJeju = findViewById<Button>(R.id.btn_jeju)

        val regionButtons = listOf(
            btnSeoul, btnGyeonggi, btnGangwon,
            btnGyeongsang, btnJeolla, btnChungcheong, btnJeju
        )

        val defaultTextColor = getColor(android.R.color.white)
        val selectedTextColor = Color.parseColor("#E14141")
        regionButtons.forEach { it.setTextColor(defaultTextColor) }

        // 지역 선택 → 미리 정의한 위/경도 → 날씨 API → MyPageActivity
        regionButtons.forEach { button ->
            button.setOnClickListener {
                regionButtons.forEach { it.setTextColor(defaultTextColor) }
                button.setTextColor(selectedTextColor)

                currentLocationName = button.text.toString()
                tvLocation.text = currentLocationName

                val (lat, lon) = getLatLonForRegion(currentLocationName)

                bottomSheetBehavior.isHideable = false
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                callWeatherApi(lat, lon, overrideRegionName = true)
            }
        }

        btnRefresh.setOnClickListener {
            currentSentence = "오늘 $currentLocationName 의 날씨를 다시 한 번 느껴보세요."
            tvSentence.text = currentSentence
        }

        btnStar.setOnClickListener {
            Toast.makeText(this, "즐겨찾기는 마이페이지에서 관리돼요.", Toast.LENGTH_SHORT).show()
        }
    }

    /** 1. OS 위치 권한 체크 + 팝업 **/
    private fun checkLocationPermission() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine) {
            // 이미 허용됨
            fetchLocationAndCallWeather()
        } else {
            // OS 기본 권한 팝업 띄우기
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }
    }

    /** 2. 권한 팝업 결과 처리 **/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndCallWeather()
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다. 지역 버튼으로 직접 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 3. 현재 위치 → 날씨 API **/
    private fun fetchLocationAndCallWeather() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocationName = "현재 위치"
                tvLocation.text = currentLocationName

                bottomSheetBehavior.isHideable = false
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                callWeatherApi(location.latitude, location.longitude, overrideRegionName = false)
            } else {
                Toast.makeText(this, "현재 위치를 찾지 못했습니다. 지역을 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "위치 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /** 4. 위/경도 기반 날씨 API 호출 **/
    private fun callWeatherApi(lat: Double, lon: Double, overrideRegionName: Boolean) {
        RetrofitClient.service.getWeather(lat, lon, WEATHER_API_KEY)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@RegionSelectActivity, "날씨 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val data = response.body() ?: return

                    val temp = data.main.temp.toInt()
                    val apiRegionName = data.name

                    val finalRegionName =
                        if (overrideRegionName) currentLocationName else apiRegionName

                    tvLocation.text = finalRegionName
                    tvTemperature.text = "${temp}°"

                    currentSentence = "지금 $finalRegionName 의 기온은 ${temp}도예요."
                    tvSentence.text = currentSentence

                    goToMyPage(
                        region = finalRegionName,
                        tempText = "${temp}°",
                        sentence = currentSentence
                    )
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Toast.makeText(this@RegionSelectActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /** 5. MyPageActivity로 이동 **/
    private fun goToMyPage(region: String, tempText: String, sentence: String) {
        val intent = Intent(this, MY_PAGE_ACTIVITY).apply {
            putExtra("selected_region", region)
            putExtra("selected_temperature", tempText)
            putExtra("selected_sentence", sentence)
            putExtra("selected_date", tvTodayDate.text.toString())
        }
        startActivity(intent)
        finish()
    }

    /** 6. 지역명 → 위/경도 매핑 **/
    private fun getLatLonForRegion(regionName: String): Pair<Double, Double> {
        return when (regionName) {
            "서울", "서울특별시" -> 37.5665 to 126.9780
            "경기", "경기도" -> 37.4138 to 127.5183
            "강원", "강원도" -> 37.8228 to 128.1555
            "경상", "경상도" -> 35.4606 to 128.2132
            "전라", "전라도" -> 35.7167 to 127.1448
            "충청", "충청도" -> 36.6357 to 127.4913
            "제주", "제주도" -> 33.4996 to 126.5312
            else -> 37.5665 to 126.9780
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
