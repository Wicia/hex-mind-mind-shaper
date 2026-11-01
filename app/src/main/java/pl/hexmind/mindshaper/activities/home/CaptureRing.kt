package pl.hexmind.mindshaper.activities.home

import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.common.ui.AddFormButtons
import pl.hexmind.mindshaper.common.ui.dpToPx
import kotlin.math.cos
import kotlin.math.sin

/**
 * Capture ring on Home: fab tap = the capture options (note, photo, voice, flashcards) fly out around it
 * and the fab turns by 45 degrees, another fab tap hides them and turns it back. Back or an option tap close it too.
 * Options spread evenly on an ellipse from the top, clockwise - wider than tall, as the options are.
 * Options are add-form buttons (AddFormButtons): "+" and the icon, or gray "?" and the icon for a feature turned off -
 * its tap calls onDisabledTap instead.
 *
 * XML: options and the fab centered in one FrameLayout (options gone), the fab last = on top.
 *
 * Usage:
 *   val ring = CaptureRing(this, fab)
 *   ring.addOption(findViewById(R.id.btn_foo), isEnabled = { … }, onDisabledTap = { … }) { … }  // clockwise from the top
 */
class CaptureRing(
    activity: ComponentActivity,
    private val fab: View
) {

    private class Option(val view: MaterialButton, val isEnabled: () -> Boolean)

    private val options = mutableListOf<Option>()

    var isOpen = false
        private set

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = close()
    }

    init {
        fab.setOnClickListener { toggle() }
        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
    }

    /** Options go clockwise from the top in the order they are added; [isEnabled] is checked on each open and tap */
    fun addOption(
        view: MaterialButton,
        isEnabled: () -> Boolean = { true },
        onDisabledTap: () -> Unit = {},
        onTap: () -> Unit
    ) {
        options.add(Option(view, isEnabled))
        view.setOnClickListener {
            close(animate = false)
            if (isEnabled()) onTap() else onDisabledTap()
        }
    }

    fun toggle() = if (isOpen) close() else open()

    fun open() {
        if (isOpen) return
        isOpen = true
        backCallback.isEnabled = true

        fab.animate().rotation(ROTATION_OPEN).setDuration(ANIMATION_MS).start()

        val radiusX = fab.context.dpToPx(RADIUS_X_DP).toFloat()
        val radiusY = fab.context.dpToPx(RADIUS_Y_DP).toFloat()
        options.forEachIndexed { index, option ->
            AddFormButtons.applyState(option.view, option.isEnabled())

            val angle = Math.toRadians(START_ANGLE_DEG + FULL_CIRCLE_DEG * index / options.size)
            option.view.apply {
                animate().cancel()
                translationX = 0f
                translationY = 0f
                alpha = 0f
                scaleX = START_SCALE
                scaleY = START_SCALE
                visibility = View.VISIBLE
                animate()
                    .translationX(radiusX * cos(angle).toFloat())
                    .translationY(radiusY * sin(angle).toFloat())
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setInterpolator(OvershootInterpolator())
                    .setDuration(ANIMATION_MS)
                    .start()
            }
        }
    }

    /** animate = false - an option opens another screen or a dialog right away, the ring must not fly back under it */
    fun close(animate: Boolean = true) {
        if (!isOpen) return
        isOpen = false
        backCallback.isEnabled = false

        fab.animate().cancel()
        if (animate) fab.animate().rotation(0f).setDuration(ANIMATION_MS).start() else fab.rotation = 0f

        options.forEach { option ->
            option.view.apply {
                animate().cancel()
                if (animate && visibility == View.VISIBLE) {
                    animate()
                        .translationX(0f).translationY(0f)
                        .alpha(0f).scaleX(START_SCALE).scaleY(START_SCALE)
                        .setInterpolator(null)
                        .setDuration(ANIMATION_MS)
                        .withEndAction { visibility = View.GONE }
                        .start()
                }
                else {
                    visibility = View.GONE
                }
            }
        }
    }

    private companion object {
        const val ANIMATION_MS = 200L
        const val ROTATION_OPEN = 45f
        // Fab center -> option center, ~25dp gap: 28dp fab radius + half of the option (~92dp x 48dp) + gap
        const val RADIUS_X_DP = 100
        const val RADIUS_Y_DP = 76
        // First option straight above the fab (screen y grows down)
        const val START_ANGLE_DEG = -90.0
        const val FULL_CIRCLE_DEG = 360.0
        const val START_SCALE = 0.5f
    }
}
