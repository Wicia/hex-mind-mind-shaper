package pl.hexmind.mindshaper.activities.capture.handlers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.audio.AudioAmplitudeExtractor
import pl.hexmind.mindshaper.common.audio.AudioVisualizerView
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import timber.log.Timber
import java.io.File
import androidx.core.content.withStyledAttributes

/**
 * Universal audio recording and playback view
 * Supports two modes:
 * - RECORD_PLAYBACK: Full recording + playback functionality
 * - PLAYBACK_ONLY: Playback only (for viewing saved recordings)
 */
class AudioRecordingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Mode {
        RECORD_PLAYBACK,  // Recording + Playback (Capture screen)
        PLAYBACK_ONLY     // Playback only (Details screen)
    }

    enum class State {
        RECORDING,
        PLAYING,
        WAITING
    }

    companion object {
        private const val TAG = "AudioRecordingView"
        private const val TIMER_UPDATE_INTERVAL_MS = 100L
        private const val AMPLITUDE_UPDATE_INTERVAL_MS = 50L
        private const val SKIP_DURATION_MS = 5000L
    }

    /**
     * Callback interface for recording events
     */
    interface RecordingCallback {
        fun onRecordingStarted()
        fun onRecordingStopped(file: File, durationMs: Long)
        fun onRecordingError(error: String)
        fun onPlaybackStarted()
        fun onPlaybackStopped()
        fun onPermissionRequired()
    }

    // UI Components
    val btnRecordNew: MaterialButton
    val btnRecordStopPlay: MaterialButton
    val btnSkipBackward: MaterialButton
    val btnSkipForward: MaterialButton
    private val tvInfo: TextView
    private val tvTimerSecLasts: TextView
    private val tvTimerSecRemains: TextView
    val audioVisualizer: AudioVisualizerView

    // Recording state
    private var recorder: MediaRecorder? = null
    private var tempAudioFile: File? = null
    private var recordingStartTime = 0L
    private var currentRecordingDuration = 0L

    // Playback state
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private val playbackHandler = Handler(Looper.getMainLooper())
    private var viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Handlers for recording
    private val recordingTimerHandler = Handler(Looper.getMainLooper())
    private val amplitudeHandler = Handler(Looper.getMainLooper())

    private var mode: Mode = Mode.RECORD_PLAYBACK
    private var state: State = State.WAITING
    private var callback: RecordingCallback? = null

    init {
        inflate(context, R.layout.capture_view_recording, this)
        orientation = VERTICAL

        // Initialize UI components
        btnRecordNew = findViewById(R.id.btn_record_new)
        btnRecordStopPlay = findViewById(R.id.btn_record_stop_play)
        btnSkipBackward = findViewById(R.id.btn_skip_backward)
        btnSkipForward = findViewById(R.id.btn_skip_forward)
        tvInfo = findViewById(R.id.tv_info)
        tvTimerSecLasts = findViewById(R.id.tv_timer_sec_lasts)
        tvTimerSecRemains = findViewById(R.id.tv_timer_sec_remains)
        audioVisualizer = findViewById(R.id.audio_visualizer)

        // Read mode from XML attributes if provided
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.AudioRecordingView) {
                val modeValue = getInt(R.styleable.AudioRecordingView_recordingMode, 0)
                mode = if (modeValue == 1) Mode.PLAYBACK_ONLY else Mode.RECORD_PLAYBACK
                setupUIForMode()
            }
        }

        setupListeners()
        updateUIForState()
    }

    private fun setupUIForMode() {
        when (mode) {
            Mode.RECORD_PLAYBACK -> {
                btnRecordNew.visibility = VISIBLE
            }
            Mode.PLAYBACK_ONLY -> {
                btnRecordNew.visibility = GONE
            }
        }
    }

    private fun updateUIForState() {
        val isRecording = state == State.RECORDING
        val isPlaying = state == State.PLAYING
        val hasRecording = audioFile?.exists() == true

        updateButtons(isRecording, isPlaying, hasRecording)
        showSkipButtons(isPlaying)
    }

    fun updateButtons(isRecording: Boolean, isPlaying: Boolean, hasRecording: Boolean) {
        when {
            isRecording -> {
                btnRecordNew.isEnabled = false

                btnRecordStopPlay.isEnabled = true
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_recording_stop)
            }
            isPlaying -> {
                btnRecordNew.isEnabled = false

                btnRecordStopPlay.isEnabled = true
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_recording_stop)
            }
            hasRecording -> {
                btnRecordNew.isEnabled = true

                btnRecordStopPlay.isEnabled = true
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_recording_play)
            }
            else -> {
                btnRecordNew.isEnabled = true

                btnRecordStopPlay.isEnabled = false
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_recording_play)
            }
        }
    }

    // Runnables
    private val recordingTimerRunnable = object : Runnable {
        override fun run() {
            if (state == State.RECORDING) {
                currentRecordingDuration = System.currentTimeMillis() - recordingStartTime

                if (currentRecordingDuration >= ThoughtValidator.VOICE_RECORDING_MAX_DURATION_MS) {
                    stopRecording()
                    showStatus(
                        context.getString(R.string.capture_voice_status_max_duration_reached),
                        R.color.validation_error
                    )
                } else {
                    updateTimer(currentRecordingDuration, ThoughtValidator.VOICE_RECORDING_MAX_DURATION_MS)
                    recordingTimerHandler.postDelayed(this, TIMER_UPDATE_INTERVAL_MS)
                }
            }
        }
    }

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (state == State.RECORDING && recorder != null) {
                val amplitude = recorder?.maxAmplitude ?: 0
                updateVisualization(amplitude)
                amplitudeHandler.postDelayed(this, AMPLITUDE_UPDATE_INTERVAL_MS)
            }
        }
    }

    private val playbackUpdateRunnable = object : Runnable {
        override fun run() {
            if (state == State.PLAYING && player != null) {
                player?.let {
                    if (it.isPlaying) {
                        val currentPosition = it.currentPosition.toLong()
                        val duration = it.duration.toLong()
                        updateTimer(currentPosition, duration)
                        updatePlaybackPosition(currentPosition.toFloat() / duration)
                        playbackHandler.postDelayed(this, TIMER_UPDATE_INTERVAL_MS)
                    }
                }
            }
        }
    }

    fun setRecordingCallback(callback: RecordingCallback?) {
        this.callback = callback
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        setupUIForMode()
    }

    private fun setupListeners() {
        btnRecordNew.setOnClickListener {
            if (mode == Mode.RECORD_PLAYBACK) {
                startRecording()
            }
        }

        btnRecordStopPlay.setOnClickListener {
            when (state) {
                State.RECORDING -> stopRecording()
                State.PLAYING -> stopPlaying()
                State.WAITING -> startPlaying()
            }
        }

        btnSkipBackward.setOnClickListener { skipBackward() }
        btnSkipForward.setOnClickListener { skipForward() }
    }

    // ===========================================
    //      Recording Methods
    // ===========================================

    private fun startRecording() {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callback?.onPermissionRequired()
            return
        }

        deleteCurrentRecording()
        showStatus(
            context.getString(R.string.capture_voice_recording_tooltip),
            R.color.text_secondary
        )

        // Create temp file
        tempAudioFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(tempAudioFile!!.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                prepare()
                start()
            }

            state = State.RECORDING
            recordingStartTime = System.currentTimeMillis()
            currentRecordingDuration = 0L

            clearVisualization()
            showVisualization(show = true, isRecording = true)
            updateUIForState()

            recordingTimerHandler.post(recordingTimerRunnable)
            amplitudeHandler.post(amplitudeRunnable)

            callback?.onRecordingStarted()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting recording")
            showStatus(
                context.getString(R.string.capture_voice_error_recording),
                R.color.validation_error
            )
            cleanupRecorder()
            callback?.onRecordingError(e.message ?: "Unknown error")
        }
    }

    private fun stopRecording() {
        recordingTimerHandler.removeCallbacks(recordingTimerRunnable)
        amplitudeHandler.removeCallbacks(amplitudeRunnable)

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error stopping recording")
        } finally {
            recorder = null
        }

        state = State.WAITING

        if (tempAudioFile?.exists() == true && (tempAudioFile?.length() ?: 0) > 0) {
            showStatus(
                context.getString(R.string.capture_voice_status_recording_saved),
                R.color.validation_success
            )

            audioFile = tempAudioFile
            showVisualization(show = true, isRecording = false)

            val duration = currentRecordingDuration
            callback?.onRecordingStopped(audioFile!!, duration)

            // Load visualization for playback
            audioFile?.let { loadAudioVisualization(it) }

        } else {
            showStatus(
                context.getString(R.string.capture_voice_error_recording),
                R.color.validation_error
            )
            tempAudioFile?.delete()
            tempAudioFile = null
            callback?.onRecordingError("Empty recording file")
        }

        updateUIForState()
    }

    private fun cleanupRecorder() {
        recorder?.release()
        recorder = null
        state = State.WAITING
    }

    fun deleteCurrentRecording() {
        stopPlaying()

        tempAudioFile?.delete()
        tempAudioFile = null
        audioFile = null

        clearVisualization()
        updateUIForState()
        showVisualization(false)
    }
    
    fun getCurrentRecording(): Recording = Recording(audioFile, currentRecordingDuration)

    // ===========================================
    //      Playback Methods
    // ===========================================

    fun loadAudioForPlayback(file: File) {
        cleanupResources(cancelCoroutines = false)
        audioFile = file

        clearVisualization()
        showStatus("Ładowanie nagrania...", R.color.text_secondary)

        loadAudioVisualization(file)
    }

    private fun loadAudioVisualization(file: File) {
        viewScope.launch {
            try {
                val amplitudes = withContext(Dispatchers.IO) {
                    Timber.tag(TAG).d("Extracting amplitudes from: ${file.absolutePath}")
                    AudioAmplitudeExtractor.extractAmplitudes(file)
                }

                clearVisualization()
                amplitudes.forEach { amplitude ->
                    audioVisualizer.addNormalizedAmplitude(amplitude)
                }
                showVisualization(show = true, isRecording = false)

                try {
                    val tempPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                    }
                    val durationMs = tempPlayer.duration.toLong()
                    tempPlayer.release()

                    updateTimer(0L, durationMs)
                }
                catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error preparing MediaPlayer")
                }

                updateUIForState()
            }
            catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading amplitudes")
                showStatus("Błąd przy wczytywaniu nagrania", R.color.validation_error)
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

            state = State.PLAYING
            updateUIForState()
            playbackHandler.post(playbackUpdateRunnable)
            callback?.onPlaybackStarted()
        }
        catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting playback")
            showStatus("Błąd odtwarzania", R.color.validation_error)
        }
    }

    private fun stopPlaying() {
        playbackHandler.removeCallbacks(playbackUpdateRunnable)

        player?.release()
        player = null
        state = State.WAITING

        updatePlaybackPosition(0f)
        updateUIForState()

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
                Timber.tag(TAG).e(e, "Error resetting timer")
            }
        }

        callback?.onPlaybackStopped()
    }

    private fun skipBackward() {
        player?.let {
            val currentPosition = it.currentPosition
            val newPosition = (currentPosition - SKIP_DURATION_MS).coerceAtLeast(0)
            it.seekTo(newPosition.toInt())
            Timber.tag(TAG).d("Skipped backward to ${newPosition}ms")
        }
    }

    private fun skipForward() {
        player?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition = (currentPosition + SKIP_DURATION_MS).coerceAtMost(duration.toLong())
            it.seekTo(newPosition.toInt())
            Timber.tag(TAG).d("Skipped forward to ${newPosition}ms")
        }
    }

    // ===========================================
    //      Public UI Update Methods
    // ===========================================

    fun showStatus(text: String, colorRes: Int) {
        audioVisualizer.visibility = GONE
        tvInfo.visibility = VISIBLE
        tvInfo.text = text
        tvInfo.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    fun updateTimer(currentMs: Long, totalMs: Long) {
        when (state) {
            State.PLAYING -> {
                val currentSeconds = currentMs / 1000
                val totalSeconds = totalMs / 1000

                val currentMinutes = currentSeconds / 60
                val currentSecs = currentSeconds % 60

                val remainingSeconds = totalSeconds - currentSeconds
                val remainingMinutes = remainingSeconds / 60
                val remainingSecs = remainingSeconds % 60

                tvTimerSecLasts.text = String.format("%02d:%02d |", currentMinutes, currentSecs)
                tvTimerSecRemains.text = String.format("| %02d:%02d", remainingMinutes, remainingSecs)
            }

            State.RECORDING -> {
                val currentSeconds = currentMs / 1000
                val maxSeconds = ThoughtValidator.VOICE_RECORDING_MAX_DURATION_MS / 1000

                val currentMinutes = currentSeconds / 60
                val currentSecs = currentSeconds % 60

                val remainingSeconds = maxSeconds - currentSeconds
                val remainingMinutes = remainingSeconds / 60
                val remainingSecs = remainingSeconds % 60

                tvTimerSecLasts.text = String.format("%02d:%02d |", currentMinutes, currentSecs)
                tvTimerSecRemains.text = String.format("| %02d:%02d", remainingMinutes, remainingSecs)
            }

            State.WAITING -> {
                val totalSeconds = totalMs / 1000
                val totalMinutes = totalSeconds / 60
                val totalSecs = totalSeconds % 60

                tvTimerSecLasts.text = "00:00 |"
                tvTimerSecRemains.text = String.format("| %02d:%02d", totalMinutes, totalSecs)
            }
        }
    }

    fun showSkipButtons(show: Boolean) {
        btnSkipBackward.visibility = if (show) VISIBLE else INVISIBLE
        btnSkipForward.visibility = if (show) VISIBLE else INVISIBLE
    }

    fun showVisualization(show: Boolean, isRecording: Boolean = false) {
        when {
            isRecording -> {
                tvInfo.visibility = VISIBLE
                audioVisualizer.visibility = GONE
                audioVisualizer.setRecordingMode(true)
            }
            show -> {
                tvInfo.visibility = GONE
                audioVisualizer.visibility = VISIBLE
                audioVisualizer.setRecordingMode(false)
            }
            else -> {
                tvInfo.visibility = VISIBLE
                audioVisualizer.visibility = GONE
                audioVisualizer.setRecordingMode(false)
            }
        }
    }

    fun updateVisualization(amplitude: Int) {
        audioVisualizer.addRawAmplitude(amplitude)
    }

    fun clearVisualization() {
        audioVisualizer.clear()
    }

    fun updatePlaybackPosition(position: Float) {
        audioVisualizer.setPlaybackPosition(position)
    }

    // ===========================================
    //      Lifecycle Methods
    // ===========================================

    fun cleanupResources(cancelCoroutines: Boolean = true) {
        // Stop any ongoing operations
        if (state == State.RECORDING) {
            stopRecording()
        }
        if (state == State.PLAYING) {
            stopPlaying()
        }

        // Remove all callbacks
        recordingTimerHandler.removeCallbacks(recordingTimerRunnable)
        amplitudeHandler.removeCallbacks(amplitudeRunnable)
        playbackHandler.removeCallbacks(playbackUpdateRunnable)

        // Release resources
        cleanupRecorder()
        player?.release()
        player = null

        if (cancelCoroutines) {
            viewScope.cancel()
            viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        // Clean up files
        tempAudioFile?.delete()
        tempAudioFile = null
        audioFile = null

        clearVisualization()
        state = State.WAITING
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanupResources()
    }
}

data class Recording (
    val file : File? = null,
    val duration : Long? = 0L
){
    fun fileExists() : Boolean{
        return file != null
    }
}