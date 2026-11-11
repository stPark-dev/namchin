package dev.stpark.namchin.review

import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File

/**
 * 필터 입힌 JPEG에 원본 사진 정보(찍은 날짜·기종·노출·위치)를 옮겨 붙인다.
 * 픽셀은 이미 똑바로 돌려 놓았으므로 회전 정보는 "똑바로"로 둔다
 * (원본의 "90도 돌려 보라"가 남으면 갤러리가 한 번 더 돌린다).
 */
object ExifCarry {
    private val TAGS = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
    )

    /** @param scratch 잠깐 쓸 파일을 둘 곳 (ExifInterface는 파일에만 저장할 수 있다) */
    fun carry(original: ByteArray, jpeg: ByteArray, scratch: File): ByteArray {
        val source = ExifInterface(ByteArrayInputStream(original))
        val file = File.createTempFile("exif-carry", ".jpg", scratch)
        try {
            file.writeBytes(jpeg)
            ExifInterface(file).apply {
                TAGS.forEach { tag -> source.getAttribute(tag)?.let { setAttribute(tag, it) } }
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
            return file.readBytes()
        } finally {
            file.delete()
        }
    }
}
