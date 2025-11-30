package pl.hexmind.mindshaper.common.audio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import kotlin.math.min

class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintPlayed = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintUnplayed = Paint(Paint.ANTI_ALIAS_FLAG)

    private val amplitudes = mutableListOf<Float>()

    private var playbackPosition = 0f

    private var barWidth = 3f
    private var barSpacing = 2f

    // Stan
    private var isRecordingMode = false

    init {
        paintPlayed.color = ContextCompat.getColor(context, R.color._orange_grim)
        paintPlayed.strokeWidth = barWidth
        paintPlayed.strokeCap = Paint.Cap.ROUND

        paintUnplayed.color = ContextCompat.getColor(context, R.color._gray_mid)
        paintUnplayed.strokeWidth = barWidth
        paintUnplayed.strokeCap = Paint.Cap.ROUND
    }

    fun addAmplitude(amplitude: Int) {
        val normalizedAmplitude = amplitude.toFloat() / 32767f // Normalize to 0-1
        amplitudes.add(normalizedAmplitude)

        if (!isRecordingMode) {
            invalidate()
        }
    }

    fun clear() {
        amplitudes.clear()
        playbackPosition = 0f
        invalidate()
    }

    fun setPlaybackPosition(position: Float) {
        playbackPosition = position.coerceIn(0f, 1f)
        invalidate()
    }

    fun setRecordingMode(recording: Boolean) {
        isRecordingMode = recording
        if (!recording && amplitudes.isNotEmpty()) {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (amplitudes.isEmpty()) return

        val centerY = height / 2f
        val availableWidth = width.toFloat()

        val totalBarWidth = barWidth + barSpacing
        val maxBars = (availableWidth / totalBarWidth).toInt()

        val sampledAmplitudes = if (amplitudes.size > maxBars) {
            sampleAmplitudes(amplitudes, maxBars)
        }
        else if (amplitudes.size < maxBars) {
            interpolateAmplitudes(amplitudes, maxBars)
        }
        else {
            amplitudes
        }

        val playedBarsCount = (sampledAmplitudes.size * playbackPosition).toInt()

        for (i in sampledAmplitudes.indices) {
            val x = i * totalBarWidth + barWidth / 2
            val amplitude = sampledAmplitudes[i]

            val barHeight = amplitude * (height / 2f) * 7.2f

            val paint = if (i < playedBarsCount) paintPlayed else paintUnplayed

            canvas.drawLine(
                x,
                centerY - barHeight,
                x,
                centerY + barHeight,
                paint
            )
        }
    }

    /**
     * Amplitudes sampling when there is too much of these (downsampling)
     */
    private fun sampleAmplitudes(source: List<Float>, targetCount: Int): List<Float> {
        if (source.size <= targetCount) return source

        val result = mutableListOf<Float>()
        val samplingRatio = source.size.toFloat() / targetCount

        for (i in 0 until targetCount) {
            val startIdx = (i * samplingRatio).toInt()
            val endIdx = min(((i + 1) * samplingRatio).toInt(), source.size)

            // Weź średnią z grupy sampli
            val average = source.subList(startIdx, endIdx).average().toFloat()
            result.add(average)
        }

        return result
    }

    /**
     * Interpolate amplitudes where there is too many of these (upsampling)
     */
    private fun interpolateAmplitudes(source: List<Float>, targetCount: Int): List<Float> {
        if (source.size >= targetCount) return source
        if (source.isEmpty()) return List(targetCount) { 0f }

        val result = mutableListOf<Float>()
        val ratio = (source.size - 1).toFloat() / (targetCount - 1)

        for (i in 0 until targetCount) {
            val sourceIndex = i * ratio
            val lowerIndex = sourceIndex.toInt()
            val upperIndex = min(lowerIndex + 1, source.size - 1)
            val fraction = sourceIndex - lowerIndex

            val interpolated = source[lowerIndex] * (1 - fraction) + source[upperIndex] * fraction
            result.add(interpolated)
        }

        return result
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        barWidth = 3f
        barSpacing = 2f
        paintPlayed.strokeWidth = barWidth
        paintUnplayed.strokeWidth = barWidth
    }
}