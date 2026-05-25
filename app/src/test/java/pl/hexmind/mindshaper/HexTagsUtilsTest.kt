package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.ui.views.content.HtmlConverter

class HexTagsUtilsTest {

    @Test
    fun `test podstawowy`() {
        val input = "zaczelo sie od tego #a ten tekst jest projektem @ a ten jest dobrą duszą"
        val output = HexTagsUtils.parseInput(input)
        assertEquals("zaczelo sie od tego", output.subject)
        assertEquals("a ten tekst jest projektem", output.project)
        assertEquals("a ten jest dobrą duszą", output.soulMate)
    }

    @Test
    fun `dodatkowe symbole`() {
        val input = "@dobre dusze # zaraz za nim jest projekt"
        val output = HexTagsUtils.parseInput(input)
        assertNull(output.subject)
        assertEquals("zaraz za nim jest projekt", output.project)
        assertEquals("dobre dusze", output.soulMate)
    }

    @Test
    fun `tylko projekt`() {
        val input = "tekst #projekt"
        val output = HexTagsUtils.parseInput(input)
        assertEquals("tekst", output.subject)
        assertEquals("projekt", output.project)
        assertNull(output.soulMate)
    }

    @Test
    fun `tylko soulmate`() {
        val input = "tekst @osoba"
        val output = HexTagsUtils.parseInput(input)
        assertEquals("tekst", output.subject)
        assertNull(output.project)
        assertEquals("osoba", output.soulMate)
    }
}