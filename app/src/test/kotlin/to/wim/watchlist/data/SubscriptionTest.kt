package to.wim.watchlist.data

import to.wim.watchlist.data.model.Access
import to.wim.watchlist.data.model.PaywallPlan
import to.wim.watchlist.data.model.PlanPeriod
import to.wim.watchlist.data.model.PlayStatus
import to.wim.watchlist.data.model.PricePhase
import to.wim.watchlist.data.model.PurchaseInfo
import to.wim.watchlist.data.model.StoredAccess
import to.wim.watchlist.data.model.SubscriptionOffer
import to.wim.watchlist.data.model.SubscriptionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import java.util.concurrent.TimeUnit

class SubscriptionTest {
    private val id = SubscriptionRules.PRODUCT_ID
    private val now = 1_800_000_000_000L
    private val day = TimeUnit.DAYS.toMillis(1)

    @Test
    fun purchasesOfThisSubscriptionDecideTheStatus() {
        assertEquals(PlayStatus.Inactive, SubscriptionRules.status(emptyList()))
        assertEquals(PlayStatus.Active, SubscriptionRules.status(listOf(PurchaseInfo(listOf(id), purchased = true, pending = false))))
        assertEquals(PlayStatus.Pending, SubscriptionRules.status(listOf(PurchaseInfo(listOf(id), purchased = false, pending = true))))
        // Another product doesn't unlock the app.
        assertEquals(PlayStatus.Inactive, SubscriptionRules.status(listOf(PurchaseInfo(listOf("other"), purchased = true, pending = false))))
    }

    @Test
    fun withoutAnActiveSubscriptionTheAppIsLocked() {
        assertEquals(Access.Allowed, SubscriptionRules.access(true, PlayStatus.Active, null, now))
        assertEquals(Access.Locked, SubscriptionRules.access(true, PlayStatus.Inactive, null, now))
        assertEquals(Access.Locked, SubscriptionRules.access(true, PlayStatus.Pending, null, now))
        // A remembered subscription doesn't outweigh Google Play saying it ended.
        assertEquals(Access.Locked, SubscriptionRules.access(true, PlayStatus.Inactive, StoredAccess(true, now), now))
    }

    @Test
    fun aRecentlyActiveSubscriptionOpensTheAppWhilePlayCantBeAsked() {
        val recent = StoredAccess(active = true, checkedAt = now - 2 * day)
        val old = StoredAccess(active = true, checkedAt = now - 8 * day)
        assertEquals(Access.Allowed, SubscriptionRules.access(true, PlayStatus.Unknown, recent, now))
        assertEquals(Access.Allowed, SubscriptionRules.access(true, PlayStatus.Unavailable, recent, now))
        assertEquals(Access.Locked, SubscriptionRules.access(true, PlayStatus.Unavailable, old, now))
        assertEquals(Access.Checking, SubscriptionRules.access(true, PlayStatus.Unknown, null, now))
        // Google Play said no last time: locked until it answers again, so nothing of Plus flashes open.
        assertEquals(Access.Locked, SubscriptionRules.access(true, PlayStatus.Unknown, StoredAccess(false, now), now))
        // A clock set back doesn't extend it.
        assertEquals(Access.Locked, SubscriptionRules.access(true, PlayStatus.Unavailable, StoredAccess(true, now + day), now))
    }

    @Test
    fun debugBuildsAreNeverLocked() {
        assertEquals(Access.Allowed, SubscriptionRules.access(false, PlayStatus.Inactive, null, now))
    }

    private val yearly = PricePhase(4_990_000, "€4.99", "P1Y", "EUR")
    private val monthly = PricePhase(990_000, "€0.99", "P1M", "EUR")
    private val free = PricePhase(0, "Free", "P2W", "EUR")
    private val y = SubscriptionRules.YEARLY_PLAN
    private val m = SubscriptionRules.MONTHLY_PLAN

    @Test
    fun theYearlyPlanComesFirstWithItsFreeTrial() {
        val offers =
            listOf(
                SubscriptionOffer("month", m, listOf(monthly)),
                SubscriptionOffer("year", y, listOf(yearly)),
                SubscriptionOffer("trial", y, listOf(free, yearly)),
            )
        assertEquals(
            listOf(
                PaywallPlan("trial", PlanPeriod.Yearly, 14, "€4.99", 4_990_000, "EUR"),
                PaywallPlan("month", PlanPeriod.Monthly, null, "€0.99", 990_000, "EUR"),
            ),
            SubscriptionRules.paywallPlans(offers),
        )
    }

    @Test
    fun withoutTheTrialThePlainPlansAreOffered() {
        // Someone who already had the trial only gets the base plans from Google Play.
        val plans = SubscriptionRules.paywallPlans(listOf(SubscriptionOffer("year", y, listOf(yearly)), SubscriptionOffer("month", m, listOf(monthly))))
        assertEquals(listOf("year", "month"), plans.map { it.token })
        assertEquals(listOf(null, null), plans.map { it.trialDays })
        assertEquals(emptyList<PaywallPlan>(), SubscriptionRules.paywallPlans(emptyList()))
        // Unknown base plans and offers without a paid phase are left out.
        assertEquals(emptyList<PaywallPlan>(), SubscriptionRules.paywallPlans(listOf(SubscriptionOffer("x", "weekly", listOf(yearly)), SubscriptionOffer("f", y, listOf(free)))))
    }

    private fun plan(
        period: PlanPeriod,
        micros: Long,
        currency: String = "EUR",
    ) = PaywallPlan("t", period, null, "", micros, currency)

    @Test
    fun theYearlyPlanSavesComparedWithTwelveMonths() {
        assertEquals(58, SubscriptionRules.savingsPercent(plan(PlanPeriod.Yearly, 4_990_000), plan(PlanPeriod.Monthly, 990_000)))
        // Not cheaper, or in another currency: no saving shown.
        assertNull(SubscriptionRules.savingsPercent(plan(PlanPeriod.Yearly, 12_000_000), plan(PlanPeriod.Monthly, 990_000)))
        assertNull(SubscriptionRules.savingsPercent(plan(PlanPeriod.Yearly, 4_990_000), plan(PlanPeriod.Monthly, 990_000, "USD")))
    }

    @Test
    fun theYearlyPriceIsShownPerMonth() {
        assertEquals("€0.42", SubscriptionRules.perMonth(plan(PlanPeriod.Yearly, 4_990_000), Locale.UK))
        assertEquals("$0.42", SubscriptionRules.perMonth(plan(PlanPeriod.Yearly, 4_990_000, "USD"), Locale.US))
        assertNull(SubscriptionRules.perMonth(plan(PlanPeriod.Yearly, 4_990_000, ""), Locale.US))
    }

    @Test
    fun theReminderComesTwoDaysBeforeTheTrialEnds() {
        val end = SubscriptionRules.trialEnd(now, 14)
        assertEquals(now + 14 * day, end)
        assertEquals(now + 12 * day, SubscriptionRules.reminderAt(end, now))
        // Bought late in the trial window (or the reminder is late): remind right away.
        assertEquals(now, SubscriptionRules.reminderAt(now + day, now))
        // The trial already ended: no reminder.
        assertNull(SubscriptionRules.reminderAt(now - day, now))
    }

    @Test
    fun trialPeriodsAreReadInDays() {
        assertEquals(14, SubscriptionRules.days("P14D"))
        assertEquals(14, SubscriptionRules.days("P2W"))
        assertEquals(30, SubscriptionRules.days("P1M"))
        assertNull(SubscriptionRules.days("two weeks"))
    }
}
