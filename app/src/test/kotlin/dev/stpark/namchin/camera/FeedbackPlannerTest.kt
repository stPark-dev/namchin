package dev.stpark.namchin.camera

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.live.LiveOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeedbackPlannerTest {

    private fun out(announce: Guidance? = null, aligned: Boolean = false, fire: Boolean = false) =
        LiveOutput(display = announce, cues = listOfNotNull(announce), announce = announce, aligned = aligned, match = null, fire = fire)

    private val all = FeedbackSettings(voice = true, vibrate = true, autoShutter = true)

    @Test
    fun `new cue is spoken with a light tick`() {
        assertEquals(
            listOf(Feedback.Speak(Guidance.MoveBack), Feedback.Tick),
            FeedbackPlanner.plan(wasAligned = false, out(announce = Guidance.MoveBack), all),
        )
    }

    @Test
    fun `voice and vibration can be turned off independently`() {
        assertEquals(
            listOf(Feedback.Tick),
            FeedbackPlanner.plan(false, out(announce = Guidance.MoveBack), all.copy(voice = false)),
        )
        assertEquals(
            listOf(Feedback.Speak(Guidance.MoveBack)),
            FeedbackPlanner.plan(false, out(announce = Guidance.MoveBack), all.copy(vibrate = false)),
        )
    }

    @Test
    fun `becoming aligned buzzes once`() {
        assertEquals(listOf(Feedback.AlignedBuzz), FeedbackPlanner.plan(wasAligned = false, out(aligned = true), all))
        assertTrue(FeedbackPlanner.plan(wasAligned = true, out(aligned = true), all).isEmpty())
    }

    @Test
    fun `auto shutter fire is passed through even without vibration`() {
        val events = FeedbackPlanner.plan(true, out(aligned = true, fire = true), all.copy(vibrate = false))

        assertEquals(listOf(Feedback.Fire), events)
    }

    @Test
    fun `fire is ignored when auto shutter is off`() {
        assertTrue(FeedbackPlanner.plan(true, out(aligned = true, fire = true), all.copy(autoShutter = false)).isEmpty())
    }
}
