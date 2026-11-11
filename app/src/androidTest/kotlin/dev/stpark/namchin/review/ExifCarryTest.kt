package dev.stpark.namchin.review

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExifCarryTest {
    private val cache: File = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir

    private fun jpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    /** 카메라가 찍은 것처럼: 가로로 누운 픽셀 + "90도 돌려 보라" + 찍은 날짜·기종·위치 */
    private fun cameraShot(): ByteArray {
        val file = File(cache, "exif-carry-original.jpg")
        file.writeBytes(jpeg(40, 30))
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:11:11 12:47:10")
            setAttribute(ExifInterface.TAG_MAKE, "samsung")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "37/1,30/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "127/1,0/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            saveAttributes()
        }
        return file.readBytes().also { file.delete() }
    }

    @Test
    fun theFilteredPhotoKeepsTheShotInfoButIsMarkedUpright() {
        val upright = jpeg(30, 40)

        val result = ExifCarry.carry(cameraShot(), upright, cache)

        val exif = ExifInterface(ByteArrayInputStream(result))
        assertEquals(ExifInterface.ORIENTATION_NORMAL, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1))
        assertEquals("2026:11:11 12:47:10", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertEquals("samsung", exif.getAttribute(ExifInterface.TAG_MAKE))
        val latLong = FloatArray(2)
        assertEquals(true, exif.getLatLong(latLong))
        assertEquals(37.5f, latLong[0], 0.001f)
        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(result, 0, result.size, size)
        assertEquals(30 to 40, size.outWidth to size.outHeight)
    }

    @Test
    fun anOriginalWithoutExifStillGivesAnUprightPhoto() {
        val result = ExifCarry.carry(jpeg(40, 30), jpeg(30, 40), cache)

        val exif = ExifInterface(ByteArrayInputStream(result))
        assertEquals(ExifInterface.ORIENTATION_NORMAL, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1))
    }
}
