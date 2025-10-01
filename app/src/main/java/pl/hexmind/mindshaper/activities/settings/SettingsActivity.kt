package pl.hexmind.mindshaper.activities.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.main.MainActivity
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.databinding.ActivitySettingsBinding
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.IconsService
import pl.hexmind.mindshaper.services.MediaStorageService
import pl.hexmind.mindshaper.services.dto.DomainDTO
import javax.inject.Inject

/**
 * Activity providing personalized settings for the memory app
 */
@AndroidEntryPoint
class SettingsActivity : CoreActivity() {

    @Inject
    lateinit var domainService : DomainsService

    @Inject
    lateinit var iconsService : IconsService

    @Inject
    lateinit var appSettingsStorage: AppSettingsStorage

    @Inject
    lateinit var mediaStorageService : MediaStorageService

    @Inject
    lateinit var domainValidator: DomainValidator

    private lateinit var binding: ActivitySettingsBinding

    private var selectedAudioUri: Uri? = null

    // Activity result launcher for audio file selection
    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedAudioFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Domains icons
        initDomainButtons()
    }

    private fun initDomainButtons() {
        val gridLayout = findViewById<GridLayout>(R.id.gl_domains)
        lifecycleScope.launch {
            val titles = domainService.getAllDomains()

            try {
                // Create buttons with loaded icons
                titles.forEachIndexed { domainIndex , domainDTO ->
                    val buttonView = layoutInflater.inflate(R.layout.item_settings_domain, gridLayout, false)

                    val ivDomainName = buttonView.findViewById<TextView>(R.id.tv_domain_name)
                    ivDomainName.text = domainDTO.name

                    val ivDomainIcon = buttonView.findViewById<ImageView>(R.id.iv_domain_icon)
                    ivDomainIcon.setImageDrawable(iconsService.getDrawableIcon(domainDTO.iconId))

                    buttonView.setOnClickListener {
                        onDomainButtonClick(domainIndex, domainDTO)
                    }

                    gridLayout.addView(buttonView)
                }

            } catch (e: Exception) {
                // TODO: add UI control + handle error using: R.string.settings_domains_loading_error))
            }
        }
    }

    private fun onDomainButtonClick(domainTileIndex: Int, currentDomainDTO : DomainDTO) {
        showIconPickerDialog(currentDomainDTO) { updatedDTO ->
            updateDomainButton(domainTileIndex, updatedDTO)
            lifecycleScope.launch { domainService.updateDomain(dto = updatedDTO) }
        }
    }

    /**
     * Load previously saved settings using AppSettingsStorage
     */
    private fun loadSavedSettings() {
        // Load app name
        val yourName = appSettingsStorage.getYourName()
        binding.etYourName.setText(yourName)

        // Load audio file info with error handling
        val audioUri = appSettingsStorage.getWelcomeAudioUri()
        if (audioUri != null) {
            try {
                // Test if URI is still accessible
                if (mediaStorageService.isUriAccessible(audioUri)) {
                    selectedAudioUri = audioUri
                    updateAudioFileDisplay(audioUri)
                } else {
                    // URI is no longer accessible, clear it
                    clearAudioSelection()
                    showAudioErrorMessage(getString(R.string.files_audio_error_file_not_accessible))
                }
            } catch (e: Exception) {
                clearAudioSelection()
                showAudioErrorMessage(getString(R.string.file_audio_error_reading_file))
            }
        }
    }

    /**
     * Clear audio selection and update UI
     */
    private fun clearAudioSelection() {
        selectedAudioUri = null
        binding.tvSelectedFile.text = getString(R.string.files_audio_error_no_file_selected)
        appSettingsStorage.clearWelcomeAudio()
    }

    /**
     * Show error message for audio file issues
     */
    private fun showAudioErrorMessage(message: String) {
        binding.tvSelectedFile.text = message
        binding.tvSelectedFile.setTextColor(getColor(R.color.validation_error))
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
            showShortToast(R.string.files_error_cannot_open_file_picker)
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

            val fileName = mediaStorageService.getSimpleFileName(uri)
            showShortToast(R.string.files_info_selected, fileName)

            // Reset text color to default - TODO: Use custom color from colors.xml
            binding.tvSelectedFile.setTextColor(getColor(R.color.text_secondary))

        } catch (e: SecurityException) {
            // Fallback: still save the URI but warn about potential access issues
            selectedAudioUri = uri
            updateAudioFileDisplay(uri)
            showShortToast(R.string.files_info_selected_temp_access)
        } catch (e: Exception) {
            showAudioErrorMessage(getString(R.string.file_audio_error_reading_file))
            showShortToast(R.string.files_error_selecting_file)
        }
    }

    /**
     * Update audio file display with filename, extension and folder
     */
    private fun updateAudioFileDisplay(uri: Uri) {
        val fileInfo = mediaStorageService.getDetailedFileInfo(uri)
        binding.tvSelectedFile.text = fileInfo
    }

    /**
     * Save all settings using AppSettingsStorage
     */
    private fun saveSettings() {
        // Save app name
        val yourName = binding.etYourName.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.settings_your_name_default)
        appSettingsStorage.setYourName(yourName)

        // Save audio file path only if accessible
        selectedAudioUri?.let { uri ->
            if (mediaStorageService.isUriAccessible(uri)) {
                appSettingsStorage.setWelcomeAudioUri(uri)
            } else {
                appSettingsStorage.clearWelcomeAudio()
                showAudioErrorMessage(getString(R.string.files_audio_error_file_not_accessible))
            }
        }

        showShortToast(R.string.common_info_changes_saved)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * Show icon picker dialog with 4 columns and vertical scrolling
     */
    private fun showIconPickerDialog(currentDomainDTO: DomainDTO, onDTOUpdated: (DomainDTO) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_domain_edit, null)
        val rvIconsList = dialogView.findViewById<RecyclerView>(R.id.rv_icons_list)
        val etDomainName = dialogView.findViewById<TextInputEditText>(R.id.et_domain_name)
        val tvDomainNameValidationInfo = dialogView.findViewById<TextView>(R.id.tv_domain_name_validation_info)

        etDomainName.setText(currentDomainDTO.name)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton(getString(R.string.common_btn_cancel), null)
            .create()

        // Fixed 4-columns + vertical scrolling
        rvIconsList.layoutManager = GridLayoutManager(this, 4)

        lifecycleScope.launch {
            try {
                rvIconsList.visibility = View.GONE

                val availableIconsIds = iconsService.getAvailableIconsIds()
                val iconsMap = iconsService.loadIconsBatch(availableIconsIds)

                rvIconsList.visibility = View.VISIBLE

                val adapter = IconPickerAdapter(
                    iconsIds = availableIconsIds,
                    iconsMap = iconsMap,
                    selectedIconNumber = currentDomainDTO.iconId
                )
                { selectedIconNumber ->
                    val updatedName = etDomainName.text.toString()
                    val updatedDTO = DomainDTO(id = currentDomainDTO.id, name = updatedName, iconId = selectedIconNumber)
                    val validationResult = domainValidator.validate(updatedDTO)
                    when(validationResult){
                        is ValidationResult.Valid -> {
                            onDTOUpdated(updatedDTO)
                            dialog.dismiss()
                        }
                        is ValidationResult.Error -> {
                            tvDomainNameValidationInfo.text = validationResult.message
                        }
                    }
                }

                rvIconsList.adapter = adapter

            }
            catch (e: Exception) {
                // TODO: Handling exception is needed?
            }
        }

        dialog.show()
    }

    /**
     * Helper method to update button icon after selection
     */
    private fun updateDomainButton(buttonIndex: Int, updatedDomainDTO : DomainDTO) {
        lifecycleScope.launch {
            // Find the button in GridLayout and update its icon
            val glDomains = findViewById<GridLayout>(R.id.gl_domains)
            if (buttonIndex < glDomains.childCount) {
                val buttonView = glDomains.getChildAt(buttonIndex)

                val ivDomainIcon = buttonView.findViewById<ImageView>(R.id.iv_domain_icon)
                val iconDrawable = iconsService.getDrawableIcon(updatedDomainDTO.iconId)
                ivDomainIcon.setImageDrawable(iconDrawable)

                val tvDomainName = buttonView.findViewById<TextView>(R.id.tv_domain_name)
                tvDomainName.text = updatedDomainDTO.name
            }
        }
    }
}