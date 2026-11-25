package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import to.wim.watchlist.data.model.SimklPlan

class SimklPlanTest {
    @Test
    fun `free in any case is the free plan`() {
        assertEquals(SimklPlan.Free, SimklPlan.of("free"))
        assertEquals(SimklPlan.Free, SimklPlan.of(" Free "))
    }

    @Test
    fun `paid plans keep their name in capitals`() {
        assertEquals(SimklPlan.Paid("PRO"), SimklPlan.of("pro"))
        assertEquals(SimklPlan.Paid("VIP"), SimklPlan.of("vip"))
    }

    @Test
    fun `no account type shows nothing`() {
        assertNull(SimklPlan.of(null))
        assertNull(SimklPlan.of("  "))
    }
}
