package to.wim.watchlist.data.model

/**
 * The user's SIMKL plan ("free", "pro", "vip" in the profile's account type), shown on the profile
 * as SIMKL's: a bare "Free" next to the name read as the plan of Watchlist itself.
 */
sealed interface SimklPlan {
    data object Free : SimklPlan

    /** A paid SIMKL plan, by its own name in capitals ("PRO", "VIP"). */
    data class Paid(
        val name: String,
    ) : SimklPlan

    companion object {
        fun of(raw: String?): SimklPlan? {
            val value = raw?.trim().orEmpty()
            return when {
                value.isEmpty() -> null
                value.equals("free", ignoreCase = true) -> Free
                else -> Paid(value.uppercase())
            }
        }
    }
}
