package dev.stpark.namchin.core.model

/**
 * PersonPose <-> 문자열. 저장용.
 * 형식: `aspect|JOINT,x,y,conf;JOINT,x,y,conf;...`
 */
object PoseCodec {

    fun encode(pose: PersonPose): String =
        "${pose.aspectRatio}|" + pose.keypoints.values.joinToString(";") {
            "${it.joint.name},${it.position.x},${it.position.y},${it.confidence}"
        }

    /** 형식이 잘못되면 null. 모르는 관절 이름은 건너뛴다. */
    fun decode(text: String): PersonPose? = runCatching {
        val (aspect, body) = text.split("|", limit = 2).also { require(it.size == 2) }
        val keypoints = body.split(";").filter { it.isNotEmpty() }.mapNotNull { entry ->
            val parts = entry.split(",")
            require(parts.size == 4)
            val values = parts.drop(1).map { it.toFloat() }
            val joint = Joint.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            joint to Keypoint(joint, Point(values[0], values[1]), values[2])
        }.toMap()
        PersonPose(keypoints, aspect.toFloat())
    }.getOrNull()
}
