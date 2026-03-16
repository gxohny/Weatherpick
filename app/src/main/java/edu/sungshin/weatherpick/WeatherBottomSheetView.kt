package edu.sungshin.weatherpick

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout

class WeatherBottomSheetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        // 원래 쓰던 바텀시트 레이아웃을 이 Custom View 안에 붙이기
        LayoutInflater.from(context)
            .inflate(R.layout.layout_weather_bottom_sheet, this, true)
    }
}
