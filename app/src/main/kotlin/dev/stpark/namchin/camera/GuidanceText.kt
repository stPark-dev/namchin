package dev.stpark.namchin.camera

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.stpark.namchin.R
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.pose.PoseDirection.Direction

/** 안내 → 화면/음성 문구와 "왜?" 설명. */
object GuidanceText {
    /** 문구 틀과, 틀에 끼울 다른 문자열 리소스들. */
    data class TextSpec(@StringRes val res: Int, val args: List<Int> = emptyList())

    fun spec(cue: Guidance): TextSpec = when (cue) {
        is Guidance.Pose -> cue.direction
            ?.let { TextSpec(R.string.guide_pose_directed, listOf(partObject(cue.part), movement(it))) }
            ?: TextSpec(posePart(cue.part))
        else -> TextSpec(framing(cue))
    }

    /** 왜 이렇게 하라는지 한 줄. */
    @StringRes
    fun reason(cue: Guidance): Int = when (cue) {
        Guidance.HoldSteady -> R.string.why_hold_steady
        Guidance.NoPerson -> R.string.why_no_person
        is Guidance.Level -> R.string.why_level
        Guidance.MoveCloser, Guidance.MoveBack -> R.string.why_distance
        Guidance.TiltUp, Guidance.TiltDown -> R.string.why_tilt
        Guidance.PanLeft, Guidance.PanRight -> R.string.why_pan
        is Guidance.Pose -> R.string.why_pose
        Guidance.HeadCut -> R.string.why_head_cut
        Guidance.FeetToBottom -> R.string.why_feet
        Guidance.TooMuchHeadroom -> R.string.why_headroom
        is Guidance.CutAtJoint -> R.string.why_cut
        is Guidance.GazeSpace -> R.string.why_gaze
    }

    @Composable
    fun text(cue: Guidance): String {
        val spec = spec(cue)
        val args = spec.args.map { stringResource(it) }.toTypedArray<Any>()
        return stringResource(spec.res, *args)
    }

    fun text(context: Context, cue: Guidance): String {
        val spec = spec(cue)
        return context.getString(spec.res, *spec.args.map { context.getString(it) }.toTypedArray<Any>())
    }

    @StringRes
    private fun framing(cue: Guidance): Int = when (cue) {
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
        is Guidance.Pose -> posePart(cue.part)
    }

    /** "왼팔을" 처럼 조사까지 붙은 부위 (방향 안내용). */
    @StringRes
    private fun partObject(part: BodyPart): Int = when (part) {
        BodyPart.LEFT_UPPER_ARM, BodyPart.LEFT_FOREARM -> R.string.part_left_arm
        BodyPart.RIGHT_UPPER_ARM, BodyPart.RIGHT_FOREARM -> R.string.part_right_arm
        BodyPart.LEFT_THIGH, BodyPart.LEFT_SHIN -> R.string.part_left_leg
        BodyPart.RIGHT_THIGH, BodyPart.RIGHT_SHIN -> R.string.part_right_leg
        BodyPart.LEFT_TORSO, BodyPart.RIGHT_TORSO -> R.string.part_torso
    }

    @StringRes
    private fun movement(direction: Direction): Int = when (direction) {
        Direction.RAISE -> R.string.move_raise
        Direction.LOWER -> R.string.move_lower
        Direction.OUTWARD -> R.string.move_outward
        Direction.INWARD -> R.string.move_inward
        Direction.BEND -> R.string.move_bend
        Direction.STRAIGHTEN -> R.string.move_straighten
        Direction.LEAN_LEFT -> R.string.move_lean_left
        Direction.LEAN_RIGHT -> R.string.move_lean_right
    }

    /** 방향을 모를 때: 부위만 ("여자친구 왼쪽 아래팔을 레퍼런스처럼") */
    @StringRes
    private fun posePart(part: BodyPart): Int = when (part) {
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
