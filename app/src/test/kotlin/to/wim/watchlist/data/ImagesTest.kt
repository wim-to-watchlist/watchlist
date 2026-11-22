package to.wim.watchlist.data

import org.junit.Assert.assertEquals
import org.junit.Test
import to.wim.watchlist.data.model.Images

/** Every image goes through wsrv.nl, so the phone contacts no host PRIVACY.md doesn't list. */
class ImagesTest {
    @Test
    fun `a SIMKL image path goes through wsrv`() {
        assertEquals("https://wsrv.nl/?url=https://simkl.in/posters/74/abc_ca.webp", Images.poster("74/abc"))
    }

    @Test
    fun `a full image URL from SIMKL goes through wsrv too`() {
        assertEquals(
            "https://wsrv.nl/?url=https%3A%2F%2Fexample.com%2Fa.jpg%3Fs%3D1",
            Images.avatar("https://example.com/a.jpg?s=1"),
        )
        assertEquals("https://wsrv.nl/?url=https%3A%2F%2Fexample.com%2Fa.jpg", Images.poster("//example.com/a.jpg"))
    }

    @Test
    fun `a URL that already uses wsrv is kept`() {
        val url = "https://wsrv.nl/?url=https://simkl.in/x.jpg"
        assertEquals(url, Images.fanart(url))
    }

    @Test
    fun `trailer thumbnails go through wsrv`() {
        assertEquals("https://wsrv.nl/?url=https://img.youtube.com/vi/abc123/mqdefault.jpg", Images.youtube("abc123"))
    }
}
