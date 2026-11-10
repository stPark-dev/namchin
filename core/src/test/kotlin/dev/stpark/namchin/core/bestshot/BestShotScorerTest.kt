package dev.stpark.namchin.core.bestshot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BestShotScorerTest {

    private fun shot(id: String, pose: Float? = 0.9f, eyes: Float? = 0.9f, sharp: Float = 100f) =
        ShotCandidate(id, poseScore = pose, eyesOpen = eyes, sharpness = sharp)

    @Test
    fun `empty input gives empty ranking`() {
        assertTrue(BestShotScorer.rank(emptyList()).isEmpty())
    }

    @Test
    fun `ranks by combined score, best first`() {
        val ranked = BestShotScorer.rank(
            listOf(shot("mid", pose = 0.7f), shot("best", pose = 0.95f), shot("low", pose = 0.3f)),
        )

        assertEquals(listOf("best", "mid", "low"), ranked.map { it.candidate.id })
    }

    @Test
    fun `shots with closed eyes are excluded`() {
        val ranked = BestShotScorer.rank(listOf(shot("blink", eyes = 0.1f), shot("ok")))

        assertEquals(listOf("ok"), ranked.map { it.candidate.id })
    }

    @Test
    fun `missing face is kept with neutral eye score`() {
        val ranked = BestShotScorer.rank(listOf(shot("noFace", eyes = null)))

        assertEquals(0.5f * 0.9f + 0.3f * 0.5f + 0.2f * 1f, ranked.single().score, 1e-4f)
    }

    @Test
    fun `missing pose counts as zero`() {
        val ranked = BestShotScorer.rank(listOf(shot("noPose", pose = null)))

        assertEquals(0.3f * 0.9f + 0.2f * 1f, ranked.single().score, 1e-4f)
    }

    @Test
    fun `sharpness is relative to the sharpest shot in the batch`() {
        val ranked = BestShotScorer.rank(listOf(shot("sharp", sharp = 200f), shot("blurry", sharp = 50f)))

        val blurry = ranked.first { it.candidate.id == "blurry" }
        assertEquals(0.5f * 0.9f + 0.3f * 0.9f + 0.2f * 0.25f, blurry.score, 1e-4f)
    }

    @Test
    fun `all zero sharpness does not divide by zero`() {
        val ranked = BestShotScorer.rank(listOf(shot("a", sharp = 0f)))

        assertEquals(0.5f * 0.9f + 0.3f * 0.9f, ranked.single().score, 1e-4f)
    }

    @Test
    fun `if every shot has closed eyes they are all ranked anyway`() {
        val ranked = BestShotScorer.rank(listOf(shot("a", eyes = 0.1f, pose = 0.5f), shot("b", eyes = 0.1f)))

        assertEquals(listOf("b", "a"), ranked.map { it.candidate.id })
    }
}
