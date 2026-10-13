package pl.hexmind.mindshaper.common.ui.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.hexmind.mindshaper.R

/**
 * Speed dial over the screen's fab: tap = scrim + options (common_speed_dial_option) above the fab, "+" turns into "x".
 * Scrim tap, fab tap, Back or an option tap close it.
 * The fab's own click stays with the screen - it calls [toggle] when the dial applies (e.g. only on one tab).
 *
 * XML: <include layout="@layout/common_speed_dial" /> before the fab in the CoordinatorLayout.
 *
 * Usage:
 *   val speedDial = SpeedDial(this, fab)
 *   speedDial.addOption(R.string.foo, R.drawable.ic_foo) { … }  // top to bottom
 *   fab.setOnClickListener { speedDial.toggle() }
 */
class SpeedDial(
    activity: ComponentActivity,
    private val fab: FloatingActionButton
) {

    private val scrim: View = activity.findViewById(R.id.v_speed_dial_scrim)
    private val options: ViewGroup = activity.findViewById(R.id.ll_speed_dial_options)

    var isOpen = false
        private set

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = close()
    }

    init {
        scrim.setOnClickListener { close() }
        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
    }

    /** Options stack top to bottom in the order they are added - the last one sits right above the fab */
    fun addOption(@StringRes labelRes: Int, @DrawableRes iconRes: Int, onTap: () -> Unit) {
        val option = LayoutInflater.from(options.context).inflate(R.layout.common_speed_dial_option, options, false)
        options.addView(option)

        val label = option.findViewById<TextView>(R.id.tv_speed_dial_label)
        val optionFab = option.findViewById<FloatingActionButton>(R.id.fab_speed_dial_option)
        label.setText(labelRes)
        optionFab.setImageResource(iconRes)
        optionFab.contentDescription = label.text

        // Label tap works like the mini fab - a bigger target
        listOf(label, optionFab).forEach { target ->
            target.setOnClickListener {
                close(animate = false)
                onTap()
            }
        }
    }

    fun toggle() = if (isOpen) close() else open()

    fun open() {
        if (isOpen) return
        isOpen = true
        backCallback.isEnabled = true

        fadeIn(scrim, slideUp = false)
        fadeIn(options, slideUp = true)
        fab.animate().rotation(ROTATION_OPEN).setDuration(ANIMATION_MS).start()
    }

    /** animate = false - an option opens a dialog right away, the dial must not fade under it */
    fun close(animate: Boolean = true) {
        if (!isOpen) return
        isOpen = false
        backCallback.isEnabled = false

        if (animate) {
            fadeOut(scrim)
            fadeOut(options)
            fab.animate().rotation(0f).setDuration(ANIMATION_MS).start()
        }
        else {
            listOf(scrim, options).forEach { view -> view.visibility = View.GONE }
            fab.animate().cancel()
            fab.rotation = 0f
        }
    }

    private fun fadeIn(view: View, slideUp: Boolean) {
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = if (slideUp) slideDistance() else 0f
        view.visibility = View.VISIBLE
        view.animate().alpha(1f).translationY(0f).setDuration(ANIMATION_MS).start()
    }

    private fun fadeOut(view: View) {
        view.animate().cancel()
        view.animate().alpha(0f).setDuration(ANIMATION_MS)
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    private fun slideDistance(): Float = SLIDE_DP * fab.resources.displayMetrics.density

    companion object {
        private const val ANIMATION_MS = 150L
        // "+" rotated by 45 degrees reads as "x"
        private const val ROTATION_OPEN = 45f
        private const val SLIDE_DP = 16f
    }
}
