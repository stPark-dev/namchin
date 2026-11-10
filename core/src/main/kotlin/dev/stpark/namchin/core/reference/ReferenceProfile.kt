package dev.stpark.namchin.core.reference

import dev.stpark.namchin.core.framing.PersonFrame
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.pose.PoseSimilarity

/** 거리 비교에 쓸 기준 길이. */
enum class SizeBasis { TORSO, NECK, NONE }

/** 레퍼런스가 촬영 가이드에 얼마나 쓸모 있는지 요약. */
data class ReferenceProfile(
    val personFound: Boolean,
    val comparableParts: Int,
    val poseComparable: Boolean,
    val sizeBasis: SizeBasis,
) {
    companion object {
        fun of(pose: PersonPose?): ReferenceProfile {
            if (pose == null) return ReferenceProfile(false, 0, false, SizeBasis.NONE)
            val frame = PersonFrame.from(pose)
            return ReferenceProfile(
                personFound = true,
                comparableParts = PoseSimilarity.comparablePartCount(pose),
                poseComparable = PoseSimilarity.isComparable(pose),
                sizeBasis = when {
                    frame?.torso != null -> SizeBasis.TORSO
                    frame?.neck != null -> SizeBasis.NECK
                    else -> SizeBasis.NONE
                },
            )
        }
    }
}
