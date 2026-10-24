package pl.hexmind.mindshaper.common.ui.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Breadcrumb waiting to be opened: a sparkle next to one view, sending out a ring every few seconds.
 *
 * Lives on the window, above all views - no card or list clips it and the anchor itself stays untouched.
 * Follows the anchor on every frame (scrolling) and hides while the anchor is not fully visible.
 * The side is always given by the caller - there is no default place next to a view.
 * Tap = removed, then onClick. Removed on its own when the anchor leaves the window.
 *
 * Usage: HexBreadcrumbMarker.attach(activity, anchor, Side.START, text) { ... }
 */
@SuppressLint("ViewConstructor")
class HexBreadcrumbMarker private constructor(
    activity: ComponentActivity,
    private val anchor: View,
    private val side: Side,
    private val onClick: () -> Unit
) : View(activity) {

    // START / END of the anchor, mirrored in RTL
    enum class Side { START, END }

    companion object {
        private const val MARKER_SIZE_DP = 52f        // touch target, also room for the full-grown ring
        private const val SPARKLE_SIZE_DP = 24f
        private const val SPARKLE_TO_ANCHOR_GAP_DP = 4f

        // Ring like the mastered star of flashcards
        private const val SIGNAL_SCALE = 2.0f
        private const val SIGNAL_ALPHA = 0.9f
        private const val SIGNAL_MS = 900L
        private const val SIGNAL_FIRST_MS = 600L
        private const val SIGNAL_PAUSE_MS = 2200L

        fun attach(
            activity: ComponentActivity,
            anchor: View,
            side: Side,
            contentDescription: String,
            onClick: () -> Unit
        ): HexBreadcrumbMarker {
            val decorView = activity.window.decorView as ViewGroup
            val marker = HexBreadcrumbMarker(activity, anchor, side, onClick)
            marker.contentDescription = contentDescription
            val size = marker.dp(MARKER_SIZE_DP).toInt()
            decorView.addView(marker, ViewGroup.LayoutParams(size, size))
            return marker
        }
    }

    private val sparkle: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_sparkle)!!.mutate().apply {
        setTint(ContextCompat.getColor(context, R.color.breadcrumb_marker))
    }
    private val ring: Drawable = ContextCompat.getDrawable(context, R.drawable.shape_sparkle_signal)!!.mutate().apply {
        alpha = 0
    }
    private var ringAnimator: ValueAnimator? = null
    private var isRemoved = false

    private val anchorLocation = IntArray(2)
    private val ownParentLocation = IntArray(2)
    private val anchorVisibleRect = Rect()

    private val signal = object : Runnable {
        override fun run() {
            if (ValueAnimator.areAnimatorsEnabled()) sendRing()
            postDelayed(this, SIGNAL_PAUSE_MS)
        }
    }

    private val followAnchor = ViewTreeObserver.OnPreDrawListener {
        placeNextToAnchor()
        true
    }

    private val anchorAttachState = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit
        override fun onViewDetachedFromWindow(view: View) = remove()
    }

    init {
        isClickable = true
        visibility = INVISIBLE    // until the first frame places it
        setOnClickListener {
            remove()
            onClick()
        }

        anchor.addOnAttachStateChangeListener(anchorAttachState)
        postDelayed(signal, SIGNAL_FIRST_MS)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(followAnchor)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(followAnchor)
        super.onDetachedFromWindow()
    }

    fun remove() {
        if (isRemoved) return
        isRemoved = true
        removeCallbacks(signal)
        ringAnimator?.cancel()
        anchor.removeOnAttachStateChangeListener(anchorAttachState)
        (parent as? ViewGroup)?.removeView(this)
    }

    // ! Hidden while the anchor is cut off (e.g. scrolled under the header) - otherwise it would float over it
    private fun placeNextToAnchor() {
        val isAnchorFullyVisible = anchor.isShown &&
            anchor.getGlobalVisibleRect(anchorVisibleRect) &&
            anchorVisibleRect.width() == anchor.width &&
            anchorVisibleRect.height() == anchor.height
        visibility = if (isAnchorFullyVisible) VISIBLE else INVISIBLE
        if (!isAnchorFullyVisible) return

        anchor.getLocationInWindow(anchorLocation)
        (parent as View).getLocationInWindow(ownParentLocation)

        val isRtl = anchor.layoutDirection == LAYOUT_DIRECTION_RTL
        val isOnLeft = (side == Side.START) != isRtl
        val sparkleCenterOffset = dp(SPARKLE_TO_ANCHOR_GAP_DP) + dp(SPARKLE_SIZE_DP) / 2f
        val centerX = if (isOnLeft) {
            anchorLocation[0] - sparkleCenterOffset
        } else {
            anchorLocation[0] + anchor.width + sparkleCenterOffset
        }
        val centerY = anchorLocation[1] + anchor.height / 2f

        translationX = centerX - ownParentLocation[0] - width / 2f
        translationY = centerY - ownParentLocation[1] - height / 2f
    }

    private fun sendRing() {
        val startRadius = dp(SPARKLE_SIZE_DP) / 2f

        ringAnimator?.cancel()
        ringAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SIGNAL_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val radius = startRadius * (1f + (SIGNAL_SCALE - 1f) * progress)
                ring.setBounds(
                    (width / 2f - radius).toInt(), (height / 2f - radius).toInt(),
                    (width / 2f + radius).toInt(), (height / 2f + radius).toInt()
                )
                ring.alpha = ((1f - progress) * SIGNAL_ALPHA * 255).toInt()
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        ring.draw(canvas)

        val half = dp(SPARKLE_SIZE_DP) / 2f
        sparkle.setBounds(
            (width / 2f - half).toInt(), (height / 2f - half).toInt(),
            (width / 2f + half).toInt(), (height / 2f + half).toInt()
        )
        sparkle.draw(canvas)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )
}
