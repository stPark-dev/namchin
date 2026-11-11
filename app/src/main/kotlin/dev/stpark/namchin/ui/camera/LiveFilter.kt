package dev.stpark.namchin.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import dev.stpark.namchin.core.color.Filter
import dev.stpark.namchin.core.color.LutStrip

/**
 * 카메라 미리보기에 필터를 GPU로 입히는 효과.
 * 파랑 칸 두 개를 골라 (칸 안의 빨강·초록은 텍스처가 부드럽게 보간) 섞는다.
 */
object LiveFilter {
    private const val AGSL = """
        uniform shader content;
        uniform shader lut;
        uniform float n;
        half4 main(float2 p) {
            half4 c = content.eval(p);
            float3 s = clamp(float3(c.rgb), 0.0, 1.0) * (n - 1.0);
            float b0 = floor(s.b);
            float b1 = min(b0 + 1.0, n - 1.0);
            half3 lo = lut.eval(float2(b0 * n + s.r + 0.5, s.g + 0.5)).rgb;
            half3 hi = lut.eval(float2(b1 * n + s.r + 0.5, s.g + 0.5)).rgb;
            return half4(mix(lo, hi, half(s.b - b0)), c.a);
        }
    """

    /** 필터를 바꿀 때마다 변환표·셰이더를 다시 만들지 않게 필터별로 하나씩 (하나 약 140KB). 메인 스레드에서만 쓴다 */
    private val cache = java.util.EnumMap<Filter, RenderEffect>(Filter::class.java)

    /**
     * 화면 색 공간이 sRGB(기본)라는 전제: content 값이 0..1 sRGB여야 표의 좌표가 맞다.
     * 넓은 색역(P3)·HDR 모드를 켜면 다시 볼 것.
     */
    fun effect(filter: Filter): RenderEffect = cache.getOrPut(filter) { build(filter) }

    private fun build(filter: Filter): RenderEffect {
        val strip = LutStrip.of(filter.recipe::apply)
        val bitmap = Bitmap.createBitmap(strip.pixels, strip.width, strip.height, Bitmap.Config.ARGB_8888)
        val lut = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
        val shader = RuntimeShader(AGSL).apply {
            setInputShader("lut", lut)
            setFloatUniform("n", LutStrip.SIZE.toFloat())
        }
        return RenderEffect.createRuntimeShaderEffect(shader, "content")
    }
}
