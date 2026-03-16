package edu.sungshin.weatherpick

import android.content.*
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import edu.sungshin.weatherpick.network.RetrofitClient
import edu.sungshin.weatherpick.network.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MyPageActivity : AppCompatActivity() {

    // 뷰 변수 선언
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var tvTodayDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvSentence: TextView
    private lateinit var btnRefresh: ImageView
    private lateinit var btnStar: ImageButton
    private lateinit var imgSavedWeather: ImageView
    private lateinit var tvSavedDate: TextView
    private lateinit var tvSavedSentence: TextView
    private lateinit var btnPrev: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var spRegion: Spinner

    private val regionList = listOf("서울", "경기", "강원", "경상", "전라", "충청", "제주")
    private var currentSentence: String = ""

    // 즐겨찾기 데이터 변수
    data class FavoriteItem(val date: String, val temp: String, val sentence: String)
    private var favoriteList: MutableList<FavoriteItem> = mutableListOf()
    private var currentFavoriteIndex: Int = 0

    // 네트워크 리시버
    private val networkReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context, intent: Intent?) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            findViewById<TextView>(R.id.tv_offline_banner).visibility = if (isConnected) View.GONE else View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        initViews() // 1. 뷰 연결
        applyIncomingWeatherData() // 2. 전달받은 데이터 세팅
        setupRegionSpinner() // 3. 지역 선택 설정
        loadFavorites() // 4. 즐겨찾기 로드
        displayCurrentFavorite() // 5. 하단 목록 표시

        // 이전/다음 버튼 리스너
        btnPrev.setOnClickListener {
            if (favoriteList.isNotEmpty()) {
                currentFavoriteIndex = if (currentFavoriteIndex > 0) currentFavoriteIndex - 1 else favoriteList.size - 1
                displayCurrentFavorite()
            }
        }
        btnNext.setOnClickListener {
            if (favoriteList.isNotEmpty()) {
                currentFavoriteIndex = (currentFavoriteIndex + 1) % favoriteList.size
                displayCurrentFavorite()
            }
        }

        // 바텀시트 동작 설정
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet))
        bottomSheetBehavior.peekHeight = dpToPx(60)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // 새로고침 버튼 클릭 (Gemini 호출)
        btnRefresh.setOnClickListener { requestNewSentenceFromGemini() }

        // 별표(즐겨찾기) 버튼 클릭
        btnStar.setOnClickListener { toggleFavoriteForCurrent() }
    }

    // --- 메뉴 및 로그아웃 ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    // --- 새로고침 (Gemini API 호출) ---
    private fun requestNewSentenceFromGemini() {
        val tempDouble = tvTemperature.text.toString().replace(Regex("[^0-9.-]"), "").toDoubleOrNull() ?: 15.0
        val region = tvLocation.text.toString()

        tvSentence.text = "AI가 문장을 새로 짓고 있어요..."
        tvSentence.setTextColor(Color.GRAY)

        GeminiClient.requestSentence(tempDouble, region, "감성적인") { result ->
            runOnUiThread {
                if (result != null) {
                    tvSentence.text = result
                    tvSentence.setTextColor(Color.BLACK)
                    currentSentence = result
                    syncStarWithSaved() // 별표 상태 갱신
                    Toast.makeText(this, "새 문장이 도착했습니다!", Toast.LENGTH_SHORT).show()
                } else {
                    tvSentence.text = "문장을 가져오지 못했습니다."
                    tvSentence.setTextColor(Color.RED)
                }
            }
        }
    }

    // --- 데이터 저장 및 로드 ---
    private fun loadFavorites() {
        val prefs = getSharedPreferences("weatherpick_prefs", MODE_PRIVATE)
        val set = prefs.getStringSet("saved_sentences", emptySet()) ?: emptySet()
        favoriteList = set.mapNotNull { raw ->
            val parts = raw.split("|")
            if (parts.size >= 3) FavoriteItem(parts[0], parts[1], parts.subList(2, parts.size).joinToString("|")) else null
        }.sortedByDescending { it.date }.toMutableList() // 최신순 정렬
    }

    private fun displayCurrentFavorite() {
        if (favoriteList.isEmpty()) {
            tvSavedSentence.text = "저장된 문구가 없습니다."
            tvSavedDate.text = ""
            btnPrev.alpha = 0.3f
            btnNext.alpha = 0.3f
        } else {
            val item = favoriteList[currentFavoriteIndex]
            tvSavedDate.text = item.date
            tvSavedSentence.text = item.sentence
            btnPrev.alpha = 1.0f
            btnNext.alpha = 1.0f
        }
    }

    private fun toggleFavoriteForCurrent() {
        if (currentSentence.isBlank() || currentSentence.contains("짓고 있어요")) return
        val key = "${tvTodayDate.text}|${tvTemperature.text}|$currentSentence"
        val prefs = getSharedPreferences("weatherpick_prefs", MODE_PRIVATE)
        val set = prefs.getStringSet("saved_sentences", mutableSetOf())!!.toMutableSet()

        if (set.contains(key)) {
            set.remove(key)
            Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            set.add(key)
            Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putStringSet("saved_sentences", set).apply()
        loadFavorites()
        if (favoriteList.isNotEmpty()) currentFavoriteIndex = 0 // 최신 항목으로 이동
        displayCurrentFavorite()
        syncStarWithSaved()
    }

    private fun syncStarWithSaved() {
        val key = "${tvTodayDate.text}|${tvTemperature.text}|$currentSentence"
        val saved = getSharedPreferences("weatherpick_prefs", MODE_PRIVATE).getStringSet("saved_sentences", emptySet()) ?: emptySet()
        btnStar.setColorFilter(if (saved.contains(key)) Color.parseColor("#FFC107") else Color.parseColor("#AAAAAA"))
    }

    // --- 기타 뷰 초기화 ---
    private fun initViews() {
        tvTodayDate = findViewById(R.id.tv_today_date); tvLocation = findViewById(R.id.tv_location)
        tvTemperature = findViewById(R.id.tv_temperature); tvSentence = findViewById(R.id.tv_sentence)
        btnRefresh = findViewById(R.id.btn_refresh_sentence); btnStar = findViewById(R.id.btn_save_sentence)
        imgSavedWeather = findViewById(R.id.img_saved_weather); tvSavedDate = findViewById(R.id.tv_saved_date)
        tvSavedSentence = findViewById(R.id.tv_saved_sentence); btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next); spRegion = findViewById(R.id.sp_region)
    }

    private fun applyIncomingWeatherData() {
        tvTodayDate.text = intent.getStringExtra("selected_date") ?: SimpleDateFormat("yy.MM.dd", Locale.KOREA).format(Date())
        tvLocation.text = intent.getStringExtra("selected_region") ?: "서울"
        tvTemperature.text = intent.getStringExtra("selected_temperature") ?: "15°C"
        currentSentence = intent.getStringExtra("ai_sentence") ?: ""
        tvSentence.text = currentSentence.ifBlank { "새로고침을 눌러주세요." }
        syncStarWithSaved()
    }

    private fun setupRegionSpinner() {
        spRegion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regionList)
        findViewById<ImageView>(R.id.ic_location).setOnClickListener {
            AlertDialog.Builder(this).setTitle("지역 선택").setItems(regionList.toTypedArray()) { _, which ->
                tvLocation.text = regionList[which]
                fetchWeatherForRegion(regionList[which])
            }.show()
        }
    }

    private fun fetchWeatherForRegion(region: String) {
        val (lat, lon) = when (region) {
            "서울" -> 37.5665 to 126.9780
            "경기" -> 37.2894 to 127.0530
            "강원" -> 37.8228 to 128.1555
            "경상" -> 35.8590 to 128.6007
            "전라" -> 35.7167 to 127.1448
            "충청" -> 36.6357 to 127.4917
            "제주" -> 33.4996 to 126.5312
            else -> 37.5665 to 126.9780
        }
        RetrofitClient.service.getWeather(lat, lon, "b7db4d9e2cb3cb8d3717d4c4ffed7aac")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        tvTemperature.text = "${response.body()?.main?.temp?.toInt() ?: 15}°C"
                        requestNewSentenceFromGemini()
                    }
                }
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {}
            })
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    override fun onStart() { super.onStart(); registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)) }
    override fun onStop() { super.onStop(); unregisterReceiver(networkReceiver) }
}