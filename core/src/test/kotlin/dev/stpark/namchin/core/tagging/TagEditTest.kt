package dev.stpark.namchin.core.tagging

import dev.stpark.namchin.core.tagging.TagEdit.ChipState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TagEditTest {

    private val cafe = Tag("카페")
    private val autumn = Tag("가을")
    private val full = Tag("전신")

    @Test
    fun `chips show own tags as on and learned ones as suggested`() {
        val states = TagEdit.states(own = setOf(cafe), learned = setOf(autumn))

        assertEquals(ChipState.ON, states[cafe])
        assertEquals(ChipState.SUGGESTED, states[autumn])
        assertEquals(ChipState.OFF, states[full])
    }

    @Test
    fun `toggling a chip confirms only the reviewed tags, not learned suggestions`() {
        assertEquals(setOf(cafe, full), TagEdit.toggle(own = setOf(cafe), tag = full))
        assertEquals(emptySet<Tag>(), TagEdit.toggle(own = setOf(cafe), tag = cafe))
    }

    @Test
    fun `tapping a suggested chip accepts just that suggestion`() {
        assertEquals(setOf(cafe, autumn), TagEdit.toggle(own = setOf(cafe), tag = autumn))
    }

    @Test
    fun `confirm as is takes own tags plus suggestions`() {
        assertEquals(setOf(cafe, autumn), TagEdit.confirmAll(own = setOf(cafe), learned = setOf(autumn)))
    }
}
