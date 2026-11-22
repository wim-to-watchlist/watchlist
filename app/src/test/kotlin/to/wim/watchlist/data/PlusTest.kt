package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import to.wim.watchlist.data.model.Access
import to.wim.watchlist.data.model.Plus

class PlusTest {
    @Test
    fun `subscribers and the trial get Plus`() {
        assertTrue(Plus.unlocked(Access.Allowed))
    }

    @Test
    fun `still asking Google Play counts as Plus, so nothing flashes locked`() {
        assertTrue(Plus.unlocked(Access.Checking))
    }

    @Test
    fun `without a subscription Plus is locked`() {
        assertFalse(Plus.unlocked(Access.Locked))
    }

    @Test
    fun `the debug preview locks Plus even for a subscriber`() {
        assertFalse(Plus.unlocked(Access.Allowed, previewLocked = true))
        assertFalse(Plus.unlocked(Access.Checking, previewLocked = true))
    }

    @Test
    fun `locked Up next shows the first show and counts the rest`() {
        assertEquals(listOf("a") to 2, Plus.preview(listOf("a", "b", "c")))
    }

    @Test
    fun `locked Up next with one show or none has nothing more to count`() {
        assertEquals(listOf("a") to 0, Plus.preview(listOf("a")))
        assertEquals(emptyList<String>() to 0, Plus.preview(emptyList<String>()))
    }

    @Test
    fun `without Plus the bottom bar has its default tabs and order`() {
        val mine = NavConfig(NavTab.defaultOrder.reversed(), setOf(NavTab.Discover))
        assertEquals(NavConfig(NavTab.defaultOrder, NavTab.defaultEnabled), Plus.nav(plus = false, stored = mine))
        assertEquals(mine, Plus.nav(plus = true, stored = mine))
    }

    @Test
    fun `the TMDB token is only used with Plus`() {
        assertEquals("eyJtoken", Plus.tmdbKey(plus = true, stored = "eyJtoken"))
        assertEquals(null, Plus.tmdbKey(plus = false, stored = "eyJtoken"))
    }
}
