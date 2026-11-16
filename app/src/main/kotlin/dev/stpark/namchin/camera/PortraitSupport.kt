package dev.stpark.namchin.camera

/**
 * 인물사진(배경 흐림, CameraX BOKEH 확장). 폰 제조사 카메라의 배경 흐림 처리를 빌려 쓴다.
 * 외부 앱용 배경 흐림 미리보기는 느려서(S23 Ultra 초당 약 5장), 평소엔 일반 카메라로 코칭하고
 * 셔터를 누른 순간에만 배경 흐림 세션으로 바꿔 한 장 찍고 돌아온다.
 */
object PortraitSupport {
    fun offered(bokeh: Boolean): Boolean = bokeh

    fun use(requested: Boolean, offered: Boolean): Boolean = requested && offered

    /** 셔터를 누르면 먼저 배경 흐림 세션으로 바꿔야 하나 (바뀐 뒤에 찍는다) */
    fun switchBeforeShot(portraitMode: Boolean, sessionActive: Boolean): Boolean = portraitMode && !sessionActive
}
