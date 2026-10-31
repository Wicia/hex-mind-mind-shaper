package pl.hexmind.mindshaper.common.ui.views

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Breadcrumb over the whole screen: everything dimmed except a round hole around one view,
 * a short text with a button under it and a hand-drawn arrow from them to the hole.
 *
 * Text goes above the target when it sits in the lower half of the screen, below it otherwise.
 * Tap in the hole = onTargetClick, the button or back = just closes; onClosed runs in all three.
 * Tap anywhere else does nothing - the spotlight is not closed by accident before it is read.
 * One of several steps (stepCount > 1) shows a counter next to the button.
 * Hole: a circle for buttons, a rounded rectangle for wide views (fields, rows).
 * A target inside a dialog (own window) gets the spotlight in a full-screen window above that dialog.
 *
 * Usage: HexSpotlight.show(activity, targetView, text, buttonText) { ... }
 *        HexSpotlight.showOverDialog(dialog, targetView, text, buttonText) { ... }
 */
@SuppressLint("ViewConstructor")
class HexSpotlight private constructor(
    context: Context,
    backDispatcher: OnBackPressedDispatcher,
    lifecycleOwner: LifecycleOwner,
    private val target: View,
    private val text: String,
    buttonText: String,
    step: Int,
    stepCount: Int,
    private val holeShape: HoleShape,
    private val onClosed: () -> Unit,
    private val onTargetClick: () -> Unit,
    private val onRemoved: () -> Unit = {}
) : FrameLayout(context) {

    enum class HoleShape { CIRCLE, ROUNDED_RECT }

    companion object {
        private const val HOLE_PADDING_DP = 8f
        private const val HOLE_CORNER_DP = 12f
        private const val TEXT_SIDE_MARGIN_DP = 32f
        private const val TEXT_TO_HOLE_GAP_DP = 96f
        private const val TEXT_SIZE_SP = 19f
        private const val TEXT_LINE_SPACING = 0.85f    // Baloo 2 has tall lines - pulled closer
        private const val TEXT_TO_FOOTER_GAP_DP = 16f
        private const val ARROW_THICKNESS_DP = 3f
        private const val ARROW_START_GAP_DP = 12f
        private const val ARROW_END_GAP_DP = 8f
        private const val ARROW_BEND = 0.28f          // sideways bow, part of the arrow length
        private const val ARROW_HEAD_ANGLE_DEG = 28.0
        private const val ARROW_HEAD_LONG_DP = 16f   // uneven sides of the head look drawn by hand
        private const val ARROW_HEAD_SHORT_DP = 12f
        const val FADE_DURATION_MS = 200L

        /**
         * @param buttonText Next when another step follows, Got it on the last one
         * @param step 1-based number of this spotlight among stepCount shown one after another
         */
        fun show(
            activity: ComponentActivity,
            target: View,
            text: String,
            buttonText: String,
            step: Int = 1,
            stepCount: Int = 1,
            holeShape: HoleShape = HoleShape.CIRCLE,
            onClosed: () -> Unit = {},
            onTargetClick: () -> Unit
        ) {
            val decorView = activity.window.decorView as ViewGroup
            val spotlight = HexSpotlight(
                activity, activity.onBackPressedDispatcher, activity,
                target, text, buttonText, step, stepCount, holeShape, onClosed, onTargetClick
            )
            decorView.addView(spotlight, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        /**
         * Target inside a shown dialog - the dialog's window is only as big as the dialog, so the spotlight
         * gets its own window above it: full screen, see-through, no dim of its own. Closed = window gone.
         * ! The window takes the focus - a keyboard of the dialog comes back only when the caller asks for it
         */
        fun showOverDialog(
            dialog: Dialog,
            target: View,
            text: String,
            buttonText: String,
            step: Int = 1,
            stepCount: Int = 1,
            holeShape: HoleShape = HoleShape.CIRCLE,
            onClosed: () -> Unit = {},
            onTargetClick: () -> Unit
        ) {
            val host = ComponentDialog(dialog.context)
            host.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val spotlight = HexSpotlight(
                dialog.context, host.onBackPressedDispatcher, host,
                target, text, buttonText, step, stepCount, holeShape, onClosed, onTargetClick,
                onRemoved = { host.dismiss() }
            )
            host.setContentView(spotlight, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            host.window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                // The spotlight fades in and out by itself
                setWindowAnimations(0)
            }
            host.show()
        }
    }

    private val holeCenter = PointF()
    private var isDismissing = false
    private var holeRadius = 0f          // circle
    private val holeRect = RectF()       // rounded rectangle

    // Target position the hole was placed for - a dialog still settling moves its views after the first layout
    private val placedTargetLocation = IntArray(2)
    private val targetMoveListener = ViewTreeObserver.OnPreDrawListener {
        val location = IntArray(2).also { target.getLocationOnScreen(it) }
        if (!location.contentEquals(placedTargetLocation)) placeHoleAndText()
        true
    }

    private val dimPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
    private val arrowPath = Path()

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.spotlight_dim)
    }

    // Round caps and joins keep the arrow soft, like a marker
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(ARROW_THICKNESS_DP)
        color = ContextCompat.getColor(context, R.color.spotlight_arrow)
    }

    // Text is drawn on the same canvas as the dim and the arrow - wrapped and centered
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP, resources.displayMetrics)
        color = ContextCompat.getColor(context, R.color.spotlight_text)
        typeface = ResourcesCompat.getFont(context, R.font.baloo2)
    }
    private var textLayout: StaticLayout? = null
    private val textOrigin = PointF()

    // Counter + button - real views, placed under the text once the text is laid out
    private val footer: View = LayoutInflater.from(context).inflate(R.layout.common_spotlight_footer, this, false)

    // Back closes the spotlight first, not the screen under it
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dismiss()
            onClosed()
        }
    }

    init {
        setWillNotDraw(false)
        isClickable = true
        alpha = 0f
        contentDescription = text

        footer.findViewById<TextView>(R.id.tv_spotlight_step_counter).apply {
            isVisible = stepCount > 1
            this.text = context.getString(R.string.common_spotlight_step_counter, step, stepCount)
        }
        footer.findViewById<MaterialButton>(R.id.btn_spotlight_action).apply {
            this.text = buttonText
            setOnClickListener {
                if (isDismissing) return@setOnClickListener
                dismiss()
                onClosed()
            }
        }
        footer.visibility = INVISIBLE    // until placed under the text
        addView(footer)

        backDispatcher.addCallback(lifecycleOwner, backCallback)

        // Target and overlay (with the footer inside) must both be laid out to know where the hole goes
        target.doOnLayout {
            doOnLayout {
                placeHoleAndText()
                target.viewTreeObserver.addOnPreDrawListener(targetMoveListener)
                animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
            }
        }
    }

    private fun placeHoleAndText() {
        // On screen, not in window - the target may sit in another window (a dialog)
        val targetLocation = IntArray(2).also { target.getLocationOnScreen(it) }
        val ownLocation = IntArray(2).also { getLocationOnScreen(it) }
        targetLocation.copyInto(placedTargetLocation)

        holeCenter.set(
            targetLocation[0] - ownLocation[0] + target.width / 2f,
            targetLocation[1] - ownLocation[1] + target.height / 2f
        )
        val padding = dp(HOLE_PADDING_DP)
        holeRadius = max(target.width, target.height) / 2f + padding
        holeRect.set(
            holeCenter.x - target.width / 2f - padding, holeCenter.y - target.height / 2f - padding,
            holeCenter.x + target.width / 2f + padding, holeCenter.y + target.height / 2f + padding
        )

        dimPath.reset()
        dimPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        when (holeShape) {
            HoleShape.CIRCLE       -> dimPath.addCircle(holeCenter.x, holeCenter.y, holeRadius, Path.Direction.CW)
            HoleShape.ROUNDED_RECT -> dimPath.addRoundRect(holeRect, dp(HOLE_CORNER_DP), dp(HOLE_CORNER_DP), Path.Direction.CW)
        }

        val isTargetLow = holeCenter.y > height / 2f
        val gap = dp(TEXT_TO_HOLE_GAP_DP)
        val sideMargin = dp(TEXT_SIDE_MARGIN_DP)

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, (width - 2 * sideMargin).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, TEXT_LINE_SPACING)
            .setIncludePad(false)
            .build()
        textLayout = layout

        // Text and footer move as one block - the arrow starts at the block edge facing the hole
        val footerTopInBlock = layout.height + dp(TEXT_TO_FOOTER_GAP_DP)
        val blockHeight = footerTopInBlock + footer.height
        val holeHalfHeight = if (holeShape == HoleShape.CIRCLE) holeRadius else holeRect.height() / 2f
        val blockTop = if (isTargetLow) holeCenter.y - holeHalfHeight - gap - blockHeight else holeCenter.y + holeHalfHeight + gap
        textOrigin.set(sideMargin, blockTop)

        footer.x = (width - footer.width) / 2f
        footer.y = blockTop + footerTopInBlock
        footer.visibility = VISIBLE

        buildArrow(isTargetLow, blockTop, blockTop + blockHeight)
        invalidate()
    }

    // A bowed line from the block edge to the hole edge, with an uneven head
    private fun buildArrow(isTargetLow: Boolean, blockTop: Float, blockBottom: Float) {
        val textCenterX = width / 2f
        val start = PointF(
            textCenterX + (holeCenter.x - textCenterX) * 0.3f,
            if (isTargetLow) blockBottom + dp(ARROW_START_GAP_DP) else blockTop - dp(ARROW_START_GAP_DP)
        )

        val end = arrowEnd(start, isTargetLow)

        // Control point pushed sideways from the middle - one smooth bow
        val dx = end.x - start.x
        val dy = end.y - start.y
        val control = PointF(
            (start.x + end.x) / 2f - dy * ARROW_BEND,
            (start.y + end.y) / 2f + dx * ARROW_BEND
        )

        arrowPath.reset()
        arrowPath.moveTo(start.x, start.y)
        arrowPath.quadTo(control.x, control.y, end.x, end.y)

        // Head follows the curve's direction at its end
        val backAngle = atan2((end.y - control.y).toDouble(), (end.x - control.x).toDouble()) + PI
        val headAngle = Math.toRadians(ARROW_HEAD_ANGLE_DEG)
        addHeadSide(end, backAngle - headAngle, dp(ARROW_HEAD_LONG_DP))
        addHeadSide(end, backAngle + headAngle, dp(ARROW_HEAD_SHORT_DP))
    }

    // End on the hole edge, on the side facing the start
    // Rectangle: its top or bottom edge, under / over the start - kept off the rounded corners
    private fun arrowEnd(start: PointF, isTargetLow: Boolean): PointF {
        val endGap = dp(ARROW_END_GAP_DP)
        return when (holeShape) {
            HoleShape.CIRCLE -> {
                val toStartX = start.x - holeCenter.x
                val toStartY = start.y - holeCenter.y
                val toStartLength = hypot(toStartX, toStartY)
                val endDistance = holeRadius + endGap
                PointF(
                    holeCenter.x + toStartX / toStartLength * endDistance,
                    holeCenter.y + toStartY / toStartLength * endDistance
                )
            }
            HoleShape.ROUNDED_RECT -> {
                val corner = dp(HOLE_CORNER_DP)
                PointF(
                    start.x.coerceIn(holeRect.left + corner, holeRect.right - corner),
                    if (isTargetLow) holeRect.top - endGap else holeRect.bottom + endGap
                )
            }
        }
    }

    private fun addHeadSide(tip: PointF, angle: Double, length: Float) {
        arrowPath.moveTo(tip.x, tip.y)
        arrowPath.lineTo(tip.x + (cos(angle) * length).toFloat(), tip.y + (sin(angle) * length).toFloat())
    }

    // Children (the footer) are drawn after this - on top of the dim
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(dimPath, dimPaint)
        canvas.drawPath(arrowPath, arrowPaint)

        textLayout?.let { layout ->
            canvas.save()
            canvas.translate(textOrigin.x, textOrigin.y)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    // Reaches here only outside the button - consumed either way, so nothing under the spotlight reacts
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && !isDismissing) {
            val isInHole = when (holeShape) {
                HoleShape.CIRCLE       -> hypot(event.x - holeCenter.x, event.y - holeCenter.y) <= holeRadius
                HoleShape.ROUNDED_RECT -> holeRect.contains(event.x, event.y)
            }
            if (isInHole) {
                dismiss()
                onClosed()
                onTargetClick()
            }
        }
        return true
    }

    private fun dismiss() {
        isDismissing = true
        backCallback.remove()
        if (target.viewTreeObserver.isAlive) target.viewTreeObserver.removeOnPreDrawListener(targetMoveListener)
        animate().alpha(0f).setDuration(FADE_DURATION_MS).withEndAction {
            (parent as? ViewGroup)?.removeView(this)
            onRemoved()
        }.start()
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )
}
