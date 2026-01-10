package pl.hexmind.mindshaper.activities.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.common.ThoughtValueSystem
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.database.initialization.DataSnapshotManager
import pl.hexmind.mindshaper.databinding.SettingsActivityBinding
import pl.hexmind.mindshaper.services.DomainIconsService
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.MediaStorageService
import pl.hexmind.mindshaper.services.dto.DomainDTO
import pl.hexmind.mindshaper.services.validators.DomainValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity providing personalized settings for the memory app
 */
@AndroidEntryPoint
class SettingsActivity : CoreActivity() {

    @Inject
    lateinit var domainService : DomainsService

    @Inject
    lateinit var domainIconsService : DomainIconsService

    @Inject
    lateinit var mediaStorageService : MediaStorageService

    @Inject
    lateinit var domainValidator: DomainValidator

    @Inject
    lateinit var dataSnapshotManager: DataSnapshotManager

    private lateinit var binding: SettingsActivityBinding

    private var selectedAudioUri: Uri? = null
    private var selectedBackupUri: Uri? = null

    // Activity result launcher for backup file selection
    private val backupPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedBackupFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadSavedSettings()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.SETTINGS_TOOLTIP, this
        )
    }

    override fun onResume() {
        super.onResume()
        syncVoiceRecordingToogleWithPermissions()
    }

    /**
     * Initialize UI components and click listeners
     */
    private fun setupUI() {
        setupHeader(R.drawable.ic_header_settings, R.string.settings_header)

        setupListeners()
        initThoughtsValuesSystemConfig()
        initDomainButtons()
    }

    private fun setupListeners(){
        // Save settings button
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        // Backup file selection
        binding.btnSelectBackup.setOnClickListener {
            selectBackupFile()
        }

        // Load backup button
        binding.btnLoadBackup.setOnClickListener {
            showSnapshotLoadingDialog()
        }

        setupVoiceRecordingPermissionToogle()
    }

    private fun setupVoiceRecordingPermissionToogle() {
        binding.toggleRecording.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                when {
                    permissionsService.isRecordAudioGranted() -> {
                        appSettingsStorage.setVoiceRecordingEnabled(true)
                        binding.toggleRecording.isChecked = true
                    }
                    else -> {
                        showPermissionExplanationDialog()
                    }
                }
            }
            else {
                appSettingsStorage.setVoiceRecordingEnabled(false)
            }
        }

        syncVoiceRecordingToogleWithPermissions()
    }

    private fun syncVoiceRecordingToogleWithPermissions() {
        val hasPermission = permissionsService.isRecordAudioGranted()
        val wantsRecording = appSettingsStorage.isVoiceRecordingEnabled()

        binding.toggleRecording.isChecked = wantsRecording && hasPermission
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Approved
            appSettingsStorage.setVoiceRecordingEnabled(true)
            binding.toggleRecording.isChecked = true
        }
        else {
            // Denial
            appSettingsStorage.setVoiceRecordingEnabled(false)
            binding.toggleRecording.isChecked = false

            // Handling permissions "blockade"
            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                showPermissionPermanentDenialDialog()
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_thoughts_permissions_dialog_header))
            .setMessage(getString(R.string.common_thoughts_permissions_dialog_message, "Aby móc nagrywać dźwięk, aplikacja potrzebuje dostępu do mikrofonu."))
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("Nie teraz") { _, _ ->
                binding.toggleRecording.isChecked = false
            }
            .setOnCancelListener {
                binding.toggleRecording.isChecked = false
            }
            .show()
    }

    private fun showPermissionPermanentDenialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_thoughts_permissions_blockade))
            .setMessage(getString(R.string.settings_thoughts_permissions_blockade_tooltip))
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun initThoughtsValuesSystemConfig() {
        val radioGroup = findViewById<RadioGroup>(R.id.rg_values_system)
        val thoughtValueSystem = appSettingsStorage.getThoughtValueSystem()

        when (thoughtValueSystem) {
            ThoughtValueSystem.STANDARD_6 -> radioGroup.check(R.id.rb_system_6)
            ThoughtValueSystem.STANDARD_10 -> radioGroup.check(R.id.rb_system_10)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedButtonId ->
            when (checkedButtonId) {
                R.id.rb_system_6 -> {
                    appSettingsStorage.setThoughtValueSystemId(ThoughtValueSystem.STANDARD_6)
                }
                R.id.rb_system_10 -> {
                    appSettingsStorage.setThoughtValueSystemId(ThoughtValueSystem.STANDARD_10)
                }
            }
        }
    }

    private fun initDomainButtons() {
        val gridLayout = findViewById<GridLayout>(R.id.gl_domains)
        lifecycleScope.launch {
            val titles = domainService.getAllDomains()

            try {
                // Create buttons with loaded icons
                titles.forEachIndexed { domainIndex , domainDTO ->
                    val buttonView = layoutInflater.inflate(R.layout.settings_domains_item, gridLayout, false)

                    val ivDomainName = buttonView.findViewById<TextView>(R.id.tv_domain_name)
                    ivDomainName.text = domainDTO.name

                    val ivDomainIcon = buttonView.findViewById<ImageView>(R.id.iv_domain_icon)
                    val resourceId = domainIconsService.getIconResourceId(domainDTO.iconId)
                    ivDomainIcon.setImageResource(resourceId)

                    buttonView.setOnClickListener {
                        onDomainButtonClick(domainIndex, domainDTO)
                    }

                    gridLayout.addView(buttonView)
                }
            }
            catch (e: Exception) {
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
    }

    /**
     * Open file picker for backup JSON file selection in Downloads/mindshaper_backup folder
     */
    private fun selectBackupFile() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)

                // Set initial directory to Downloads/mindshaper_backup (API 26+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val downloadsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Download/mindshaper_backup")
                    putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, downloadsUri)
                }
            }

            backupPickerLauncher.launch(intent)
        }
        catch (e: Exception) {
            showShortToast(R.string.files_error_cannot_open_file_picker)
        }
    }

    /**
     * Handle selected backup file
     */
    private fun handleSelectedBackupFile(uri: Uri) {
        try {
            // Verify if file is accessible
            if (mediaStorageService.isUriAccessible(uri)) {
                selectedBackupUri = uri
                val fileName = mediaStorageService.getFileNameFromUri(uri)
                binding.tvSelectedBackup.text = getString(R.string.settings_backup_state_file_selected, fileName)
                binding.btnLoadBackup.isEnabled = true

                // Reset text color to default
                binding.tvSelectedBackup.setTextColor(getColor(R.color.text_secondary))
            }
            else {
                showBackupErrorMessage(getString(R.string.files_audio_error_file_not_accessible))
            }
        }
        catch (e: Exception) {
            showBackupErrorMessage(getString(R.string.file_audio_error_reading_file))
            showShortToast(R.string.files_error_selecting_file)
        }
    }

    private fun showSnapshotLoadingDialog(){
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_deletion_dialog_title))
            .setMessage(getString(R.string.settings_snapshot_restore_warning))
            .setPositiveButton(getString(R.string.common_deletion_dialog_yes)) { dialog, _ ->
                loadBackupFile()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.common_deletion_dialog_no)) { dialog, _ ->
                Timber.d("Snapshot loading cancelled")
                dialog.dismiss()
            }
            .show()
    }

    private fun loadBackupFile() {
        selectedBackupUri?.let { uri ->
            try {
                lifecycleScope.launch {
                    val fileName = mediaStorageService.getFileNameFromUri(uri)
                    dataSnapshotManager.restoreSnapshot(fileName, this@SettingsActivity)
                    showShortToast(R.string.settings_backup_loaded_success)
                    // Reset after successful load
                    clearBackupSelection()
                }
            }
            catch (e: Exception) {
                showBackupErrorMessage(getString(R.string.settings_backup_error_loading))
                showShortToast(R.string.settings_backup_error_loading)
            }
        }
    }

    /**
     * Clear backup selection and update UI
     */
    private fun clearBackupSelection() {
        selectedBackupUri = null
        binding.tvSelectedBackup.text = getString(R.string.settings_backup_state_no_file)
        binding.btnLoadBackup.isEnabled = false
        binding.tvSelectedBackup.setTextColor(getColor(R.color.text_secondary))
    }

    /**
     * Show error message for backup file issues
     */
    private fun showBackupErrorMessage(message: String) {
        binding.tvSelectedBackup.text = message
        binding.tvSelectedBackup.setTextColor(getColor(R.color.validation_error))
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

        showShortToast(R.string.common_info_changes_saved)

        val intent = Intent(this, HomeActivity::class.java)
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

                val availableIconsIds = domainIconsService.getAvailableIconsIds()
                val iconsMap = domainIconsService.loadIconsBatch(availableIconsIds)

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
                val resourceId = domainIconsService.getIconResourceId(updatedDomainDTO.iconId)
                ivDomainIcon.setImageResource(resourceId)

                val tvDomainName = buttonView.findViewById<TextView>(R.id.tv_domain_name)
                tvDomainName.text = updatedDomainDTO.name
            }
        }
    }
}