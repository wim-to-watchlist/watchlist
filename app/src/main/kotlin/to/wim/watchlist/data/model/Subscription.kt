package to.wim.watchlist.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Period
import java.time.format.DateTimeParseException
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit

/** What Google Play last said about the subscription. */
enum class PlayStatus {
    /** Not asked yet (the app just started). */
    Unknown,

    /** A purchased (or in its free trial) subscription. */
    Active,

    /** Bought, but Google Play hasn't confirmed the payment yet (some payment methods take days). */
    Pending,

    /** No subscription. */
    Inactive,

    /** Google Play couldn't be asked: not installed, signed out, blocked, offline. */
    Unavailable,
}

/** Whether the app opens, or shows the subscription screen. */
enum class Access { Checking, Allowed, Locked }

/** The last answer Google Play gave, kept on the phone so the app opens offline. */
data class StoredAccess(
    val active: Boolean,
    /** Epoch ms. */
    val checkedAt: Long,
)

/** A purchase as far as the app cares: which products, and whether it is paid. */
data class PurchaseInfo(
    val products: List<String>,
    val purchased: Boolean,
    val pending: Boolean,
)

/** One price phase of a subscription offer, as Google Play describes it. */
data class PricePhase(
    val priceMicros: Long,
    /** Already in the user's currency and format ("€4.99", "4,99 €"). */
    val formattedPrice: String,
    /** ISO 8601 period: "P1Y", "P1M", "P2W", "P14D". */
    val billingPeriod: String,
    /** ISO 4217: "EUR". */
    val currencyCode: String = "",
)

/** One way to buy the subscription: a base plan (yearly, monthly) or an offer on it (the free trial). */
data class SubscriptionOffer(
    val token: String,
    val basePlanId: String,
    val phases: List<PricePhase>,
)

enum class PlanPeriod { Yearly, Monthly }

/** A plan the subscription screen offers: a free trial of [trialDays] (null: none), then [price] per [period]. */
data class PaywallPlan(
    val token: String,
    val period: PlanPeriod,
    val trialDays: Int?,
    val price: String,
    val priceMicros: Long,
    val currencyCode: String,
)

/**
 * The rules of the subscription: a yearly plan with a free trial and a monthly plan, both sold by
 * Google Play (Play decides who still gets the trial). Without an active subscription the app is
 * locked. Plain Kotlin so it is unit tested; `SubscriptionRepository` talks to Google Play.
 */
object SubscriptionRules {
    /** The subscription's product ID in the Play Console (see docs/publishing.md). */
    const val PRODUCT_ID = "watchlist"

    /** Base plan IDs of [PRODUCT_ID] in the Play Console. */
    const val YEARLY_PLAN = "yearly"
    const val MONTHLY_PLAN = "monthly"

    /** How long a remembered "active" opens the app while Google Play can't be asked. */
    val OFFLINE_GRACE = TimeUnit.DAYS.toMillis(7)

    /** How long before the free trial ends the app reminds the user. */
    const val REMINDER_DAYS_BEFORE = 2

    private val DAY = TimeUnit.DAYS.toMillis(1)

    fun status(purchases: List<PurchaseInfo>): PlayStatus {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        return when {
            mine.any { it.purchased } -> PlayStatus.Active
            mine.any { it.pending } -> PlayStatus.Pending
            else -> PlayStatus.Inactive
        }
    }

    /**
     * [required] is false in debug builds, which are installed next to the Play app and can't buy
     * anything. A remembered "active" younger than [OFFLINE_GRACE] stands in while Google Play
     * can't be asked, so a subscriber isn't locked out on a plane.
     */
    fun access(
        required: Boolean,
        status: PlayStatus,
        stored: StoredAccess?,
        now: Long,
    ): Access {
        if (!required) return Access.Allowed
        val recentlyActive = stored != null && stored.active && now - stored.checkedAt in 0 until OFFLINE_GRACE
        return when (status) {
            PlayStatus.Active -> Access.Allowed
            PlayStatus.Pending, PlayStatus.Inactive -> Access.Locked
            PlayStatus.Unknown -> if (recentlyActive) Access.Allowed else Access.Checking
            PlayStatus.Unavailable -> if (recentlyActive) Access.Allowed else Access.Locked
        }
    }

    /**
     * The plans the subscription screen shows, yearly first. For each base plan an offer that starts
     * with a free phase wins (Google Play only returns the trial offer to people who haven't had
     * it), else the base plan itself.
     */
    fun paywallPlans(offers: List<SubscriptionOffer>): List<PaywallPlan> =
        listOf(YEARLY_PLAN to PlanPeriod.Yearly, MONTHLY_PLAN to PlanPeriod.Monthly).mapNotNull { (id, period) ->
            val mine = offers.filter { it.basePlanId == id && it.paid() != null }
            val chosen = mine.firstOrNull { it.phases.first().priceMicros == 0L } ?: mine.firstOrNull() ?: return@mapNotNull null
            val paid = chosen.paid()!!
            val trial = chosen.phases.first().takeIf { it.priceMicros == 0L }?.let { days(it.billingPeriod) }
            PaywallPlan(chosen.token, period, trial, paid.formattedPrice, paid.priceMicros, paid.currencyCode)
        }

    private fun SubscriptionOffer.paid() = phases.lastOrNull { it.priceMicros > 0 }

    /** How much cheaper the yearly plan is than twelve months: 58 for €4.99 against €0.99. Null when it isn't. */
    fun savingsPercent(
        yearly: PaywallPlan,
        monthly: PaywallPlan,
    ): Int? {
        if (yearly.currencyCode != monthly.currencyCode || monthly.priceMicros <= 0) return null
        val percent = Math.round((1 - yearly.priceMicros.toDouble() / (monthly.priceMicros * 12)) * 100).toInt()
        return percent.takeIf { it > 0 }
    }

    /** The yearly price per month in the user's currency ("€0.42"); null when the currency is unknown. */
    fun perMonth(
        yearly: PaywallPlan,
        locale: Locale,
    ): String? {
        val currency = runCatching { Currency.getInstance(yearly.currencyCode) }.getOrNull() ?: return null
        val format = NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }
        val perMonth = BigDecimal(yearly.priceMicros).divide(BigDecimal(12_000_000), 2, RoundingMode.HALF_UP)
        return format.format(perMonth)
    }

    /** When a free trial of [trialDays] bought at [purchasedAt] ends (epoch ms). */
    fun trialEnd(
        purchasedAt: Long,
        trialDays: Int,
    ): Long = purchasedAt + trialDays * DAY

    /** When to remind the user that the trial ending at [trialEnd] is about to end; null when it already ended. */
    fun reminderAt(
        trialEnd: Long,
        now: Long,
    ): Long? {
        if (now >= trialEnd) return null
        return maxOf(trialEnd - REMINDER_DAYS_BEFORE * DAY, now)
    }

    /** What the subscription screen shows in the preview of debug builds, which can't ask Google Play. */
    val SAMPLE_PLANS =
        listOf(
            PaywallPlan("preview", PlanPeriod.Yearly, 14, "€4.99", 4_990_000, "EUR"),
            PaywallPlan("preview", PlanPeriod.Monthly, null, "€0.99", 990_000, "EUR"),
        )

    /** "P14D" → 14, "P2W" → 14, "P1M" → 30; null when it can't be read. */
    fun days(period: String): Int? =
        try {
            val p = Period.parse(period)
            (p.years * 365 + p.months * 30 + p.days).takeIf { it > 0 }
        } catch (e: DateTimeParseException) {
            null
        }
}
