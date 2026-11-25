package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Test
import to.wim.watchlist.data.model.StableOrder

class StableOrderTest {
    private fun order(
        shown: List<String>,
        sorted: List<String>,
    ) = StableOrder.apply(shown, sorted) { it }

    @Test
    fun firstTimeKeepsTheSortedOrder() {
        assertEquals(listOf("a", "b", "c") to listOf("a", "b", "c"), order(emptyList(), listOf("a", "b", "c")))
    }

    @Test
    fun markedShowStaysWhereItWas() {
        // "b" was just watched, so the sort puts it first: it stays second while the screen is open.
        assertEquals(listOf("a", "b", "c"), order(listOf("a", "b", "c"), listOf("b", "a", "c")).first)
    }

    @Test
    fun finishedShowLeavesAndNewShowGoesInAtItsPlace() {
        assertEquals(listOf("a", "c"), order(listOf("a", "b", "c"), listOf("c", "a")).first)
        assertEquals(listOf("d", "a", "b"), order(listOf("a", "b"), listOf("d", "a", "b")).first)
        assertEquals(listOf("a", "b", "d"), order(listOf("a", "b"), listOf("b", "a", "d")).first)
    }
}
