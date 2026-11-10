package dev.stpark.namchin.camera

import androidx.annotation.StringRes
import dev.stpark.namchin.R
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import dev.stpark.namchin.core.model.BodyPart

/** 안내 → 화면/음성 문구. */
object GuidanceText {
    @StringRes
    fun of(cue: Guidance): Int = when (cue) {
        Guidance.NoPerson -> R.string.guide_no_person
        is Guidance.Level -> when (cue.rotation) {
            Rotation.CLOCKWISE -> R.string.guide_level_cw
            Rotation.COUNTERCLOCKWISE -> R.string.guide_level_ccw
        }
        Guidance.MoveCloser -> R.string.guide_move_closer
        Guidance.MoveBack -> R.string.guide_move_back
        Guidance.TiltUp -> R.string.guide_tilt_up
        Guidance.TiltDown -> R.string.guide_tilt_down
        Guidance.PanLeft -> R.string.guide_pan_left
        Guidance.PanRight -> R.string.guide_pan_right
        is Guidance.Pose -> when (cue.part) {
            BodyPart.LEFT_UPPER_ARM -> R.string.guide_pose_left_upper_arm
            BodyPart.LEFT_FOREARM -> R.string.guide_pose_left_forearm
            BodyPart.RIGHT_UPPER_ARM -> R.string.guide_pose_right_upper_arm
            BodyPart.RIGHT_FOREARM -> R.string.guide_pose_right_forearm
            BodyPart.LEFT_TORSO -> R.string.guide_pose_left_torso
            BodyPart.RIGHT_TORSO -> R.string.guide_pose_right_torso
            BodyPart.LEFT_THIGH -> R.string.guide_pose_left_thigh
            BodyPart.LEFT_SHIN -> R.string.guide_pose_left_shin
            BodyPart.RIGHT_THIGH -> R.string.guide_pose_right_thigh
            BodyPart.RIGHT_SHIN -> R.string.guide_pose_right_shin
        }
    }
}
