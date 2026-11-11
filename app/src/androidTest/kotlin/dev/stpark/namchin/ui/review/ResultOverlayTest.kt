package dev.stpark.namchin.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.stpark.namchin.core.bestshot.ReviewItem
import dev.stpark.namchin.core.color.Filter
import dev.stpark.namchin.review.Look
import dev.stpark.namchin.review.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultOverlayTest {

    @get:Rule
    val rule = createComposeRule()

    private val state = ReviewState.Ready(
        items = listOf(
            ReviewItem(id = "content://shot/1", score = 1f, best = true, eyesClosed = false),
            ReviewItem(id = "content://shot/2", score = 0.5f, best = false, eyesClosed = false),
        ),
        reference = null,
    )

    /** 손가락은 누르는 동안 조금씩 움직인다 (adb 탭과 달리) */
    private fun fingerTap(text: String) {
        rule.onNodeWithText(text).performTouchInput {
            down(center)
            moveBy(Offset(2f, 1f))
            moveBy(Offset(1f, 2f))
            up()
        }
    }

    @Test
    fun keepAllRespondsToAFingerThatMovesSlightly() {
        var closed = 0
        rule.setContent { ResultOverlay(state, onKeep = {}, onClose = { closed++ }) }

        fingerTap("모두 두기")

        rule.runOnIdle { assertEquals(1, closed) }
    }

    @Test
    fun keepBestRespondsToAFingerThatMovesSlightly() {
        var kept: String? = null
        rule.setContent { ResultOverlay(state, onKeep = { kept = it }, onClose = {}) }

        fingerTap("베스트만 남기기")

        rule.runOnIdle { assertEquals("content://shot/1", kept) }
    }

    @Test
    fun touchesOnEmptyAreaDoNotReachTheCameraBelow() {
        var cameraTapped = 0
        rule.setContent {
            Box(Modifier.fillMaxSize().clickable { cameraTapped++ })
            ResultOverlay(state, onKeep = {}, onClose = {})
        }

        fingerTap("베스트 컷")

        rule.runOnIdle { assertEquals(0, cameraTapped) }
    }

    @Test
    fun pinchOnTheOverlayDoesNotZoomTheCameraBelow() {
        var zoomed = false
        rule.setContent {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) zoomed = true } })
            ResultOverlay(state, onKeep = {}, onClose = {})
        }

        rule.onRoot().performTouchInput {
            down(0, center - Offset(50f, 0f))
            down(1, center + Offset(50f, 0f))
            repeat(5) { i ->
                moveTo(0, center - Offset(50f + i * 40f, 0f))
                moveTo(1, center + Offset(50f + i * 40f, 0f))
            }
            up(0)
            up(1)
        }

        rule.runOnIdle { assertEquals(false, zoomed) }
    }

    @Test
    fun filtersAreOfferedWithoutAReferenceAndPreviewWhenPicked() {
        var previewed: Look? = null
        rule.setContent {
            ResultOverlay(state, onKeep = {}, onClose = {}, onPreviewColor = { _, look, _ -> previewed = look })
        }

        rule.onNodeWithText("레퍼런스 색감").assertDoesNotExist()
        fingerTap("필름")

        rule.runOnIdle { assertEquals(Look.Preset(Filter.FILM), previewed) }
    }

    @Test
    fun theCameraFilterIsPickedAlready() {
        var previewed: Look? = null
        rule.setContent {
            ResultOverlay(
                state, onKeep = {}, onClose = {},
                onPreviewColor = { _, look, _ -> previewed = look },
                initialLook = Look.Preset(Filter.MONO),
            )
        }

        rule.runOnIdle { assertEquals(Look.Preset(Filter.MONO), previewed) }
    }

    @Test
    fun aSingleShotOnlyNeedsADoneButton() {
        var closed = 0
        val single = state.copy(items = state.items.take(1))
        rule.setContent { ResultOverlay(single, onKeep = {}, onClose = { closed++ }) }

        rule.onNodeWithText("베스트만 남기기").assertDoesNotExist()
        fingerTap("완료")

        rule.runOnIdle { assertEquals(1, closed) }
    }
}
