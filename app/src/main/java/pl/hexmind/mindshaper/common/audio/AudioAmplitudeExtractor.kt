package pl.hexmind.mindshaper.common.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * Extract audio amplitudes from audio files for waveform visualization
 */
class AudioAmplitudeExtractor {

    companion object {
        private const val TAG = "AudioAmplitudeExtractor"

        /**
         * Extract amplitudes from audio file using MediaExtractor and MediaCodec
         *
         * @param audioFile Audio file (m4a, mp3, etc.)
         * @param targetSamples Number of amplitude samples to return (default 100)
         * @return List of normalized amplitudes (0.0 - 1.0)
         */
        fun extractAmplitudes(audioFile: File, targetSamples: Int = 100): List<Float> {
            if (!audioFile.exists()) {
                Timber.tag(TAG).e("Audio file does not exist: ${audioFile.absolutePath}")
                return emptyList()
            }

            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            val amplitudes = mutableListOf<Float>()

            try {
                extractor.setDataSource(audioFile.absolutePath)

                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }

                if (audioTrackIndex == -1 || audioFormat == null) {
                    Log.e(TAG, "No audio track found")
                    return emptyList()
                }

                extractor.selectTrack(audioTrackIndex)

                val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(audioFormat, null, null, 0)
                codec.start()

                val bufferInfo = MediaCodec.BufferInfo()
                var isEOS = false
                val rawAmplitudes = mutableListOf<Float>()

                // Decode audio and collect amplitudes
                while (!isEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()

                        val sampleSize = extractor.readSampleData(inputBuffer ?: ByteBuffer.allocate(0), 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }

                    val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)

                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val amplitude = calculateRMSAmplitude(outputBuffer, bufferInfo.size)
                            rawAmplitudes.add(amplitude)
                        }

                        codec.releaseOutputBuffer(outputBufferIndex, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            isEOS = true
                        }
                    }
                }

                amplitudes.addAll(resampleAmplitudes(rawAmplitudes, targetSamples))

                Timber.tag(TAG)
                    .d("Extracted ${amplitudes.size} amplitude samples from ${rawAmplitudes.size} raw samples")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error extracting amplitudes")
                return emptyList()
            } finally {
                try {
                    codec?.stop()
                    codec?.release()
                    extractor.release()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error releasing resources")
                }
            }

            return amplitudes
        }

        /**
         * Calculate RMS (Root Mean Square) amplitude from audio buffer
         */
        private fun calculateRMSAmplitude(buffer: ByteBuffer, size: Int): Float {
            buffer.position(0)

            var sum = 0.0
            var count = 0

            // Assume 16-bit PCM audio
            while (buffer.remaining() >= 2 && count < size / 2) {
                val sample = buffer.short.toFloat() / Short.MAX_VALUE
                sum += sample * sample
                count++
            }

            return if (count > 0) {
                sqrt(sum / count).toFloat()
            } else {
                0f
            }
        }

        /**
         * Resample amplitudes to target sample count
         */
        private fun resampleAmplitudes(source: List<Float>, targetCount: Int): List<Float> {
            if (source.isEmpty()) return emptyList()
            if (source.size <= targetCount) return source

            val result = mutableListOf<Float>()
            val ratio = source.size.toFloat() / targetCount

            for (i in 0 until targetCount) {
                val startIdx = (i * ratio).toInt()
                val endIdx = ((i + 1) * ratio).toInt().coerceAtMost(source.size)

                // Use max amplitude for better visualization
                val maxAmplitude = source.subList(startIdx, endIdx).maxOrNull() ?: 0f
                result.add(maxAmplitude)
            }

            return result
        }
    }
}