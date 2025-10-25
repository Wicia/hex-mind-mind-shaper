package pl.hexmind.mindshaper.activities

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.mindshaper.services.PhasesService
import javax.inject.Inject

/**
 * Core activity which should be parent  to be inherited by children
 */
open class CoreActivity() : AppCompatActivity() {

    @Inject
    lateinit var phasesService: PhasesService

    fun showShortToast(stringResourceId : Int, param : String? = ""){
        Toast.makeText(this, getString(stringResourceId, param), Toast.LENGTH_SHORT).show()
    }

    fun showErrorAndFinish(stringResourceId : Int) {
        Toast.makeText(this, getString(stringResourceId), Toast.LENGTH_SHORT).show()
        finish()
    }
}