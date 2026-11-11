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
    /** 문구 틀과 틀에 끼울 값들. Int는 문자열 리소스로 바꾸므로, 숫자는 반드시 String으로 넣는다. */
    data class TextSpec(@StringRes val res: Int, val args: List<Any> = emptyList())

    fun spec(cue: Guidance): TextSpec = when (cue) {
        is Guidance.Pose -> cue.direction
            ?.let { TextSpec(R.string.guide_pose_directed, listOf(partObject(cue.part, it), movement(it))) }
            ?: TextSpec(posePart(cue.part))
        is Guidance.TeleZoom -> TextSpec(R.string.guide_tele_zoom, listOf(ZoomLogic.number(cue.ratio)))
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
        is Guidance.TeleZoom -> R.string.why_tele_zoom
    }

    @Composable
    fun text(cue: Guidance): String {
        val spec = spec(cue)
        val args = spec.args.map { if (it is Int) stringResource(it) else it }.toTypedArray()
        return stringResource(spec.res, *args)
    }

    fun text(context: Context, cue: Guidance): String {
        val spec = spec(cue)
        return context.getString(spec.res, *spec.args.map { if (it is Int) context.getString(it) else it }.toTypedArray())
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
        is Guidance.TeleZoom -> R.string.guide_tele_zoom
    }

    /**
     * "왼팔을" 처럼 조사까지 붙은 부위 (방향 안내용).
     * 아래팔·정강이를 옮기라는 말은 손·발로 (팔 전체를 들면 맞던 위팔까지 움직이므로), 굽히고 펴는 건 팔·다리로.
     */
    @StringRes
    private fun partObject(part: BodyPart, direction: Direction): Int {
        val limb = direction == Direction.BEND || direction == Direction.STRAIGHTEN
        return when (part) {
            BodyPart.LEFT_UPPER_ARM -> R.string.part_left_arm
            BodyPart.RIGHT_UPPER_ARM -> R.string.part_right_arm
            BodyPart.LEFT_FOREARM -> if (limb) R.string.part_left_arm else R.string.part_left_hand
            BodyPart.RIGHT_FOREARM -> if (limb) R.string.part_right_arm else R.string.part_right_hand
            BodyPart.LEFT_THIGH -> R.string.part_left_leg
            BodyPart.RIGHT_THIGH -> R.string.part_right_leg
            BodyPart.LEFT_SHIN -> if (limb) R.string.part_left_leg else R.string.part_left_foot
            BodyPart.RIGHT_SHIN -> if (limb) R.string.part_right_leg else R.string.part_right_foot
            BodyPart.LEFT_TORSO, BodyPart.RIGHT_TORSO -> R.string.part_torso
        }
    }

    @StringRes
    private fun movement(direction: Direction): Int = when (direction) {
        Direction.RAISE -> R.string.move_raise
        Direction.LOWER -> R.string.move_lower
        Direction.OUTWARD -> R.string.move_outward
        Direction.INWARD -> R.string.move_inward
        Direction.BEND -> R.string.move_bend
        Direction.STRAIGHTEN -> R.string.move_straighten
        // 화면 왼쪽 = 카메라를 보는 그녀의 오른쪽 (팔·다리 좌우와 같은 기준으로 말한다)
        Direction.LEAN_LEFT -> R.string.move_lean_her_right
        Direction.LEAN_RIGHT -> R.string.move_lean_her_left
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
