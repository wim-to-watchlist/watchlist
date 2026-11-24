package to.wim.watchlist.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LargeTextTest {
    @Test
    fun `posters keep their width at normal or smaller text`() {
        assertEquals(1f, LargeText.posterWidthFactor(1f))
        assertEquals(1f, LargeText.posterWidthFactor(0.85f))
    }

    @Test
    fun `posters grow with larger text`() {
        assertEquals(1.3f, LargeText.posterWidthFactor(1.3f), 0.0001f)
    }

    @Test
    fun `posters grow at most half as wide again`() {
        assertEquals(1.5f, LargeText.posterWidthFactor(2f))
    }

    @Test
    fun `three rating tiles share a Pixel 7 line at normal text`() {
        assertEquals(3, LargeText.tilesPerRow(356f, 1f))
    }

    @Test
    fun `fewer rating tiles per line with larger text, never zero`() {
        assertEquals(2, LargeText.tilesPerRow(356f, 2f))
        assertEquals(1, LargeText.tilesPerRow(100f, 2f))
    }

    @Test
    fun `fitSize keeps the full size when it fits`() {
        assertEquals(12f, LargeText.fitSize(12f) { true })
    }

    @Test
    fun `fitSize steps down to the largest size that fits`() {
        assertEquals(10.5f, LargeText.fitSize(12f) { it <= 10.5f })
    }

    @Test
    fun `fitSize stops at the minimum`() {
        assertEquals(8f, LargeText.fitSize(12f) { false })
    }

    @Test
    fun `card lines grow from one to two with larger text`() {
        assertEquals(1, LargeText.cardLines(1f))
        assertEquals(2, LargeText.cardLines(1.15f))
        assertEquals(2, LargeText.cardLines(2f))
    }
}
