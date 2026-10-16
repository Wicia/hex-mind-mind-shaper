package pl.hexmind.mindshaper.activities.home

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dpToPx
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lab bubbles over the onboarding flask on Home: every so often a small ring leaves the flask's
 * neck, rises with a slight wobble, grows a little and fades out.
 *
 * Runs only while the screen is visible - [start] in onResume, [stop] in onPause.
 */
class LabBubblesAnimator(
    private val container: FrameLayout,
    private val flaskIcon: View
) {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val spawnNextBubble = object : Runnable {
        override fun run() {
            if (!isRunning) return

            spawnBubble()
            handler.postDelayed(this, Random.nextLong(SPAWN_MIN_MS, SPAWN_MAX_MS))
        }
    }

    fun start() {
        // ! Animations off in the system settings = no bubbles, the icon stays still
        if (isRunning || !ValueAnimator.areAnimatorsEnabled()) return

        isRunning = true
        handler.post(spawnNextBubble)
    }

    // Bubbles already in the air finish on their own and remove themselves
    fun stop() {
        isRunning = false
        handler.removeCallbacks(spawnNextBubble)
    }

    private fun spawnBubble() {
        // ! Before the first layout the icon has no size yet - the bubble would start in the corner
        if (flaskIcon.width == 0) return

        val context = container.context
        val bubbleSize = context.dpToPx(Random.nextInt(BUBBLE_MIN_DP, BUBBLE_MAX_DP + 1))

        val bubble = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.shape_lab_bubble)
            alpha = 0f
        }
        container.addView(bubble, FrameLayout.LayoutParams(bubbleSize, bubbleSize))

        // Neck of the flask: horizontal centre of the icon, just below its top edge
        val startX = flaskIcon.x + flaskIcon.width / 2f - bubbleSize / 2f +
            context.dpToPx(Random.nextInt(-NECK_SPREAD_DP, NECK_SPREAD_DP + 1))
        val drawnHeight = flaskIcon.height - flaskIcon.paddingTop - flaskIcon.paddingBottom
        val startY = flaskIcon.y + flaskIcon.paddingTop + drawnHeight * NECK_TOP_FRACTION - bubbleSize / 2f

        val rise = context.dpToPx(Random.nextInt(RISE_MIN_DP, RISE_MAX_DP + 1)).toFloat()
        val wobble = context.dpToPx(WOBBLE_DP).toFloat() * (if (Random.nextBoolean()) 1 else -1)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = Random.nextLong(RISE_MIN_MS, RISE_MAX_MS)
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float

                bubble.x = startX + wobble * sin(progress * 2 * PI).toFloat()
                bubble.y = startY - rise * progress

                val growth = SCALE_START + (1f - SCALE_START) * progress
                bubble.scaleX = growth
                bubble.scaleY = growth

                // Quick fade in at the neck, slow fade out towards the top
                bubble.alpha = MAX_ALPHA * if (progress < FADE_IN_PART) {
                    progress / FADE_IN_PART
                }
                else {
                    (1f - progress) / (1f - FADE_IN_PART)
                }
            }
            doOnEnd { container.removeView(bubble) }
            start()
        }
    }

    private companion object {
        const val SPAWN_MIN_MS = 450L
        const val SPAWN_MAX_MS = 1100L

        const val RISE_MIN_MS = 1400L
        const val RISE_MAX_MS = 2200L

        const val BUBBLE_MIN_DP = 4
        const val BUBBLE_MAX_DP = 8

        const val RISE_MIN_DP = 40
        const val RISE_MAX_DP = 64

        const val WOBBLE_DP = 3
        const val NECK_SPREAD_DP = 2

        // The flask's mouth sits ~15% down the icon's drawn area
        const val NECK_TOP_FRACTION = 0.15f

        const val SCALE_START = 0.6f
        const val MAX_ALPHA = 0.9f
        const val FADE_IN_PART = 0.2f
    }
}
