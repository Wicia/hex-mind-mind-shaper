package pl.hexmind.mindshaper.activities.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.main.CoreActivity
import pl.hexmind.mindshaper.activities.main.MainActivity
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.databinding.ActivitySettingsBinding
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.DomainsService
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
    lateinit var appSettingsStorage: AppSettingsStorage

    @Inject
    lateinit var mediaStorageService : MediaStorageService

    @Inject
    lateinit var domainIconsLoader : DomainIconLoader

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

        val gridLayout = findViewById<GridLayout>(R.id.domains_grid_layout)
        initDomainButtons(gridLayout)
    }

    private fun initDomainButtons(gridLayout: GridLayout) {
        lifecycleScope.launch {
            val titles = domainService.getAllDomains()

            try {
                // Create buttons with loaded icons
                titles.forEachIndexed { domainIndex , domainDTO ->
                    val buttonView = layoutInflater.inflate(R.layout.item_settings_domain, gridLayout, false)

                    val ivDomainName = buttonView.findViewById<TextView>(R.id.tv_domain_name)
                    ivDomainName.text = domainDTO.name

                    val ivDomainIcon = buttonView.findViewById<ImageView>(R.id.iv_domain_icon)
                    ivDomainIcon.setImageDrawable(domainIconsLoader.loadIcon(domainDTO.assetImageId))

                    buttonView.setOnClickListener {
                        onDomainButtonClick(domainIndex, domainDTO)
                    }

                    gridLayout.addView(buttonView)
                }

            } catch (e: Exception) {
                showErrorMessage(getString(R.string.settings_domains_loading_error))
            }
        }
    }

    private fun showErrorMessage(message: String) {
        findViewById<TextView>(R.id.tv_domains_state_loading_info)?.let { errorView ->
            errorView.text = message
            errorView.visibility = View.VISIBLE
            errorView.setTextColor(ContextCompat.getColor(this, R.color.error_red))
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
        val appName = appSettingsStorage.getYourName()
        binding.etYourName.setText(appName)

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
        binding.tvSelectedFile.setTextColor(getColor(R.color.error_red))
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

            val fileName = mediaStorageService.getSimpleFileName(uri)
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
        val fileInfo = mediaStorageService.getDetailedFileInfo(uri)
        binding.tvSelectedFile.text = fileInfo
    }

    /**
     * Save all settings using AppSettingsStorage
     */
    private fun saveSettings() {
        // Save app name
        val appName = binding.etYourName.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.main_greetings_header_default)
        appSettingsStorage.setYourName(appName)

        // Save audio file path only if accessible
        selectedAudioUri?.let { uri ->
            if (mediaStorageService.isUriAccessible(uri)) {
                appSettingsStorage.setWelcomeAudioUri(uri)
            } else {
                appSettingsStorage.clearWelcomeAudio()
                showAudioErrorMessage(getString(R.string.audio_file_no_longer_accessible))
            }
        }

        // Save phase names with defaults
        val phase1 = binding.etPhase1.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.common_phase1_default_name)
        binding.etPhase1.setText(phase1)

        val phase2 = binding.etPhase2.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.common_phase2_default_name)
        binding.etPhase2.setText(phase2)

        val phase3 = binding.etPhase3.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.common_phase3_default_name)
        binding.etPhase3.setText(phase3)

        appSettingsStorage.setPhaseNames(phase1, phase2, phase3)

        showShortToast(R.string.common_info_changes_saved)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * Show icon picker dialog with 3 columns and vertical scrolling
     */
    private fun showIconPickerDialog(currentDomainDTO: DomainDTO, onDTOUpdated: (DomainDTO) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_domain_edit, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.icons_recycler)
        val loadingIndicator = dialogView.findViewById<ProgressBar>(R.id.loading_icons)
        val etDomainName = dialogView.findViewById<TextInputEditText>(R.id.et_domain_name)
        val tvDomainNameValidationInfo = dialogView.findViewById<TextView>(R.id.tv_domain_name_validation_info)

        etDomainName.setText(currentDomainDTO.name)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton(getString(R.string.common_btn_cancel), null)
            .create()

        // Fixed 3 columns with vertical scrolling
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch {
            try {
                loadingIndicator.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE

                val availableIcons = domainIconsLoader.getAvailableIcons()
                val iconsMap = domainIconsLoader.loadIconsBatch(availableIcons)

                loadingIndicator.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                val adapter = IconPickerAdapter(
                    icons = availableIcons,
                    iconsMap = iconsMap,
                    selectedIconNumber = currentDomainDTO.assetImageId
                )
                { selectedIconNumber ->
                    val updatedName = etDomainName.text.toString()
                    val updatedDTO = DomainDTO(id = currentDomainDTO.id, name = updatedName, assetImageId = selectedIconNumber)
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

                recyclerView.adapter = adapter

            } catch (e: Exception) {
                loadingIndicator.visibility = View.GONE
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
            val gridLayout = findViewById<GridLayout>(R.id.domains_grid_layout)
            if (buttonIndex < gridLayout.childCount) {
                val buttonView = gridLayout.getChildAt(buttonIndex)

                val ivDomainIcon = buttonView.findViewById<ImageView>(R.id.iv_domain_icon)
                val drawable = domainIconsLoader.loadIcon(updatedDomainDTO.assetImageId)
                ivDomainIcon.setImageDrawable(drawable)

                val tvDomainName = buttonView.findViewById<TextView>(R.id.tv_domain_name)
                tvDomainName.text = updatedDomainDTO.name
            }
        }
    }
}