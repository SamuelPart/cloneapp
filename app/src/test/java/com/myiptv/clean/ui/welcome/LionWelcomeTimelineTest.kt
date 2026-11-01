package com.myiptv.clean.ui.welcome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LionWelcomeTimelineTest {
    @Test fun twelveExpressionsEachLastExactlyOneSecond() {
        assertEquals(
            listOf(
                LionExpression.HALF_CLOSED, LionExpression.WORRIED, LionExpression.WINK,
                LionExpression.DETERMINED, LionExpression.SKEPTICAL, LionExpression.NEUTRAL,
                LionExpression.HAPPY, LionExpression.SLEEPY, LionExpression.CURIOUS,
                LionExpression.DIZZY, LionExpression.CONNECTED, LionExpression.SQUEEZED,
            ),
            LionWelcomeTimeline.expressions,
        )
        assertEquals(12_000L, LionWelcomeTimeline.durationMs)
        assertEquals(LionExpression.HALF_CLOSED, LionWelcomeTimeline.expressionAt(-1L))

        LionWelcomeTimeline.expressions.forEachIndexed { index, expression ->
            val start = index * 1_000L
            assertEquals(expression, LionWelcomeTimeline.expressionAt(start))
            assertEquals(expression, LionWelcomeTimeline.expressionAt(start + 999L))
            assertFalse(LionWelcomeTimeline.isFinished(start + 999L))
        }
        assertEquals(LionExpression.SQUEEZED, LionWelcomeTimeline.expressionAt(12_000L))
        assertTrue(LionWelcomeTimeline.isFinished(12_000L))
        assertTrue(LionWelcomeTimeline.isFinished(Long.MAX_VALUE))
    }

    @Test fun eachChangeClosesThenReopensTheEyesWithoutAddingTime() {
        val epsilon = 0.0001f
        assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(-1L), epsilon)
        assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(0L), epsilon)
        for (index in 1 until LionWelcomeTimeline.expressions.size) {
            val boundary = index * LionWelcomeTimeline.FRAME_DURATION_MS
            val before = LionWelcomeTimeline.eyeScaleAt(boundary - 40L)
            val almostClosed = LionWelcomeTimeline.eyeScaleAt(boundary - 20L)
            val closed = LionWelcomeTimeline.eyeScaleAt(boundary)
            val reopening = LionWelcomeTimeline.eyeScaleAt(boundary + 50L)

            assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(boundary - 80L), epsilon)
            assertTrue("Los ojos se cierran progresivamente", before in 0.08f..1f && almostClosed < before)
            assertEquals(LionWelcomeTimeline.MIN_EYE_SCALE, closed, epsilon)
            assertTrue("Los ojos se vuelven a abrir", reopening > closed && reopening < 1f)
            assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(boundary + 100L), epsilon)
            assertEquals(LionWelcomeTimeline.expressions[index - 1], LionWelcomeTimeline.expressionAt(boundary - 1L))
            assertEquals(LionWelcomeTimeline.expressions[index], LionWelcomeTimeline.expressionAt(boundary))
        }
        assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(11_999L), epsilon)
        assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(12_000L), epsilon)
        assertEquals(12_000L, LionWelcomeTimeline.durationMs)
    }

    @Test fun renderingSleepsBetweenBlinksAndStopsAtTheOriginalDeadline() {
        assertEquals(920L, LionWelcomeTimeline.millisUntilVisualUpdate(-1L))
        assertEquals(920L, LionWelcomeTimeline.millisUntilVisualUpdate(0L))
        assertEquals(1L, LionWelcomeTimeline.millisUntilVisualUpdate(919L))
        assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(920L))
        assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(999L))
        assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(1_000L))
        assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(1_099L))
        assertEquals(820L, LionWelcomeTimeline.millisUntilVisualUpdate(1_100L))
        assertEquals(900L, LionWelcomeTimeline.millisUntilVisualUpdate(11_100L))
        assertEquals(1L, LionWelcomeTimeline.millisUntilVisualUpdate(11_999L))
        assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(12_000L))
        for (index in 1 until LionWelcomeTimeline.expressions.size) {
            val boundary = index * LionWelcomeTimeline.FRAME_DURATION_MS
            assertEquals(1L, LionWelcomeTimeline.millisUntilVisualUpdate(boundary - 81L))
            assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(boundary - 80L))
            assertEquals(0L, LionWelcomeTimeline.millisUntilVisualUpdate(boundary + 99L))
            assertTrue(LionWelcomeTimeline.millisUntilVisualUpdate(boundary + 100L) > 0L)
        }
    }

    @Test fun pausingDuringABlinkResumesTheSameEyePose() {
        val clock = LionVisibleClock()
        clock.resume(1_000L)
        clock.pause(1_970L)
        val pausedElapsed = clock.elapsedAt(20_000L)
        assertEquals(970L, pausedElapsed)
        val pausedScale = LionWelcomeTimeline.eyeScaleAt(pausedElapsed)
        assertEquals(pausedScale, LionWelcomeTimeline.eyeScaleAt(clock.elapsedAt(30_000L)), 0.0001f)
        clock.resume(30_000L)
        assertEquals(LionWelcomeTimeline.MIN_EYE_SCALE, LionWelcomeTimeline.eyeScaleAt(clock.elapsedAt(30_030L)), 0.0001f)
        assertEquals(LionExpression.WORRIED, LionWelcomeTimeline.expressionAt(clock.elapsedAt(30_030L)))
        assertEquals(1f, LionWelcomeTimeline.eyeScaleAt(clock.elapsedAt(30_130L)), 0.0001f)
    }

    @Test fun timerTapAndRemoteConfirmCanOpenContentOnlyOnce() {
        val gate = WelcomeNavigationGate()
        assertTrue(gate.tryOpen())
        assertFalse(gate.tryOpen())
        assertFalse(gate.tryOpen())
    }

    @Test fun backgroundTimeDoesNotAdvanceTheAnimationOrStartContent() {
        val clock = LionVisibleClock()
        clock.resume(1_000L)
        assertEquals(400L, clock.elapsedAt(1_400L))
        clock.pause(1_400L)
        assertEquals(400L, clock.elapsedAt(60_000L))
        clock.pause(61_000L) // An extra pause must not change the saved time.
        assertEquals(400L, clock.elapsedAt(61_000L))

        clock.resume(62_000L)
        clock.resume(62_100L) // A second resume must not restart the current interval.
        assertEquals(1_000L, clock.elapsedAt(62_600L))
        assertEquals(LionExpression.WORRIED, LionWelcomeTimeline.expressionAt(clock.elapsedAt(62_600L)))
        clock.pause(62_600L)
        assertFalse(LionWelcomeTimeline.isFinished(clock.elapsedAt(100_000L)))
    }

    @Test fun restoredClockContinuesFromVisibleElapsedTimeAndClampsAtTheEnd() {
        val first = LionVisibleClock()
        first.resume(10_000L)
        first.pause(12_450L)
        val restored = LionVisibleClock(first.elapsedAt(20_000L))
        assertEquals(2_450L, restored.elapsedAt(20_000L))
        assertEquals(LionExpression.WINK, LionWelcomeTimeline.expressionAt(restored.elapsedAt(20_000L)))
        restored.resume(30_000L)
        assertEquals(11_999L, restored.elapsedAt(39_549L))
        assertFalse(LionWelcomeTimeline.isFinished(restored.elapsedAt(39_549L)))
        assertEquals(12_000L, restored.elapsedAt(39_550L))
        assertTrue(LionWelcomeTimeline.isFinished(restored.elapsedAt(99_999L)))
        assertEquals(12_000L, restored.elapsedAt(99_999L))
    }

    @Test fun twelveDistinctEyeVectorsExistAndShareTheSameCoordinateSpace() {
        val mainDirectory = listOf(File("src/main"), File("app/src/main"))
            .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val drawings = mutableSetOf<String>()
        for (index in 1..12) {
            val file = File(mainDirectory, "res/drawable/samplay_lion_eyes_${index.toString().padStart(2, '0')}.xml")
            assertTrue("Falta la expresión $index", file.isFile)
            val vector = factory.newDocumentBuilder().parse(file).documentElement
            assertEquals("vector", vector.tagName)
            assertEquals("512", vector.getAttributeNS(androidNamespace, "viewportWidth"))
            assertEquals("512", vector.getAttributeNS(androidNamespace, "viewportHeight"))
            val paths = vector.getElementsByTagName("path")
            assertTrue("La expresión $index debe tener ojos", paths.length >= 2)
            val shape = (0 until paths.length).joinToString("|") { pathIndex ->
                val path = paths.item(pathIndex) as Element
                val fill = path.getAttributeNS(androidNamespace, "fillColor")
                val stroke = path.getAttributeNS(androidNamespace, "strokeColor")
                assertTrue("Los ojos deben ser negros", fill == "#FF0A0A0A" || stroke == "#FF0A0A0A")
                path.getAttributeNS(androidNamespace, "pathData")
            }
            assertTrue("La expresión $index está duplicada", drawings.add(shape))
        }
        assertEquals(12, drawings.size)
    }
}
