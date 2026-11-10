package dev.stpark.namchin.core.tagging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TagCodecTest {

    @Test
    fun `round trips a tag set in a stable order`() {
        val tags = setOf(Tag("카페"), Tag("전신"), Tag("가을"))

        assertEquals("가을,전신,카페", TagCodec.encode(tags))
        assertEquals(tags, TagCodec.decode(TagCodec.encode(tags)))
    }

    @Test
    fun `empty and messy text decode safely`() {
        assertEquals(emptySet<Tag>(), TagCodec.decode(""))
        assertEquals(setOf(Tag("카페"), Tag("야경")), TagCodec.decode(" 카페 ,, 야경 ,"))
    }
}
