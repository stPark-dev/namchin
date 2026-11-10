package dev.stpark.namchin.core.bestshot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShotReviewTest {

    private fun shot(id: String, pose: Float? = 0.9f, eyes: Float? = 0.9f, sharp: Float = 100f) =
        ShotCandidate(id, poseScore = pose, eyesOpen = eyes, sharpness = sharp)

    @Test
    fun `keeps shooting order and marks exactly one best`() {
        val items = ShotReview.of(listOf(shot("1", pose = 0.6f), shot("2", pose = 0.95f), shot("3", pose = 0.7f)))

        assertEquals(listOf("1", "2", "3"), items.map { it.id })
        assertEquals(listOf(false, true, false), items.map { it.best })
    }

    @Test
    fun `closed eyes are flagged and left unscored`() {
        val items = ShotReview.of(listOf(shot("1", eyes = 0.1f), shot("2")))

        assertTrue(items[0].eyesClosed)
        assertNull(items[0].score)
        assertTrue(items[1].best)
    }

    @Test
    fun `when everyone blinked there is still a best shot`() {
        val items = ShotReview.of(listOf(shot("1", eyes = 0.1f, pose = 0.5f), shot("2", eyes = 0.1f)))

        assertTrue(items.all { it.eyesClosed })
        assertEquals(listOf(false, true), items.map { it.best })
    }

    @Test
    fun `no shots gives no items`() {
        assertTrue(ShotReview.of(emptyList()).isEmpty())
    }
}
