package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.hexmind.mindshaper.common.regex.HexTagsUtils

class HexTagsUtilsTest {

    @Test
    fun `test podstawowy`() {
        val input = "zaczelo sie od tego #a ten tekst jest projektem @ a ten jest dobrą duszą"
        val output = HexTagsUtils.parseInput(input)
        assertEquals("zaczelo sie od tego", output.subject)
        assertEquals("a ten tekst jest projektem", output.project)
        assertEquals("a ten jest dobrą duszą", output.person)
    }

    @Test
    fun `dodatkowe symbole`() {
        val input = "@dobre dusze # zaraz za nim jest projekt"
        val output = HexTagsUtils.parseInput(input)
        assertNull(output.subject)
        assertEquals("zaraz za nim jest projekt", output.project)
        assertEquals("dobre dusze", output.person)
    }

    @Test
    fun `tylko projekt`() {
        val input = "tekst #projekt"
        val output = HexTagsUtils.parseInput(input)
        assertEquals("tekst", output.subject)
        assertEquals("projekt", output.project)
        assertNull(output.person)
    }

    @Test
    fun `tylko person`() {
        val input = "tekst @osoba"
        val output = HexTagsUtils.parseInput(input)
        assertEquals("tekst", output.subject)
        assertNull(output.project)
        assertEquals("osoba", output.person)
    }

    @Test
    fun `markery w tekscie notatki - tylko pojedyncze slowo po markerze`() {
        val input = "Spotkanie z @Michałem dotyczące #ProjektuX było owocne"
        val output = HexTagsUtils.extractEmbeddedTags(input)
        assertEquals("Michałem", output.person)
        assertEquals("ProjektuX", output.project)
        assertNull(output.subject)
    }

    @Test
    fun `markery w tekscie notatki - wiele tagow tego samego typu`() {
        val input = "Rozmowa z @Michałem i @Anią o #pracy i #domu"
        val output = HexTagsUtils.extractEmbeddedTags(input)
        assertEquals("Michałem Anią", output.person)
        assertEquals("pracy domu", output.project)
    }

    @Test
    fun `markery w tekscie notatki - marker w srodku slowa nie jest tagiem`() {
        val input = "@Ania napisala na cos@poczta.pl w sprawie abc#123"
        val output = HexTagsUtils.extractEmbeddedTags(input)
        assertEquals("Ania", output.person)
        assertNull(output.project)
    }

    @Test
    fun `markery w tekscie notatki - brak markerow`() {
        val output = HexTagsUtils.extractEmbeddedTags("zwykly tekst bez tagow")
        assertNull(output.person)
        assertNull(output.project)
    }

    @Test
    fun `mergeTagNames - dokleja nowe tagi bez duplikatow`() {
        val merged = HexTagsUtils.mergeTagNames("michal ania", "Michal krzysiek")
        assertEquals("michal ania krzysiek", merged)
    }

    @Test
    fun `mergeTagNames - oba puste`() {
        assertNull(HexTagsUtils.mergeTagNames(null, null))
    }
}