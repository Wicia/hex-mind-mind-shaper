package pl.hexmind.fastnote.services

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.fastnote.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility service for handling all media and permissions features
 */
@Singleton
class MediaStorageService @Inject constructor(
    @ApplicationContext private val context : Context
){
    /**
     * Checks if URI is still accessible by trying to query it
     */
    fun isUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                it.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get detailed file information: filename.extension in /folder/path
     */
    fun getDetailedFileInfo(uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                try {
                    // Try to get display name from content resolver
                    val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            cursor.getString(nameIndex)
                        } else null
                    }

                    // Try to get folder path for Documents provider
                    val folderPath = if (DocumentsContract.isDocumentUri(context, uri)) {
                        try {
                            val docId = DocumentsContract.getDocumentId(uri)
                            when {
                                uri.authority == "com.android.providers.media.documents" -> {
                                    // Media documents - try to get folder from MediaStore
                                    getMediaFolderPath(docId)
                                }
                                uri.authority == "com.android.providers.downloads.documents" -> {
                                    "Downloads"
                                }
                                uri.authority?.contains("externalstorage") == true -> {
                                    // External storage documents
                                    val pathParts = docId.split(":")
                                    if (pathParts.size > 1) {
                                        val relativePath = pathParts[1]
                                        val folder = relativePath.substringBeforeLast("/", "")
                                        if (folder.isNotEmpty()) "/$folder" else ""
                                    } else ""
                                }
                                else -> ""
                            }
                        } catch (e: Exception) {
                            ""
                        }
                    } else ""

                    val fileName = displayName ?: "unknown_file.mp3"
                    if (folderPath.isNotEmpty()) {
                        "$fileName"
                    } else {
                        fileName
                    }

                } catch (e: Exception) {
                    context.getString(R.string.unknown_audio_file)
                }
            }
            "file" -> {
                val path = uri.path ?: ""
                val fileName = path.substringAfterLast("/")
                val folderPath = path.substringBeforeLast("/")
                if (folderPath.isNotEmpty() && fileName.isNotEmpty()) {
                    "$fileName\n${context.getString(R.string.in_folder)}: $folderPath"
                } else {
                    fileName.ifEmpty { context.getString(R.string.unknown_file) }
                }
            }
            else -> context.getString(R.string.unknown_audio_file)
        }
    }

    /**
     * Get folder path from MediaStore for media documents
     */
    private fun getMediaFolderPath(docId: String): String {
        return try {
            val id = docId.split(":")[1]
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            val selection = MediaStore.Audio.Media._ID + "=?"
            val selectionArgs = arrayOf(id)

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val fullPath = cursor.getString(dataIndex)
                    fullPath?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                } else ""
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get simple filename for toast messages
     */
    fun getSimpleFileName(uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            cursor.getString(nameIndex)
                        } else null
                    } ?: "audio_file.mp3"
                } catch (e: Exception) {
                    "audio_file.mp3"
                }
            }
            else -> uri.lastPathSegment ?: "audio_file.mp3"
        }
    }
}