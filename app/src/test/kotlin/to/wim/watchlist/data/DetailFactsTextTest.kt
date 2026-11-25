package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import to.wim.watchlist.data.model.Genre
import to.wim.watchlist.data.model.countryName
import to.wim.watchlist.data.model.showsAirTime
import java.util.Locale

class DetailFactsTextTest {
    @Test
    fun airTimeIsHiddenOnceAShowHasEnded() {
        assertFalse(showsAirTime("ended"))
        assertFalse(showsAirTime("Canceled"))
        assertTrue(showsAirTime("airing"))
        assertTrue(showsAirTime("returning series"))
        // Unknown or missing: SIMKL's air time is still shown.
        assertTrue(showsAirTime(null))
        assertTrue(showsAirTime("something new"))
    }

    @Test
    fun countryCodesBecomeNamesInThePhonesLanguage() {
        assertEquals("Austria", countryName("AT", Locale.ENGLISH))
        assertEquals("Austria", countryName("at", Locale.ENGLISH))
        assertEquals("Oostenrijk", countryName("AT", Locale.forLanguageTag("nl")))
        assertEquals("Österreich", countryName("AT", Locale.GERMAN))
    }

    @Test
    fun anythingElseIsShownAsSimklWroteIt() {
        assertEquals("United Kingdom", countryName("United Kingdom", Locale.ENGLISH))
        assertEquals("XX", countryName("XX", Locale.ENGLISH))
        assertEquals("A1", countryName("A1", Locale.ENGLISH))
    }

    @Test
    fun genresAreMatchedWhateverSimklsSpelling() {
        assertEquals(Genre.ScienceFiction, Genre.fromApi("science-fiction"))
        assertEquals(Genre.ScienceFiction, Genre.fromApi("Science Fiction"))
        assertEquals(Genre.ScienceFiction, Genre.fromApi("sci-fi"))
        assertEquals(Genre.GameShow, Genre.fromApi("game-show"))
        assertEquals(Genre.Drama, Genre.fromApi(" Drama "))
        assertEquals(Genre.Children, Genre.fromApi("kids"))
        assertNull(Genre.fromApi("isekai"))
    }
}
