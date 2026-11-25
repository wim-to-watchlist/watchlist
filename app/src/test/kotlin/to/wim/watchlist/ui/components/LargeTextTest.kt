package to.wim.watchlist.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `tabs fit when the widest one times the count fits`() {
        // Widest 60 + 32 padding = 92 per tab, 3 tabs = 276.
        assertTrue(LargeText.tabsFit(listOf(40f, 60f, 50f), 276f))
        assertFalse(LargeText.tabsFit(listOf(40f, 60f, 50f), 275f))
    }

    @Test
    fun `five long shelf tabs on a small phone scroll`() {
        assertFalse(LargeText.tabsFit(listOf(60f, 58f, 90f, 55f, 62f), 360f))
        assertTrue(LargeText.tabsFit(emptyList(), 100f))
    }

    @Test
    fun `scrolling tabs widen so the last one on screen is cut about halfway`() {
        // 360 wide: three 100dp tabs end at 300, so tabs grow to show 3.5 of them.
        assertEquals(360f / 3.5f, LargeText.peekTabWidth(100f, 360f), 0.01f)
        // 344 wide: 3.5 tabs would be narrower than 100, so 2.5 are shown instead.
        assertEquals(344f / 2.5f, LargeText.peekTabWidth(100f, 344f), 0.01f)
    }

    @Test
    fun `a tab wider than two thirds of the row keeps its own width`() {
        assertEquals(344f / 1.5f, LargeText.peekTabWidth(200f, 344f), 0.01f)
        assertEquals(300f, LargeText.peekTabWidth(300f, 344f), 0.01f)
        assertEquals(400f, LargeText.peekTabWidth(400f, 344f), 0.01f)
    }

    @Test
    fun `buttons sit side by side only when every label fits at full size`() {
        // 344 wide, 8 apart: 168 each; a label gets 168 - 74 = 94.
        assertTrue(LargeText.buttonsFit(listOf(80f, 60f), 344f))
        assertTrue(LargeText.buttonsFit(listOf(94f, 60f), 344f))
        assertFalse(LargeText.buttonsFit(listOf(95f, 60f), 344f))
        assertTrue(LargeText.buttonsFit(listOf(500f), 344f))
    }

    @Test
    fun `profile statuses go two per row with large text`() {
        assertEquals(3, LargeText.statusesPerRow(1f))
        assertEquals(3, LargeText.statusesPerRow(1.3f))
        assertEquals(2, LargeText.statusesPerRow(2f))
    }
}
