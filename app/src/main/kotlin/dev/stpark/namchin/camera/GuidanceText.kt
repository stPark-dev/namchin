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
        Guidance.HoldSteady -> R.string.guide_hold_steady
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
        Guidance.HeadCut -> R.string.guide_head_cut
        Guidance.FeetToBottom -> R.string.guide_feet_to_bottom
        Guidance.TooMuchHeadroom -> R.string.guide_too_much_headroom
        is Guidance.CutAtJoint -> when (cue.point) {
            Guidance.CutPoint.ANKLE -> R.string.guide_cut_ankle
            Guidance.CutPoint.KNEE -> R.string.guide_cut_knee
            Guidance.CutPoint.WRIST -> R.string.guide_cut_wrist
        }
        is Guidance.GazeSpace -> when (cue.side) {
            Guidance.Side.LEFT -> R.string.guide_gaze_left
            Guidance.Side.RIGHT -> R.string.guide_gaze_right
        }
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
