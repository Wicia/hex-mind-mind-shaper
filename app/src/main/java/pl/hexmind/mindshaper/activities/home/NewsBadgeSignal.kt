package pl.hexmind.mindshaper.activities.home

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Wave of the unread news badge on Home: every few seconds a ring leaves the badge, grows and fades out.
 * Same rhythm as the breadcrumb ring (HexBreadcrumbMarker).
 *
 * Runs only while the screen is visible - [start] in onResume, [stop] in onPause.
 */
class NewsBadgeSignal(private val ring: View) {

    private var isRunning = false
    private var ringAnimator: ValueAnimator? = null

    private val signal = object : Runnable {
        override fun run() {
            if (!isRunning) return

            sendRing()
            ring.postDelayed(this, SIGNAL_PAUSE_MS)
        }
    }

    fun start() {
        // ! Animations off in the system settings = no wave, the badge stays still
        if (isRunning || !ValueAnimator.areAnimatorsEnabled()) return

        isRunning = true
        ring.postDelayed(signal, SIGNAL_FIRST_MS)
    }

    fun stop() {
        isRunning = false
        ring.removeCallbacks(signal)
        ringAnimator?.cancel()
        ring.alpha = 0f
    }

    private fun sendRing() {
        ringAnimator?.cancel()
        ringAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SIGNAL_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val scale = 1f + (SIGNAL_SCALE - 1f) * progress
                ring.scaleX = scale
                ring.scaleY = scale
                ring.alpha = (1f - progress) * SIGNAL_ALPHA
            }
            start()
        }
    }

    private companion object {
        const val SIGNAL_SCALE = 2.0f
        const val SIGNAL_ALPHA = 0.9f
        const val SIGNAL_MS = 900L
        const val SIGNAL_FIRST_MS = 600L
        const val SIGNAL_PAUSE_MS = 2200L
    }
}
