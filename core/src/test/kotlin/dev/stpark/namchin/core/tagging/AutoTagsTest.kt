package dev.stpark.namchin.core.tagging

import dev.stpark.namchin.core.crop.LumaImage
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.with
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoTagsTest {

    private val full = TagVocabulary.FULL_BODY
    private val upper = TagVocabulary.UPPER_BODY
    private val seated = TagVocabulary.SEATED

    @Test
    fun `standing with visible ankles is full body`() {
        assertEquals(setOf(full), AutoTags.framing(pose(STANDING)))
    }

    @Test
    fun `no knees is upper body`() {
        val upperBody = STANDING.filterKeys { "KNEE" !in it.name && "ANKLE" !in it.name }

        assertEquals(setOf(upper), AutoTags.framing(pose(upperBody)))
    }

    @Test
    fun `thighs close to horizontal mean seated`() {
        val sitting = STANDING.with(
            Joint.LEFT_KNEE to Point(0.68f, 0.60f),
            Joint.RIGHT_KNEE to Point(0.62f, 0.60f),
            Joint.LEFT_ANKLE to Point(0.68f, 0.78f),
            Joint.RIGHT_ANKLE to Point(0.62f, 0.78f),
        )

        assertEquals(setOf(full, seated), AutoTags.framing(pose(sitting)))
    }

    @Test
    fun `no person means no framing tags`() {
        assertTrue(AutoTags.framing(null).isEmpty())
    }

    @Test
    fun `dark images are night, others day`() {
        assertEquals(TagVocabulary.NIGHT, AutoTags.time(LumaImage(2, 2, IntArray(4) { 30 })))
        assertEquals(TagVocabulary.DAY, AutoTags.time(LumaImage(2, 2, IntArray(4) { 150 })))
    }

    @Test
    fun `place comes from known labels, case-insensitively`() {
        assertEquals(
            setOf(Tag("카페"), Tag("거리"), Tag("자연")),
            AutoTags.place(listOf("Desk", "road", "Sky", "Jeans", "Hair")),
        )
    }

    @Test
    fun `every tag belongs to exactly one category`() {
        val all = TagVocabulary.CATEGORIES.flatMap { it.tags }

        assertEquals(all.size, all.toSet().size)
        assertTrue(TagVocabulary.LABEL_TO_TAG.values.all { it in all })
    }

    @Test
    fun `knees guessed outside the frame do not count`() {
        // 허리 아래가 잘린 사진: ML Kit가 화면 밖 무릎을 추정해도 상반신으로 본다
        val cut = STANDING.filterKeys { "ANKLE" !in it.name }.with(
            Joint.LEFT_KNEE to Point(0.53f, 1.2f),
            Joint.RIGHT_KNEE to Point(0.47f, 1.2f),
        )

        assertEquals(setOf(upper), AutoTags.framing(pose(cut)))
    }

    @Test
    fun `a dim indoor scene is not night`() {
        assertEquals(TagVocabulary.DAY, AutoTags.time(LumaImage(2, 2, IntArray(4) { 60 })))
    }
}
