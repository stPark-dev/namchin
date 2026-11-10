package dev.stpark.namchin.camera

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.live.LiveOutput

data class FeedbackSettings(val voice: Boolean = true, val vibrate: Boolean = true, val autoShutter: Boolean = true)

/** 한 프레임 판정 결과로 낼 피드백. */
sealed interface Feedback {
    data class Speak(val cue: Guidance) : Feedback
    data object Tick : Feedback
    data object AlignedBuzz : Feedback
    data object Fire : Feedback
}

object FeedbackPlanner {
    fun plan(wasAligned: Boolean, output: LiveOutput, settings: FeedbackSettings): List<Feedback> = buildList {
        output.announce?.let { cue ->
            if (settings.voice) add(Feedback.Speak(cue))
            if (settings.vibrate) add(Feedback.Tick)
        }
        if (output.aligned && !wasAligned && settings.vibrate) add(Feedback.AlignedBuzz)
        if (output.fire && settings.autoShutter) add(Feedback.Fire)
    }
}
