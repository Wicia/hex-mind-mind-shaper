package pl.hexmind.fastnote.features.main

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.features.carousel.ThoughtProcessingPhase
import pl.hexmind.fastnote.features.carousel.ThoughtProcessingPhaseName

/*
 * Core activity which should be parent  to be inherited by children
 */
// TODO: Do przeniesienia? albo czy zrobić oddzielna klase tylko do sprawdzania fazy?
open class CoreActivity() : AppCompatActivity() {

    internal val phaseToResourceMap = mapOf(
        ThoughtProcessingPhaseName.GATHERING to R.drawable.ic_phase_gathering,
        ThoughtProcessingPhaseName.CHOOSING to R.drawable.ic_phase_choosing,
        ThoughtProcessingPhaseName.SILENT to R.drawable.ic_phase_silent,
    )
    internal val phaseToHeaderStringMap = mapOf(
        ThoughtProcessingPhaseName.GATHERING to R.string.core_phase1_default_name,
        ThoughtProcessingPhaseName.CHOOSING to R.string.core_phase2_default_name,
        ThoughtProcessingPhaseName.SILENT to R.string.core_phase3_default_name,
    )

    fun getCurrentPhase() : ThoughtProcessingPhase{
        return ThoughtProcessingPhase(ThoughtProcessingPhaseName.CHOOSING, 10)
    }

    fun showShortToast(stringId : Int, param : String? = ""){
        Toast.makeText(this, getString(stringId, param), Toast.LENGTH_SHORT).show()
    }
}