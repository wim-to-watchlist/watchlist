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
}
