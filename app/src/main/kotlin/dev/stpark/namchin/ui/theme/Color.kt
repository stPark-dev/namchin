package dev.stpark.namchin.ui.theme

import androidx.compose.ui.graphics.Color

/** 앱 아이콘·로고에서 뽑은 브랜드 색. */
object Brand {
    /** 렌즈 노랑 — 주 색상 (셔터, 선택, 강조) */
    val Yellow = Color(0xFFFECF01)

    /** 조리개 안쪽의 진한 노랑 — 눌림, 보조 강조 */
    val YellowDeep = Color(0xFFF8A800)

    /** 외곽선·글자의 남색 — 카메라 배경, 글자 */
    val Ink = Color(0xFF131D2B)
    val InkSoft = Color(0xFF223047)

    /** 아이콘 바탕 흰색 */
    val Paper = Color(0xFFFDFDFD)
    val PaperYellow = Color(0xFFFFF6CC)

    /** 구도·포즈가 맞음 (골격) */
    val Success = Color(0xFF00E676)
    val Danger = Color(0xFFE53935)
}
