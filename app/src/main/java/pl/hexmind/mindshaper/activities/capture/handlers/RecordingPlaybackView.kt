package pl.hexmind.mindshaper.activities.capture.handlers

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import kotlinx.coroutines.*
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.audio.AudioAmplitudeExtractor
import java.io.File

class RecordingPlaybackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecordingCaptureView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RecordingPlaybackView"
    }

    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var isPlaying = false

    private val playbackHandler = Handler(Looper.getMainLooper())

    // Coroutine scope for background operations/runnables
    private var viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        if (background == null) {
            setBackgroundResource(R.drawable.shape_button)
        }

        setupPlaybackListeners()
    }

    private val playbackUpdateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && player != null) {
                player?.let {
                    if (it.isPlaying) {
                        val currentPosition = it.currentPosition.toLong()
                        val duration = it.duration.toLong()
                        updateTimer(currentPosition, duration)
                        updatePlaybackPosition(currentPosition.toFloat() / duration)
                        playbackHandler.postDelayed(this, 100L)
                    }
                }
            }
        }
    }

    private fun setupPlaybackListeners() {
        btnRecordNew.visibility = GONE

        btnRecordStopPlay.setOnClickListener {
            if (isPlaying) stopPlaying() else startPlaying()
        }

        btnSkipBackward.setOnClickListener { skipBackward() }
        btnSkipForward.setOnClickListener { skipForward() }
    }

    fun loadAudio(file: File) {
        cleanup(cancelCoroutines = false) // ! false = disabled coroutines canceling for drawing visualisations with amplitudes
        audioFile = file

        clearVisualization()
        updateStatus("Ładowanie nagrania...", R.color.text_secondary)

        // Load real amplitudes in background
        viewScope.launch {
            try {
                val amplitudes = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Extracting amplitudes from: ${file.absolutePath}")
                    AudioAmplitudeExtractor.extractAmplitudes(file)
                }

                // Ustaw amplitudy w UI
                clearVisualization()
                amplitudes.forEach { amplitude ->
                    updateVisualization((amplitude * 32767).toInt())
                }
                showVisualization(show = true, isRecording = false)

                try {
                    val tempPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                    }
                    val duration = tempPlayer.duration.toLong()
                    tempPlayer.release()

                    updateTimer(0L, duration)
                }
                catch (e: Exception) {
                    Log.e(TAG, "Error preparing MediaPlayer", e)
                }

                updateButtons(isRecording = false, isPlaying = false, hasRecording = true)
                updateStatus("Gotowe do odtwarzania", R.color.validation_success)
            }
            catch (e: Exception) {
                Log.e(TAG, "Error loading amplitudes", e)
                updateStatus("Błąd przy wczytywaniu nagrania", R.color.validation_error)
            }
        }
    }

    private fun startPlaying() {
        val file = audioFile ?: return

        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlaying() }
            }

            isPlaying = true
            updateButtons(isRecording = false, isPlaying = true, hasRecording = true)
            playbackHandler.post(playbackUpdateRunnable)
        }
        catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
            updateStatus("Błąd odtwarzania", R.color.validation_error)
        }
    }

    private fun stopPlaying() {
        playbackHandler.removeCallbacks(playbackUpdateRunnable)

        player?.release()
        player = null
        isPlaying = false

        updatePlaybackPosition(0f)
        updateButtons(isRecording = false, isPlaying = false, hasRecording = true)

        // Reset timer
        audioFile?.let { file ->
            try {
                val tempPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                }
                val duration = tempPlayer.duration.toLong()
                tempPlayer.release()
                updateTimer(0L, duration)
            }
            catch (e: Exception) {
                Log.e(TAG, "Error resetting timer", e)
            }
        }
    }

    private fun skipBackward() {
        player?.let {
            val currentPosition = it.currentPosition
            val newPosition = (currentPosition - 5000).coerceAtLeast(0)
            it.seekTo(newPosition)
            Log.d(TAG, "Skipped backward to ${newPosition}ms")
        }
    }

    private fun skipForward() {
        player?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition = (currentPosition + 5000).coerceAtMost(duration)
            it.seekTo(newPosition)
            Log.d(TAG, "Skipped forward to ${newPosition}ms")
        }
    }

    fun cleanup(cancelCoroutines : Boolean = true) {
        stopPlaying()

        // Cancel all pending coroutines
        if(cancelCoroutines)
            viewScope.cancel()

        audioFile?.delete()
        audioFile = null

        clearVisualization()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanup()
    }
}