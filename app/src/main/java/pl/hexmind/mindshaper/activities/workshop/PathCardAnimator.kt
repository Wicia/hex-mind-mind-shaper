package pl.hexmind.mindshaper.activities.workshop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Encapsulates all card transition animations for the Workshop path cards.
 *
 * Three animation phases:
 *  1. [revealWithFlip]              — gray card flips on Y-axis to reveal the orange step card
 *  2. [advanceWithFade]             — current step fades out, next step fades in
 *  3. [advanceLastStepWithSlideOut] — last step slides up and fades out (with a pause before)
 *
 * Call [onCardBuilt] from [WorkshopActivity.rebuildPathCards] for every newly inflated card
 * so that the second halves of phase-1 and phase-2 animations fire correctly.
 */
class PathCardAnimator(private val viewModel: WorkshopViewModel) {

    private val pendingRevealKeys  = mutableSetOf<String>()
    private val pendingAdvanceKeys = mutableSetOf<String>()

    // ── Called from rebuildPathCards ───────────────────────────────────────────

    /**
     * Must be called after each card view is added to the list.
     * Triggers the incoming half of whichever animation is pending for this [pathKey].
     */
    fun onCardBuilt(view: View, pathKey: String) {
        when {
            pathKey in pendingRevealKeys  -> animateFlipIn(view, pathKey)
            pathKey in pendingAdvanceKeys -> animateFadeIn(view, pathKey)
        }
    }

    // ── Phase 1: reveal ────────────────────────────────────────────────────────

    /** Rotates the gray card 0° → 90°, then triggers reveal; phase 2 fires in [onCardBuilt]. */
    fun revealWithFlip(cardView: View, pathKey: String) {
        ObjectAnimator.ofFloat(cardView, "rotationY", 0f, 90f).apply {
            duration = 220
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    pendingRevealKeys.add(pathKey)
                    viewModel.revealPath(pathKey)
                }
            })
            start()
        }
    }

    private fun animateFlipIn(view: View, pathKey: String) {
        pendingRevealKeys.remove(pathKey)
        view.rotationY = -90f
        ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    // ── Phase 2: advance step ──────────────────────────────────────────────────

    /** Fades the current step card out, then advances; phase 2 fires in [onCardBuilt]. */
    fun advanceWithFade(cardView: View, pathKey: String) {
        ObjectAnimator.ofFloat(cardView, "alpha", 1f, 0f).apply {
            duration = 200
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    pendingAdvanceKeys.add(pathKey)
                    viewModel.advanceToNextStep(pathKey)
                }
            })
            start()
        }
    }

    private fun animateFadeIn(view: View, pathKey: String) {
        pendingAdvanceKeys.remove(pathKey)
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    // ── Phase 3: last step ─────────────────────────────────────────────────────

    /** Pauses 1.5 s, then slides the last-step card up while fading it out. */
    fun advanceLastStepWithSlideOut(cardView: View, pathKey: String) {
        cardView.animate()
            .translationY(-cardView.height.toFloat())
            .alpha(0f)
            .setStartDelay(1500)
            .setDuration(350)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { viewModel.advanceToNextStep(pathKey) }
            .start()
    }
}