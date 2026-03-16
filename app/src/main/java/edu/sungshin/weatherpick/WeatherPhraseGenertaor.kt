package edu.sungshin.weatherpick

object WeatherPhraseGenerator {

    // 간단하게 날씨 상태별 리스트
    private val coldPhrases = listOf(
        "햇살은 맑지만 바람이 차가워요.\n외투 챙기는 거 잊지 마세요!",
        "오늘은 손이 꽁꽁 얼 수 있어요.\n장갑 꼭 챙겨요.",
        "실내와 실외 온도 차가 커요.\n감기 조심하세요!"
    )

    private val sunnyPhrases = listOf(
        "오늘은 산책하기 좋은 맑은 날이에요.",
        "햇살이 기분 좋은 하루예요.\n가벼운 옷차림으로도 충분해요.",
        "선크림 바르는 거 잊지 마세요!"
    )

    private val rainyPhrases = listOf(
        "우산 꼭 챙기세요.\n갑작스러운 소나기가 올 수 있어요.",
        "비 오는 날엔 따뜻한 음료 한 잔 어떠세요?",
        "길이 미끄러우니 조심해서 걸어가요."
    )

    fun getRandomPhrase(condition: String): String {
        val list = when (condition) {
            "COLD" -> coldPhrases
            "SUNNY" -> sunnyPhrases
            "RAINY" -> rainyPhrases
            else -> sunnyPhrases
        }
        return list.random()
    }
}
