package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FallbackExecutorTest {

    @Test
    fun `runs on the delegate while it is alive`() {
        val delegate = Executors.newSingleThreadExecutor { Thread(it, "analysis") }
        val ran = CountDownLatch(1)
        var threadName = ""

        FallbackExecutor(delegate).execute {
            threadName = Thread.currentThread().name
            ran.countDown()
        }

        assertTrue(ran.await(1, TimeUnit.SECONDS))
        assertEquals("analysis", threadName)
        delegate.shutdown()
    }

    @Test
    fun `runs inline after the delegate is shut down instead of crashing`() {
        val delegate = Executors.newSingleThreadExecutor()
        delegate.shutdown()
        var ran = false

        FallbackExecutor(delegate).execute { ran = true }

        assertTrue(ran)
    }
}
