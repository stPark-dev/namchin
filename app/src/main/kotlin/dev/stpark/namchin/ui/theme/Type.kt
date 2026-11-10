package dev.stpark.namchin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.stpark.namchin.R

/** 로고처럼 둥글고 굵은 한글 글꼴 (주아, OFL). 브랜드가 드러나는 큰 제목·빈 화면에만 쓴다. */
val Jua = FontFamily(Font(R.font.jua))

private val base = Typography()

val NamchinTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Jua),
    displayMedium = base.displayMedium.copy(fontFamily = Jua),
    displaySmall = base.displaySmall.copy(fontFamily = Jua),
    headlineLarge = base.headlineLarge.copy(fontFamily = Jua),
    headlineMedium = base.headlineMedium.copy(fontFamily = Jua),
    headlineSmall = base.headlineSmall.copy(fontFamily = Jua),
    // 일반 UI(제목줄·버튼·본문)는 시스템 글꼴로 깔끔하게
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
