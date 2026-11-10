package dev.stpark.namchin.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ScreenTest {

    @Test
    fun `every screen survives a save and restore`() {
        listOf(
            Screen.Camera(referenceId = null),
            Screen.Library,
            Screen.Import(uri = "content://media/picker/0/x/1", token = "abc"),
            Screen.Detail(id = 42),
            Screen.Camera(referenceId = 7),
        ).forEach { assertEquals(it, Screen.restore(Screen.save(it))) }
    }

    @Test
    fun `unknown saved data restores nothing`() {
        assertNull(Screen.restore(listOf("Nope")))
        assertNull(Screen.restore(listOf("Detail", "not-a-number")))
        assertNull(Screen.restore(emptyList()))
        assertNull(Screen.restore(listOf("Camera")))
        assertNull(Screen.restore(listOf("Camera", "x")))
    }

    @Test
    fun `each new import gets a distinct token`() {
        val a = Screen.Import.new("content://x")
        val b = Screen.Import.new("content://x")

        assertEquals(a.uri, b.uri)
        assert(a.token != b.token)
    }
}
