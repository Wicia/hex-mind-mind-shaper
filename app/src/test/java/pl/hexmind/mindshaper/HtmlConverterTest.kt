package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.hexmind.mindshaper.common.ui.HtmlConverter

class HtmlConverterTest {

    @Test
    fun `test pusty tekst zwraca pusty string`() {
        assertEquals("", HtmlConverter.convertToHtml(""))
    }

    @Test
    fun `test pojedyncza linia tekstu`() {
        val input = "tekst"
        val expected = "tekst"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test dwie linie bez pustej linii - dodaje br`() {
        val input = "tekst1\ntekst2"
        val expected = "tekst1<br>tekst2"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test dwie linie z pusta linia - pierwszy bez p drugi z p`() {
        val input = "tekst1\n\ntekst2"
        val expected = "tekst1<p>tekst2</p>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test pojedynczy element listy`() {
        val input = "X element"
        val expected = "<ul><li>element</li></ul>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test dwa elementy listy`() {
        val input = "X element 1\nX element 2"
        val expected = "<ul><li>element 1</li><li>element 2</li></ul>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test tekst przed lista bez pustej linii`() {
        val input = "nagłówek\nX element"
        val expected = "nagłówek<br><ul><li>element</li></ul>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test tekst przed lista z pusta linia`() {
        val input = "nagłówek\n\nX element"
        val expected = "nagłówek<br><ul><li>element</li></ul>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test lista po ktorej jest tekst bez pustej linii`() {
        val input = "X element\ntekst"
        val expected = "<ul><li>element</li></ul><br>tekst"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test lista po ktorej jest tekst z pusta linia`() {
        val input = "X element\n\ntekst"
        val expected = "<ul><li>element</li></ul><p>tekst</p>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test kompleksowy scenariusz`() {
        val input = "Wstęp\nX punkt 1\nX punkt 2\n\nZakończenie"
        val expected = "Wstęp<br><ul><li>punkt 1</li><li>punkt 2</li></ul><p>Zakończenie</p>"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }

    @Test
    fun `test trzy linie tekstu bez pustych linii`() {
        val input = "linia1\nlinia2\nlinia3"
        val expected = "linia1<br>linia2<br>linia3"
        assertEquals(expected, HtmlConverter.convertToHtml(input))
    }
}