package dev.stpark.namchin.core.pose

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot

/**
 * 가장 어긋난 마디를 "어떻게" 고쳐야 하는지 (여자친구에게 전할 말).
 * 좌우·위아래는 화면 기준이 아니라 사람이 알아듣는 말로: 올려/내려, 바깥으로/몸 쪽으로, 굽혀/펴, 상체는 화면 기준 기울기.
 */
object PoseDirection {
    enum class Direction { RAISE, LOWER, OUTWARD, INWARD, BEND, STRAIGHTEN, LEAN_LEFT, LEAN_RIGHT }

    private const val MIN_CONFIDENCE = PoseSimilarity.MIN_CONFIDENCE

    /** 팔꿈치·무릎 굽힘이 이 각도 이상 다르면 "굽혀/펴"가 우선. */
    private const val BEND_DIFF_DEG = 25.0

    private data class Vec(val x: Double, val y: Double) {
        fun unit(): Vec = hypot(x, y).let { Vec(x / it, y / it) }
    }

    /**
     * @param part 현재 인물 기준 마디 (PoseMatch.worstPart)
     * @param mirrored 레퍼런스를 좌우반전해 비교했는지 (PoseMatch.mirrored)
     * @return 관절이 안 보이면 null
     */
    fun of(reference: PersonPose, current: PersonPose, part: BodyPart, mirrored: Boolean): Direction? {
        val refPart = if (mirrored) part.opposite() else part
        val cur = vector(current, part.from, part.to, flip = false) ?: return null
        val ref = vector(reference, refPart.from, refPart.to, flip = mirrored) ?: return null

        bendDirection(reference, current, part, refPart, mirrored)?.let { return it }

        val dx = ref.x - cur.x
        val dy = ref.y - cur.y
        if (part == BodyPart.LEFT_TORSO || part == BodyPart.RIGHT_TORSO) {
            // 어깨→골반 벡터가 왼쪽(음수)으로 가야 하면 어깨가 화면 오른쪽으로 → 상체를 오른쪽으로
            return if (dx < 0) Direction.LEAN_RIGHT else Direction.LEAN_LEFT
        }
        if (abs(dy) >= abs(dx)) return if (dy < 0) Direction.RAISE else Direction.LOWER
        // 인물의 왼쪽은 화면 오른쪽(+x)이 바깥, 오른쪽은 화면 왼쪽(-x)이 바깥
        val outwardSign = if (part.from.name.startsWith("LEFT")) 1 else -1
        return if (dx * outwardSign > 0) Direction.OUTWARD else Direction.INWARD
    }

    /** 아래팔·정강이: 팔꿈치·무릎을 얼마나 굽혔는지(위 마디와 아래 마디 사이 꺾임) 비교. */
    private fun bendDirection(
        reference: PersonPose,
        current: PersonPose,
        part: BodyPart,
        refPart: BodyPart,
        mirrored: Boolean,
    ): Direction? {
        val upper = upperOf(part) ?: return null
        val refUpper = upperOf(refPart) ?: return null
        val curBend = bend(vector(current, upper.from, upper.to, false), vector(current, part.from, part.to, false)) ?: return null
        val refBend = bend(vector(reference, refUpper.from, refUpper.to, mirrored), vector(reference, refPart.from, refPart.to, mirrored))
            ?: return null
        val diff = refBend - curBend
        return when {
            diff > BEND_DIFF_DEG -> Direction.BEND
            diff < -BEND_DIFF_DEG -> Direction.STRAIGHTEN
            else -> null
        }
    }

    private fun upperOf(part: BodyPart): BodyPart? = when (part) {
        BodyPart.LEFT_FOREARM -> BodyPart.LEFT_UPPER_ARM
        BodyPart.RIGHT_FOREARM -> BodyPart.RIGHT_UPPER_ARM
        BodyPart.LEFT_SHIN -> BodyPart.LEFT_THIGH
        BodyPart.RIGHT_SHIN -> BodyPart.RIGHT_THIGH
        else -> null
    }

    /** 두 마디 사이 꺾인 각도(도). 0 = 쭉 폄. */
    private fun bend(upper: Vec?, lower: Vec?): Double? {
        if (upper == null || lower == null) return null
        val dot = (upper.x * lower.x + upper.y * lower.y).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(dot))
    }

    private fun vector(pose: PersonPose, from: Joint, to: Joint, flip: Boolean): Vec? {
        val a = pose.reliable(from, MIN_CONFIDENCE) ?: return null
        val b = pose.reliable(to, MIN_CONFIDENCE) ?: return null
        val dx = (b.x - a.x).toDouble() * pose.aspectRatio
        val dy = (b.y - a.y).toDouble()
        if (dx == 0.0 && dy == 0.0) return null
        return Vec(if (flip) -dx else dx, dy).unit()
    }

    private fun BodyPart.opposite(): BodyPart =
        BodyPart.entries.first { it.from == from.opposite() && it.to == to.opposite() }
}
