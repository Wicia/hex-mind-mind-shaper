package pl.hexmind.mindshaper.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.models.ThoughtMetadataUpdate
import pl.hexmind.mindshaper.database.repositories.ThoughtsRepository
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.mappers.ThoughtsMapper
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import timber.log.Timber
import java.time.Instant

@Singleton
class ThoughtsService @Inject constructor(
    private val repository: ThoughtsRepository,
    @ApplicationContext
    private val context : Context
) {

    /**
     * Get all thoughts (reactive/LiveData)
     */
    fun getAllThoughts(): LiveData<List<ThoughtDTO>> {
        val result = repository.getAllThoughtsLive()
        return entityLiveDataToDtoLiveData(result)
    }

    fun getThoughtByIdLive(id: Int): LiveData<ThoughtDTO?> {
        val entityLiveData = repository.getThoughtByIdLive(id.toLong())
        return entityLiveData.map { entityThought ->
            entityThought?.let { ThoughtsMapper.INSTANCE.entityToDTO(it) }
        }
    }

    private fun entityLiveDataToDtoLiveData(entities: LiveData<List<ThoughtEntity>>): LiveData<List<ThoughtDTO>> {
        return entities.map { list ->
            ThoughtsMapper.INSTANCE.entityListToDtoList(list)
        }
    }

    suspend fun addThought(thought: ThoughtDTO) : Long {
        val entity = ThoughtsMapper.INSTANCE.dtoToEntity(thought)
        return repository.insertThought(entity)
    }

    suspend fun deleteThoughtById(id: Int) {
        repository.deleteThoughtById(id)
    }

    // === Sophisticated methods for updating specific part of thought (rich text, recording...)

    suspend fun updateThoughtMetadata(thought: ThoughtDTO) {
        val metadata = ThoughtMetadataUpdate(
            id = thought.id!!,
            domainId = thought.domainId,
            thread = thought.thread,
            soulMate = thought.soulMate,
            project = thought.project,
            value = thought.value,
            updatedAt = Instant.now().toEpochMilli()
        )
        repository.updateThoughtMetadata(metadata)
    }

    suspend fun updateThoughtRichText(thoughtId: Int, richText: String?) {
        repository.updateRichText(thoughtId, richText)
    }

    suspend fun updateThoughtRecording(thoughtId : Long, audioFile: File, duration : Long) {
        repository.saveAudioFromFile(
            thoughtId = thoughtId,
            audioFile = audioFile,
            durationMs = duration
        )
    }

    suspend fun getAudioData(thoughtId: Int): ByteArray? {
        return repository.getAudioData(thoughtId.toLong())
    }

    suspend fun deleteThoughtAudio(thoughtId: Int) {
        repository.deleteAudio(thoughtId.toLong())
    }

// ==================== PHOTOS ====================

    suspend fun updateThoughtPhoto(thoughtId: Long, photoFile: File) {
        val compressed = compressPhoto(photoFile)
        repository.savePhotoFromFile(thoughtId, compressed)
    }

    suspend fun getPhotoData(thoughtId: Int): ByteArray? {
        return repository.getPhotoData(thoughtId.toLong())
    }

    suspend fun deleteThoughtPhoto(thoughtId: Int) {
        repository.deletePhoto(thoughtId.toLong())
    }

    /**
     * Load photo from file with EXIF rotation applied (for preview)
     * Returns photo as byte array ready for display
     */
    fun loadPhotoForPreview(photoFile: File, maxSize: Int = 800): ByteArray {
        // Decode file to bitmap
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

        // Apply EXIF rotation
        val rotatedBitmap = rotateBitmapIfNeeded(bitmap, photoFile)

        // Scale to max size for preview (smaller = faster load)
        val scaledBitmap = if (rotatedBitmap.width > maxSize || rotatedBitmap.height > maxSize) {
            val scale = maxSize.toFloat() / max(rotatedBitmap.width, rotatedBitmap.height)
            Bitmap.createScaledBitmap(
                rotatedBitmap,
                (rotatedBitmap.width * scale).toInt(),
                (rotatedBitmap.height * scale).toInt(),
                true
            )
        } else rotatedBitmap

        // Convert to bytes
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val photoBytes = outputStream.toByteArray()

        // Cleanup
        rotatedBitmap.recycle()
        if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()

        return photoBytes
    }

    /**
     * Compress photo to max 5MB and scale to 1920px
     */
    private fun compressPhoto(sourceFile: File, maxSizeKB: Int = 5000): File {
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)

        // Apply EXIF rotation FIRST
        val rotatedBitmap = rotateBitmapIfNeeded(bitmap, sourceFile)

        // Scale to max 1920px
        val maxDimension = 1920
        val scaledBitmap = if (rotatedBitmap.width > maxDimension || rotatedBitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / max(rotatedBitmap.width, rotatedBitmap.height)
            Bitmap.createScaledBitmap(
                rotatedBitmap,
                (rotatedBitmap.width * scale).toInt(),
                (rotatedBitmap.height * scale).toInt(),
                true
            )
        } else rotatedBitmap

        // Compress to max size
        val compressed = File.createTempFile("compressed_", ".jpg", context.cacheDir)
        var quality = 90
        do {
            compressed.delete()
            FileOutputStream(compressed).use {
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
            }
            quality -= 10
        } while (compressed.length() > maxSizeKB * 1024 && quality > 10)

        rotatedBitmap.recycle()
        if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()

        return compressed
    }

    /**
     * Create photo URI for camera using FileProvider
     */
    fun createPhotoUri(): Uri {
        val photoFile = File.createTempFile(
            "PHOTO_${System.currentTimeMillis()}_",
            ".jpg",
            context.cacheDir
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    /**
     * Get file from URI (gallery picker)
     */
    fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            tempFile
        }
        catch (e: Exception) {
            null
        }
    }

    /**
     * Read EXIF orientation and rotate bitmap accordingly
     */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, sourceFile: File): Bitmap {
        return try {
            val exif = ExifInterface(sourceFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        }
        catch (e: Exception) {
            Timber.e(e, "Error reading EXIF orientation")
            bitmap
        }
    }
}