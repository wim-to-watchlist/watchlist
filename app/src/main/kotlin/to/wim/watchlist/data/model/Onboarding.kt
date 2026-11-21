package to.wim.watchlist.data.model

/** Where someone who isn't signed in is: the welcome screen, the demo, or the subscription screen. */
enum class SignedOutStep { Welcome, Demo, Subscribe }

/** What the app shows. */
enum class Screen {
    /** Try the demo or start the free trial. */
    Welcome,

    /** The app filled with made-up titles. */
    Demo,

    /** The subscription screen, before signing in. */
    Subscribe,

    /** Sign in with SIMKL: the subscription is active (or not needed in this build). */
    SignIn,

    /** Signed in: the app itself, or the subscription screen when it lapsed. */
    App,
}

/**
 * The order for new users: the demo first, then the subscription (Google Play, tied to the
 * Google account), then signing in with SIMKL. Someone whose subscription Google Play already
 * knows (a reinstall, a new phone) goes straight to signing in. Plain Kotlin so it is unit tested.
 */
object Onboarding {
    fun screen(
        signedIn: Boolean,
        access: Access,
        step: SignedOutStep,
    ): Screen =
        when {
            signedIn -> Screen.App
            step == SignedOutStep.Demo -> Screen.Demo
            access == Access.Allowed -> Screen.SignIn
            step == SignedOutStep.Subscribe -> Screen.Subscribe
            else -> Screen.Welcome
        }
}
