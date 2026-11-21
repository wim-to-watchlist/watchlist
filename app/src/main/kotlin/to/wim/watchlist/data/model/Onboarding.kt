package to.wim.watchlist.data.model

/** Where someone who isn't signed in is: the sign-in screen or the demo. */
enum class SignedOutStep { SignIn, Demo }

/** What the app shows. */
enum class Screen {
    /** Sign in with SIMKL, with "Try the demo" next to it. */
    SignIn,

    /** The app filled with made-up titles. */
    Demo,

    /** Signed in: the app, or the subscription screen (after the first sync) without one. */
    App,
}

/**
 * The order for new users: sign in with SIMKL (free, and most people who install a SIMKL app have
 * an account), let the first sync run, then the subscription screen counts the user's own titles.
 * The demo is for people who want to look first; it leads back to signing in. Someone whose
 * subscription Google Play already knows goes from sign-in straight into the app. Plain Kotlin so
 * it is unit tested.
 */
object Onboarding {
    fun screen(
        signedIn: Boolean,
        step: SignedOutStep,
    ): Screen =
        when {
            signedIn -> Screen.App
            step == SignedOutStep.Demo -> Screen.Demo
            else -> Screen.SignIn
        }
}
