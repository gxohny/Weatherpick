package edu.sungshin.weatherpick

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val API_KEY = "AIzaSyBfT-sl7CXS3A9vGU24WOYaiK51bMbwn-A"

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun requestSentence(
        temp: Double,
        region: String,
        style: String,
        onResult: (String) -> Unit
    ) {
        val prompt = """
            상황: $region, 기온 ${temp}도.
            
            [미션]
            위 날씨에 딱 어울리는 음식, 아이템, 소소한 행동 중 하나를 골라 친구에게 추천해줘.
            
            [절대 규칙]
            1. "지금 기온은 몇 도입니다" 같은 날씨 중계/설명 절대 금지.
            2. 바로 본론(추천)부터 말해.
            3. 말투는 "$style" 느낌으로 다정하게.
            4. 글자 수는 30자~70자 사이로, 문장이 중간에 끊기지 않게 완결해줘.
        """.trimIndent()

        val json = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 1000)
                put("temperature", 0.7)
            })
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY"

        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Gemini", "통신 실패: ${e.message}")
                onResult(getFallbackSentence(temp))
            }

            override fun onResponse(call: Call, response: Response) {
                // 429 처리
                if (response.code == 429) {
                    Log.w("Gemini", "429 Quota exceeded")
                    onResult("__429__")   // 나중에 Activity에서 구분용
                    response.close()
                    return
                }

                val responseString = response.body?.string() ?: ""
                Log.d("GeminiRaw", responseString)

                try {
                    val jsonResponse = JSONObject(responseString)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        onResult(getFallbackSentence(temp))
                        return
                    }

                    val partsArray = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")

                    val sb = StringBuilder()
                    for (i in 0 until partsArray.length()) {
                        val partText = partsArray
                            .getJSONObject(i)
                            .optString("text", "")
                        if (partText.isNotBlank()) sb.append(partText).append(" ")
                    }

                    var text = sb.toString().trim()
                    text = text
                        .replace("\n", " ")
                        .replace("\\s+".toRegex(), " ")
                        .trim()

                    if (text.length < 5) {
                        onResult(getFallbackSentence(temp))
                    } else {
                        onResult(text)
                    }

                } catch (e: Exception) {
                    Log.e("Gemini", "파싱 실패: ${e.message}")
                    onResult(getFallbackSentence(temp))
                }
            }
        })
    }

    private fun getFallbackSentence(temp: Double): String {
        return when {
            temp < 5 -> "손이 꽁꽁 얼 것 같아요! 핫팩 꼭 챙기시고 따뜻한 코코아 한 잔 어때요? 🍫"
            temp < 15 -> "쌀쌀한 공기가 느껴져요. 가디건이나 얇은 외투 챙겨 입으세요! 🧥"
            temp > 28 -> "너무 덥죠? 시원한 아이스 아메리카노 마시면서 더위 식혀보세요! 🧊"
            else -> "산책하기 완벽한 날씨예요! 좋아하는 노래 들으면서 걸어볼까요? 🎵"
        }
    }
}
