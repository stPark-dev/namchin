package dev.stpark.namchin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** 밝은 화면: 흰 바탕 위 노랑 글씨는 안 보이므로 글자·버튼은 남색, 강조 면(FAB 등)은 노랑. */
private val Light = lightColorScheme(
    primary = Brand.Ink,
    onPrimary = Brand.Paper,
    primaryContainer = Brand.Yellow,
    onPrimaryContainer = Brand.Ink,
    secondary = Brand.Yellow,
    onSecondary = Brand.Ink,
    // 선택된 칩(태그) 바탕
    secondaryContainer = Brand.Yellow,
    onSecondaryContainer = Brand.Ink,
    tertiary = Brand.YellowDeep,
    background = Brand.Paper,
    onBackground = Brand.Ink,
    surface = Brand.Paper,
    onSurface = Brand.Ink,
    surfaceVariant = Brand.Yellow.copy(alpha = 0.12f),
    onSurfaceVariant = Brand.InkSoft,
    // 카드 바탕: 렌즈 노랑을 아주 연하게
    surfaceContainerHighest = Brand.PaperYellow,
    outline = Brand.Ink.copy(alpha = 0.3f),
    error = Brand.Danger,
)

private val Dark = darkColorScheme(
    primary = Brand.Yellow,
    onPrimary = Brand.Ink,
    primaryContainer = Brand.YellowDeep,
    onPrimaryContainer = Brand.Ink,
    secondary = Brand.Paper,
    onSecondary = Brand.Ink,
    tertiary = Brand.YellowDeep,
    background = Brand.Ink,
    onBackground = Brand.Paper,
    surface = Brand.Ink,
    onSurface = Brand.Paper,
    surfaceVariant = Brand.InkSoft,
    onSurfaceVariant = Brand.Paper.copy(alpha = 0.8f),
    outline = Brand.Paper.copy(alpha = 0.3f),
    error = Brand.Danger,
)

/** 브랜드 색(노랑·남색)과 둥근 글꼴. 기기 배경화면 색(동적 색상)은 쓰지 않는다. */
@Composable
fun NamchinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = NamchinTypography,
        content = content,
    )
}
