package dev.stpark.namchin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.stpark.namchin.R

/** 로고처럼 둥글고 굵은 한글 글꼴 (주아, OFL). 제목·버튼·안내 문구에 쓴다. */
val Jua = FontFamily(Font(R.font.jua))

private val base = Typography()

val NamchinTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Jua),
    displayMedium = base.displayMedium.copy(fontFamily = Jua),
    displaySmall = base.displaySmall.copy(fontFamily = Jua),
    headlineLarge = base.headlineLarge.copy(fontFamily = Jua),
    headlineMedium = base.headlineMedium.copy(fontFamily = Jua),
    headlineSmall = base.headlineSmall.copy(fontFamily = Jua),
    titleLarge = base.titleLarge.copy(fontFamily = Jua),
    titleMedium = base.titleMedium.copy(fontFamily = Jua),
    titleSmall = base.titleSmall.copy(fontFamily = Jua),
    labelLarge = base.labelLarge.copy(fontFamily = Jua),
    labelMedium = base.labelMedium.copy(fontFamily = Jua),
    labelSmall = base.labelSmall.copy(fontFamily = Jua),
)
