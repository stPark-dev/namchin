package dev.stpark.namchin.camera

/** 셔터를 눌러도 되는가. 화면에서 고른 레퍼런스와 실제 적용된 레퍼런스가 같아야 저장 비율이 맞는다. */
object ShutterGate {
    fun canShoot(selectedId: Long?, activeId: Long?, cameraReady: Boolean, capturing: Boolean): Boolean =
        cameraReady && !capturing && selectedId == activeId
}
