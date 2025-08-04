package pl.hexmind.fastnote.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.databinding.ActivitySettingsBinding
import pl.hexmind.fastnote.features.main.CoreActivity
import pl.hexmind.fastnote.features.settings.AppSettingsStorage

/**
 * Activity providing personalized settings for the memory app
 */
class SettingsActivity : CoreActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var appSettingsStorage: AppSettingsStorage // TODO: ? moze przeniesc do CoreActivit
    private var selectedAudioUri: Uri? = null

    // Activity result launcher for audio file selection
    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedAudioFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AppSettingsStorage
        appSettingsStorage = AppSettingsStorage(this)

        setupUI()
        loadSavedSettings()
    }

    /**
     * Initialize UI components and click listeners
     */
    private fun setupUI() {
        // Audio file selection
        binding.btnSelectAudio.setOnClickListener {
            selectAudioFile()
        }

        // Save settings button
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * Load previously saved settings using AppSettingsStorage
     */
    private fun loadSavedSettings() {
        // Load app name
        val appName = appSettingsStorage.getYourName()
        binding.etYourName.setText(appName)

        // Load audio file info with error handling
        val audioUri = appSettingsStorage.getWelcomeAudioUri()
        if (audioUri != null) {
            try {
                // Test if URI is still accessible
                if (appSettingsStorage.isUriAccessible(audioUri)) {
                    selectedAudioUri = audioUri
                    updateAudioFileDisplay(audioUri)
                } else {
                    // URI is no longer accessible, clear it
                    clearAudioSelection()
                    showAudioErrorMessage(getString(R.string.audio_file_no_longer_accessible))
                }
            } catch (e: Exception) {
                clearAudioSelection()
                showAudioErrorMessage(getString(R.string.audio_file_error))
            }
        }

        // Load phase names with default fallbacks
        binding.etPhase1.setText(
            appSettingsStorage.getPhaseGatheringAlias(getString(R.string.common_phase1_default_name))
        )
        binding.etPhase2.setText(
            appSettingsStorage.getPhaseChoosingAlias(getString(R.string.common_phase2_default_name))
        )
        binding.etPhase3.setText(
            appSettingsStorage.getPhaseSilentAlias(getString(R.string.common_phase3_default_name))
        )
    }

    /**
     * Clear audio selection and update UI
     */
    private fun clearAudioSelection() {
        selectedAudioUri = null
        binding.tvSelectedFile.text = getString(R.string.no_audio_file_selected)
        appSettingsStorage.clearWelcomeAudio()
    }

    /**
     * Show error message for audio file issues
     */
    private fun showAudioErrorMessage(message: String) {
        binding.tvSelectedFile.text = message
        binding.tvSelectedFile.setTextColor(getColor(android.R.color.holo_red_dark))
    }

    /**
     * Open file picker for audio selection using document picker for persistent access
     */
    private fun selectAudioFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            // Request persistent access to the file
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            audioPickerLauncher.launch(intent)
        } catch (e: Exception) {
            showShortToast(R.string.cannot_open_file_picker)
        }
    }

    /**
     * Handle selected audio file and request persistent permissions
     */
    private fun handleSelectedAudioFile(uri: Uri) {
        try {
            // Request persistent permission to access the file
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            selectedAudioUri = uri
            updateAudioFileDisplay(uri)

            val fileName = getSimpleFileName(uri)
            showShortToast(R.string.file_selected, fileName)

            // Reset text color to default - TODO: Use custom color from colors.xml
            binding.tvSelectedFile.setTextColor(getColor(R.color.text_secondary))

        } catch (e: SecurityException) {
            // Fallback: still save the URI but warn about potential access issues
            selectedAudioUri = uri
            updateAudioFileDisplay(uri)
            showShortToast(R.string.file_selected_temp_access)
        } catch (e: Exception) {
            showAudioErrorMessage(getString(R.string.audio_file_error))
            showShortToast(R.string.error_selecting_file)
        }
    }

    /**
     * Update audio file display with filename, extension and folder
     */
    private fun updateAudioFileDisplay(uri: Uri) {
        val fileInfo = getDetailedFileInfo(uri)
        binding.tvSelectedFile.text = fileInfo
    }

    /**
     * Get detailed file information: filename.extension in /folder/path
     */
    private fun getDetailedFileInfo(uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                try {
                    // Try to get display name from content resolver
                    val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            cursor.getString(nameIndex)
                        } else null
                    }

                    // Try to get folder path for Documents provider
                    val folderPath = if (DocumentsContract.isDocumentUri(this, uri)) {
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
                    getString(R.string.unknown_audio_file)
                }
            }
            "file" -> {
                val path = uri.path ?: ""
                val fileName = path.substringAfterLast("/")
                val folderPath = path.substringBeforeLast("/")
                if (folderPath.isNotEmpty() && fileName.isNotEmpty()) {
                    "$fileName\n${getString(R.string.in_folder)}: $folderPath"
                } else {
                    fileName.ifEmpty { getString(R.string.unknown_file) }
                }
            }
            else -> getString(R.string.unknown_audio_file)
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

            contentResolver.query(
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
    private fun getSimpleFileName(uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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

    /**
     * Save all settings using AppSettingsStorage
     */
    private fun saveSettings() {
        // Save app name
        val appName = binding.etYourName.text?.toString()?.trim() ?: ""
        appSettingsStorage.setYourName(appName)

        // Save audio file path only if accessible
        selectedAudioUri?.let { uri ->
            if (appSettingsStorage.isUriAccessible(uri)) {
                appSettingsStorage.setWelcomeAudioUri(uri)
            } else {
                appSettingsStorage.clearWelcomeAudio()
                showAudioErrorMessage(getString(R.string.audio_file_no_longer_accessible))
            }
        }

        // Save phase names with defaults
        val phase1 = binding.etPhase1.text?.toString()?.trim()
            ?: getString(R.string.common_phase1_default_name)
        val phase2 = binding.etPhase2.text?.toString()?.trim()
            ?: getString(R.string.common_phase2_default_name)
        val phase3 = binding.etPhase3.text?.toString()?.trim()
            ?: getString(R.string.common_phase3_default_name)

        appSettingsStorage.setPhaseNames(phase1, phase2, phase3)

        showShortToast(R.string.common_info_changes_saved)
        finish()
    }
}