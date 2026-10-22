package pl.hexmind.mindshaper.common.ui.views

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout
import pl.hexmind.mindshaper.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Breadcrumb over the whole screen: everything dimmed except a round hole around one view,
 * a short text and a hand-drawn arrow from the text to the hole.
 *
 * Text goes above the target when it sits in the lower half of the screen, below it otherwise.
 * Tap in the hole = onTargetClick, tap anywhere else or back = just closes; onClosed runs either way.
 *
 * Usage: HexSpotlight.show(activity, targetView, text) { ... }
 */
@SuppressLint("ViewConstructor")
class HexSpotlight private constructor(
    private val activity: ComponentActivity,
    private val target: View,
    private val text: String,
    private val onClosed: () -> Unit,
    private val onTargetClick: () -> Unit
) : View(activity) {

    companion object {
        private const val HOLE_PADDING_DP = 8f
        private const val TEXT_SIDE_MARGIN_DP = 32f
        private const val TEXT_TO_HOLE_GAP_DP = 96f
        private const val TEXT_SIZE_SP = 19f
        private const val TEXT_LINE_SPACING = 0.85f    // Baloo 2 has tall lines - pulled closer
        private const val ARROW_THICKNESS_DP = 3f
        private const val ARROW_START_GAP_DP = 12f
        private const val ARROW_END_GAP_DP = 8f
        private const val ARROW_BEND = 0.28f          // sideways bow, part of the arrow length
        private const val ARROW_HEAD_ANGLE_DEG = 28.0
        private const val ARROW_HEAD_LONG_DP = 16f   // uneven sides of the head look drawn by hand
        private const val ARROW_HEAD_SHORT_DP = 12f
        private const val FADE_DURATION_MS = 200L

        fun show(
            activity: ComponentActivity,
            target: View,
            text: String,
            onClosed: () -> Unit = {},
            onTargetClick: () -> Unit
        ) {
            val decorView = activity.window.decorView as ViewGroup
            val spotlight = HexSpotlight(activity, target, text, onClosed, onTargetClick)
            decorView.addView(spotlight, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private val holeCenter = PointF()
    private var isDismissing = false
    private var holeRadius = 0f

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

    // Back closes the spotlight first, not the screen under it
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            dismiss()
            onClosed()
        }
    }

    init {
        isClickable = true
        alpha = 0f
        contentDescription = text

        activity.onBackPressedDispatcher.addCallback(activity, backCallback)

        // Target and overlay must both be laid out to know where the hole goes
        target.doOnLayout {
            doOnLayout {
                placeHoleAndText()
                animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
            }
        }
    }

    private fun placeHoleAndText() {
        val targetLocation = IntArray(2).also { target.getLocationInWindow(it) }
        val ownLocation = IntArray(2).also { getLocationInWindow(it) }

        holeCenter.set(
            targetLocation[0] - ownLocation[0] + target.width / 2f,
            targetLocation[1] - ownLocation[1] + target.height / 2f
        )
        holeRadius = max(target.width, target.height) / 2f + dp(HOLE_PADDING_DP)

        dimPath.reset()
        dimPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        dimPath.addCircle(holeCenter.x, holeCenter.y, holeRadius, Path.Direction.CW)

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

        val textTop = if (isTargetLow) holeCenter.y - holeRadius - gap - layout.height else holeCenter.y + holeRadius + gap
        textOrigin.set(sideMargin, textTop)

        buildArrow(isTargetLow, textTop, textTop + layout.height)
        invalidate()
    }

    // A bowed line from the text edge to the hole edge, with an uneven head
    private fun buildArrow(isTargetLow: Boolean, textTop: Float, textBottom: Float) {
        val textCenterX = width / 2f
        val start = PointF(
            textCenterX + (holeCenter.x - textCenterX) * 0.3f,
            if (isTargetLow) textBottom + dp(ARROW_START_GAP_DP) else textTop - dp(ARROW_START_GAP_DP)
        )

        // End on the hole edge, on the side facing the start
        val toStartX = start.x - holeCenter.x
        val toStartY = start.y - holeCenter.y
        val toStartLength = hypot(toStartX, toStartY)
        val endDistance = holeRadius + dp(ARROW_END_GAP_DP)
        val end = PointF(
            holeCenter.x + toStartX / toStartLength * endDistance,
            holeCenter.y + toStartY / toStartLength * endDistance
        )

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

    private fun addHeadSide(tip: PointF, angle: Double, length: Float) {
        arrowPath.moveTo(tip.x, tip.y)
        arrowPath.lineTo(tip.x + (cos(angle) * length).toFloat(), tip.y + (sin(angle) * length).toFloat())
    }

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && !isDismissing) {
            val isInHole = hypot(event.x - holeCenter.x, event.y - holeCenter.y) <= holeRadius
            dismiss()
            onClosed()
            if (isInHole) onTargetClick()
        }
        return true
    }

    private fun dismiss() {
        isDismissing = true
        backCallback.remove()
        animate().alpha(0f).setDuration(FADE_DURATION_MS).withEndAction {
            (parent as? ViewGroup)?.removeView(this)
        }.start()
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )
}
