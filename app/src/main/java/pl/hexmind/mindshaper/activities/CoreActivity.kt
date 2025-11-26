package pl.hexmind.mindshaper.activities

import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.mindshaper.R
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

    /**
     * !!! ACHTUNG MINEN !!!
     */
    fun setupHeader(@DrawableRes iconRes: Int, @StringRes titleRes: Int) {
        findViewById<ImageView>(R.id.iv_header_icon)?.setImageResource(iconRes)
        findViewById<TextView>(R.id.tv_header_title)?.setText(titleRes)
    }
}