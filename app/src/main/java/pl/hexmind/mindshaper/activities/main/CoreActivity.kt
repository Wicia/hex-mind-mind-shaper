package pl.hexmind.mindshaper.activities.main

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.mindshaper.activities.carousel.ThoughtProcessingPhase
import pl.hexmind.mindshaper.activities.carousel.ThoughtProcessingPhaseName
import pl.hexmind.mindshaper.services.PhasesService
import javax.inject.Inject

/**
 * Core activity which should be parent  to be inherited by children
 */
open class CoreActivity() : AppCompatActivity() {

    @Inject
    lateinit var phasesService: PhasesService

    fun getCurrentPhase() : ThoughtProcessingPhase{
        return ThoughtProcessingPhase(ThoughtProcessingPhaseName.CHOOSING, 10)
    }

    fun showShortToast(stringId : Int, param : String? = ""){
        Toast.makeText(this, getString(stringId, param), Toast.LENGTH_SHORT).show()
    }
}