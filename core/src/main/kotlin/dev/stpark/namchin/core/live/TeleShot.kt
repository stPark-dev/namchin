package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.guidance.Guidance

/**
 * 확대 촬영: 1배(광각)로 가까이서 찍으면 얼굴·몸이 왜곡되므로, 물러나서 설정한 배율로 당겨 찍게 한다.
 */
object TeleShot {
    /** 설정 배율의 이 비율 이상이면 확대한 것으로 본다 (핀치로 조금 덜 당겨도 됨) */
    const val ENOUGH = 0.8f

    /** 이보다 넓게(0.6배 등) 찍는 건 일부러 고른 것으로 보고 말하지 않는다 */
    const val DELIBERATE_WIDE = 0.9f

    /** @param target 설정의 기본 배율 (카메라 최대 배율 이하로 맞춰 줄 것). 1 이하면 확대 촬영을 쓰지 않음 */
    fun cue(zoomRatio: Float, target: Float, personVisible: Boolean): Guidance? =
        Guidance.TeleZoom(target).takeIf {
            target > 1f && personVisible && zoomRatio >= DELIBERATE_WIDE && zoomRatio < target * ENOUGH
        }

    /** 배율이 틀린 동안은 "다가가세요/물러나세요"가 의미 없으므로 뺀다. */
    fun withoutDistance(cues: List<Guidance>): List<Guidance> =
        if (cues.none { it is Guidance.TeleZoom }) cues
        else cues.filterNot { it == Guidance.MoveCloser || it == Guidance.MoveBack }
}
