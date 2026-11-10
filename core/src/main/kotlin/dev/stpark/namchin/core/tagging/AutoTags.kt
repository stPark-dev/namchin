package dev.stpark.namchin.core.tagging

import dev.stpark.namchin.core.crop.LumaImage
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import kotlin.math.abs

/** 사진 한 장에서 바로 판단할 수 있는 태그 (구도·시간·장소). */
object AutoTags {
    private const val MIN_CONFIDENCE = 0.5f

    /** 평균 밝기가 이보다 낮으면 야경. */
    const val NIGHT_LUMA = 70.0

    /** 허벅지가 수직에서 이 비율 이상 누우면 앉은 자세 (|가로| > 비율 × |세로|, 약 50도). */
    private const val SEATED_RATIO = 1.2f

    fun framing(pose: PersonPose?): Set<Tag> {
        if (pose == null) return emptySet()
        fun at(joint: Joint) = pose.reliable(joint, MIN_CONFIDENCE)
        val ankles = listOfNotNull(at(Joint.LEFT_ANKLE), at(Joint.RIGHT_ANKLE)).filter { it.x in 0f..1f && it.y in 0f..1f }
        val knees = listOfNotNull(at(Joint.LEFT_KNEE), at(Joint.RIGHT_KNEE))
        val shoulders = listOfNotNull(at(Joint.LEFT_SHOULDER), at(Joint.RIGHT_SHOULDER))
        val seated = listOf(Joint.LEFT_HIP to Joint.LEFT_KNEE, Joint.RIGHT_HIP to Joint.RIGHT_KNEE).any { (h, k) ->
            val hip = at(h)
            val knee = at(k)
            hip != null && knee != null && abs((knee.x - hip.x) * pose.aspectRatio) > SEATED_RATIO * abs(knee.y - hip.y)
        }
        return buildSet {
            if (ankles.size == 2) add(TagVocabulary.FULL_BODY)
            if (knees.isEmpty() && shoulders.isNotEmpty()) add(TagVocabulary.UPPER_BODY)
            if (seated) add(TagVocabulary.SEATED)
        }
    }

    fun time(image: LumaImage): Tag =
        if (image.data.average() < NIGHT_LUMA) TagVocabulary.NIGHT else TagVocabulary.DAY

    fun place(labels: Collection<String>): Set<Tag> =
        labels.mapNotNull { TagVocabulary.LABEL_TO_TAG[it.lowercase()] }.toSet()
}
