package pl.hexmind.fastnote.features.main

import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.fastnote.features.carousel.ThoughtProcessingPhase
import pl.hexmind.fastnote.features.carousel.ThoughtProcessingPhaseName

/*
 * Core activity which should be parent  to be inherited by children
 */
// TODO: Do przeniesienia? albo czy zrobić oddzielna klase tylko do sprawdzania fazy?
open class CoreActivity() : AppCompatActivity() {

    fun getCurrentPhase() : ThoughtProcessingPhase{
        return ThoughtProcessingPhase(ThoughtProcessingPhaseName.GATHERING, 10)
    }
}