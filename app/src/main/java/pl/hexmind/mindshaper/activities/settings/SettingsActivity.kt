package pl.hexmind.mindshaper.activities.settings

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.database.initialization.DataSnapshotManager
import pl.hexmind.mindshaper.databinding.SettingsActivityBinding
import pl.hexmind.mindshaper.services.AppSettingsStorage
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

    private var selectedBackupUri: Uri? = null

    private var slowModeHours: Int = 1

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
            OnboardingProgressStep.SETTINGS_ENTRY_TOOLTIP, this
        )
    }

    override fun onResume() {
        super.onResume()

        // Sync of permissions related widgets
        syncVoiceRecordingToggleWithPermissions()
        syncPhotoToggleWithPermissions()
        syncBackupToggleWithPermissions()
        refreshSnapshotStats()
    }

    /**
     * Initialize UI components and click listeners
     */
    private fun setupUI() {
        setupHeader(R.drawable.ic_activity_settings, R.string.settings_header)

        // ! Keep sequence: initDefaultCaptureFormConfig MUST be before setupListeners
        // so that tile states are ready before syncDefaultCaptureFormTileStates fires
        initDefaultCaptureFormConfig()
        initThoughtsValuesSystemConfig()
        initSlowModeConfig()
        initDormantModeConfig()
        setupListeners()
        initDomainButtons()
    }

    private fun setupListeners() {
        // Onboarding :)
        binding.tvYourNameSectionHeader.setOnClickListener { // dla beki :P
            onboardingManager.showTooltipForStep(
                OnboardingProgressStep.SETTINGS_YOUR_NAME_TOOLTIP, this
            )
        }
        binding.tvThoughtValueScaleSectionHeader.setOnClickListener {
            onboardingManager.showTooltipForStep(
                OnboardingProgressStep.SETTINGS_THOUGHT_VALUE_TOOLTIP, this
            )
        }
        binding.tvDomainsSectionHeader.setOnClickListener {
            onboardingManager.showTooltipForStep(
                OnboardingProgressStep.SETTINGS_DOMAINS_TOOLTIP, this
            )
        }
        binding.tvBackupSectionHeader.setOnClickListener {
            onboardingManager.showTooltipForStep(
                OnboardingProgressStep.SETTINGS_BACKUP_TOOLTIP, this
            )
        }

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

        setupVoiceRecordingFeatureToggle()
        setupPhotoFeatureToggle()
        setupBackupFeatureToggle()
        setupSlowModeListeners()
        setupDormantModeListeners()
    }

    // ========== DEFAULT CAPTURE FORM ==========

    private fun initDefaultCaptureFormConfig() {
        binding.tilesDefaultCaptureForm.setSelected(appSettingsStorage.getDefaultCaptureForm())
        // Tile enabled states are synced in onResume via syncDefaultCaptureFormTileStates
    }

    /**
     * Enables/disables capture form tiles based on preffs storage + permissions
     */
    private fun syncDefaultCaptureFormTileStates() {
        val voiceEnabled = appSettingsStorage.isVoiceRecordingEnabled() && permissionsService.isRecordAudioGranted()
        binding.tilesDefaultCaptureForm.setVoiceEnabled(voiceEnabled)

        val photoEnabled = appSettingsStorage.isPhotoFeatureEnabled()   && permissionsService.isCameraGranted()
        binding.tilesDefaultCaptureForm.setPhotoEnabled(photoEnabled)
    }

    // ========== VOICE RECORDING FEATURE ==========

    private fun setupVoiceRecordingFeatureToggle() {
        binding.switchVoiceRecordingFeature.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                when {
                    permissionsService.isRecordAudioGranted() -> {
                        appSettingsStorage.setVoiceRecordingEnabled(true)
                        binding.switchVoiceRecordingFeature.isChecked = true
                    }
                    else -> {
                        showVoiceRecordingPermissionExplanationDialog()
                    }
                }
            }
            else {
                appSettingsStorage.setVoiceRecordingEnabled(false)
            }
        }

        syncVoiceRecordingToggleWithPermissions()
    }

    private fun syncVoiceRecordingToggleWithPermissions() {
        val hasPermission = permissionsService.isRecordAudioGranted()
        val wantsRecording = appSettingsStorage.isVoiceRecordingEnabled()

        binding.switchVoiceRecordingFeature.isChecked = wantsRecording && hasPermission
        syncDefaultCaptureFormTileStates()
    }

    private val requestVoiceRecordingPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Approved
            appSettingsStorage.setVoiceRecordingEnabled(true)
            binding.switchVoiceRecordingFeature.isChecked = true
        }
        else {
            // Denial
            appSettingsStorage.setVoiceRecordingEnabled(false)
            binding.switchVoiceRecordingFeature.isChecked = false

            // Handling permissions "blockade"
            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                showVoiceRecordingPermanentDenialDialog()
            }
        }
    }

    private fun showVoiceRecordingPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_thoughts_permissions_dialog_header))
            .setMessage(getString(R.string.settings_voice_recording_info))
            .setPositiveButton(R.string.common_btn_confirm_ok) { _, _ ->
                requestVoiceRecordingPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton(R.string.common_btn_cancel_not_now) { _, _ ->
                binding.switchVoiceRecordingFeature.isChecked = false
            }
            .setOnCancelListener {
                binding.switchVoiceRecordingFeature.isChecked = false
            }
            .show()
    }

    private fun showVoiceRecordingPermanentDenialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_permissions_blockade_title))
            .setMessage(getString(R.string.settings_permissions_recording_blockade_tooltip))
            .setPositiveButton(getString(R.string.common_dialog_open_android_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.common_btn_cancel) { _, _ ->
                binding.switchVoiceRecordingFeature.isChecked = false
            }
            .show()
    }

    // ========== PHOTO FEATURE ==========

    private fun setupPhotoFeatureToggle() {
        binding.switchPhotoFeature.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                when {
                    permissionsService.isCameraGranted() -> {
                        appSettingsStorage.setPhotoFeatureEnabled(true)
                        binding.switchPhotoFeature.isChecked = true
                    }
                    else -> {
                        showPhotoPermissionExplanationDialog()
                    }
                }
            }
            else {
                appSettingsStorage.setPhotoFeatureEnabled(false)
            }
        }

        syncPhotoToggleWithPermissions()
    }

    private fun syncPhotoToggleWithPermissions() {
        val hasPermission = permissionsService.isCameraGranted()
        val wantsPhoto = appSettingsStorage.isPhotoFeatureEnabled()

        binding.switchPhotoFeature.isChecked = wantsPhoto && hasPermission
        syncDefaultCaptureFormTileStates()
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Approved
            appSettingsStorage.setPhotoFeatureEnabled(true)
            binding.switchPhotoFeature.isChecked = true
        }
        else {
            // Denial
            appSettingsStorage.setPhotoFeatureEnabled(false)
            binding.switchPhotoFeature.isChecked = false

            // Handling permissions "blockade"
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showPhotoPermanentDenialDialog()
            }
        }
    }

    private fun showPhotoPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_thoughts_permissions_dialog_header))
            .setMessage(getString(R.string.settings_permissions_photo_info))
            .setPositiveButton(R.string.common_btn_confirm_ok) { _, _ ->
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(R.string.common_btn_cancel_not_now) { _, _ ->
                binding.switchPhotoFeature.isChecked = false
            }
            .setOnCancelListener {
                binding.switchPhotoFeature.isChecked = false
            }
            .show()
    }

    private fun showPhotoPermanentDenialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_permissions_blockade_title))
            .setMessage(getString(R.string.settings_permissions_photo_blockade_tooltip))
            .setPositiveButton(R.string.common_dialog_open_android_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.common_btn_cancel) { _, _ ->
                binding.switchPhotoFeature.isChecked = false
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    // ========== BACKUP FEATURE ==========

    private fun setupBackupFeatureToggle() {
        binding.switchBackupFeature.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                when {
                    permissionsService.isStorageGranted() -> {
                        appSettingsStorage.setBackupEnabled(true)
                        binding.switchBackupFeature.isChecked = true
                    }
                    else -> {
                        showBackupPermissionExplanationDialog()
                    }
                }
            }
            else {
                appSettingsStorage.setBackupEnabled(false)
            }
        }

        syncBackupToggleWithPermissions()
    }

    private fun syncBackupToggleWithPermissions() {
        val hasPermission = permissionsService.isStorageGranted()
        val wantsBackup = appSettingsStorage.isBackupEnabled()

        binding.switchBackupFeature.isChecked = wantsBackup && hasPermission
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Approved
            appSettingsStorage.setBackupEnabled(true)
            binding.switchBackupFeature.isChecked = true
        }
        else {
            // Denial
            appSettingsStorage.setBackupEnabled(false)
            binding.switchBackupFeature.isChecked = false

            // Handling permissions "blockade"
            if (!shouldShowRequestPermissionRationale(permissionsService.getStoragePermission())) {
                showBackupPermanentDenialDialog()
            }
        }
    }

    private fun refreshSnapshotStats() {
        lifecycleScope.launch(Dispatchers.IO) {
            val stats = dataSnapshotManager.getSnapshotStats()
            val text = if (stats.count == 0) {
                getString(R.string.settings_backup_no_snapshots)
            }
            else {
                getString(R.string.settings_backup_stats, stats.count, stats.totalSizeMb)
            }
            withContext(Dispatchers.Main) {
                binding.tvSnapshotStats.text = text
            }
        }
    }

    private fun showBackupPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_thoughts_permissions_dialog_header))
            .setMessage(getString(R.string.settings_backup_permission_info))
            .setPositiveButton(R.string.common_btn_confirm_ok) { _, _ ->
                requestStoragePermissionLauncher.launch(permissionsService.getStoragePermission())
            }
            .setNegativeButton(R.string.common_btn_cancel_not_now) { _, _ ->
                binding.switchBackupFeature.isChecked = false
            }
            .setOnCancelListener {
                binding.switchBackupFeature.isChecked = false
            }
            .show()
    }

    private fun showBackupPermanentDenialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.settings_permissions_blockade_title))
            .setMessage(getString(R.string.settings_permissions_backup_blockade_tooltip))
            .setPositiveButton(R.string.common_dialog_open_android_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.common_btn_cancel) { _, _ ->
                binding.switchBackupFeature.isChecked = false
            }
            .show()
    }

    // ========== THOUGHTS VALUES SYSTEM & DOMAINS ==========

    private fun initThoughtsValuesSystemConfig() {
        binding.tilesValueSystem.setSelected(appSettingsStorage.getThoughtValueSystem())
    }

    // ========== SLOW MODE ==========

    private fun initSlowModeConfig() {
        slowModeHours = appSettingsStorage.getSlowModeHours()
        binding.switchSlowMode.isChecked = appSettingsStorage.isSlowModeEnabled()
        updateSlowModeHoursDisplay()
        syncSlowModeHoursPickerState()
    }

    private fun setupSlowModeListeners() {
        binding.switchSlowMode.setOnCheckedChangeListener { _, _ ->
            syncSlowModeHoursPickerState()
        }
        binding.btnSlowModeDecrease.setOnClickListener {
            if (slowModeHours > 1) { slowModeHours--; updateSlowModeHoursDisplay() }
        }
        binding.btnSlowModeIncrease.setOnClickListener {
            if (slowModeHours < 12) { slowModeHours++; updateSlowModeHoursDisplay() }
        }
    }

    private fun updateSlowModeHoursDisplay() {
        binding.tvSlowModeHours.text = slowModeHours.toString()
        binding.btnSlowModeDecrease.isEnabled = slowModeHours > AppSettingsStorage.SLOW_MODE_HOURS_MIN
        binding.btnSlowModeIncrease.isEnabled = slowModeHours < AppSettingsStorage.SLOW_MODE_HOURS_MAX
    }

    private fun syncSlowModeHoursPickerState() {
        val enabled = binding.switchSlowMode.isChecked
        binding.llSlowModeHoursPicker.alpha = if (enabled) 1f else 0.4f
        binding.btnSlowModeDecrease.isEnabled = enabled && slowModeHours > AppSettingsStorage.SLOW_MODE_HOURS_MIN
        binding.btnSlowModeIncrease.isEnabled = enabled && slowModeHours < AppSettingsStorage.SLOW_MODE_HOURS_MAX
    }

    // ========== DORMANT MODE ==========

    private fun initDormantModeConfig() {
        binding.switchDormantMode.isChecked = appSettingsStorage.isDormantModeEnabled()
        binding.etDormantDays.setText(appSettingsStorage.getDormantDaysThreshold().toString())
        binding.etDormantValue.setText(appSettingsStorage.getDormantValueThreshold().toString())
        syncDormantPickerState()
        validateDormantDaysInput()
        validateDormantValueInput()
    }

    private fun setupDormantModeListeners() {
        binding.switchDormantMode.setOnCheckedChangeListener { _, _ -> syncDormantPickerState() }

        // TextWatcher for live validation of value threshold
        binding.etDormantValue.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                validateDormantValueInput()
            }
        })

        // TextWatcher for live validation of days threshold
        binding.etDormantDays.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                validateDormantDaysInput()
            }
        })
    }

    private fun syncDormantPickerState() {
        val enabled = binding.switchDormantMode.isChecked
        binding.llDormantThresholdsPicker.alpha = if (enabled) 1f else 0.4f
        binding.etDormantDays.isEnabled  = enabled
        binding.etDormantValue.isEnabled = enabled
    }

    private fun validateDormantDaysInput() : Boolean {
        val raw = binding.etDormantDays.text?.toString()?.toIntOrNull()
        val isValid = raw != null && raw in AppSettingsStorage.DORMANT_DAYS_MIN..AppSettingsStorage.DORMANT_DAYS_MAX
        binding.tvDormantModeError.visibility = if (isValid) View.INVISIBLE else View.VISIBLE
        binding.tvDormantModeError.text = getString(
            R.string.settings_dormant_mode_days_error,
            AppSettingsStorage.DORMANT_DAYS_MIN,
            AppSettingsStorage.DORMANT_DAYS_MAX
        )

        return isValid
    }

    private fun validateDormantValueInput() : Boolean {
        val max = appSettingsStorage.getDormantValueMax()
        val raw = binding.etDormantValue.text?.toString()?.toIntOrNull()
        val isValid = raw != null && raw in AppSettingsStorage.DORMANT_VALUE_MIN..max
        binding.tvDormantModeError.visibility = if (isValid) View.INVISIBLE else View.VISIBLE
        binding.tvDormantModeError.text = getString(
            R.string.settings_dormant_mode_value_error,
            AppSettingsStorage.DORMANT_VALUE_MIN,
            max
        )

        return isValid
    }

    private fun readDormantDays(): Int? {
        val raw = binding.etDormantDays.text?.toString()?.toIntOrNull() ?: return null
        return raw.coerceIn(AppSettingsStorage.DORMANT_DAYS_MIN, AppSettingsStorage.DORMANT_DAYS_MAX)
    }

    private fun readDormantValue(): Int? {
        val raw = binding.etDormantValue.text?.toString()?.toIntOrNull() ?: return null
        return raw.coerceIn(AppSettingsStorage.DORMANT_VALUE_MIN, appSettingsStorage.getDormantValueMax())
    }

    // ========== DOMAINS ==========

    private fun initDomainButtons() {
        lifecycleScope.launch {
            val titles = domainService.getAllDomains()

            try {
                // Create buttons with loaded icons
                titles.forEachIndexed { domainIndex , domainDTO ->
                    val buttonView = layoutInflater.inflate(R.layout.settings_domain_item, binding.glDomains, false)

                    buttonView.findViewById<TextView>(R.id.tv_domain_name).text = domainDTO.name

                    val resourceId = domainIconsService.getIconResourceId(domainDTO.iconId)
                    buttonView.findViewById<ImageView>(R.id.iv_domain_icon).setImageResource(resourceId)

                    buttonView.setOnClickListener {
                        onDomainButtonClick(domainIndex, domainDTO)
                    }

                    binding.glDomains.addView(buttonView)
                }
            }
            catch (e: Exception) {
                // TODO: add UI control + handle error using: R.string.settings_domains_loading_error))
            }
        }
    }

    private fun onDomainButtonClick(domainTileIndex: Int, currentDomainDTO : DomainDTO) {
        showDomainEditBottomSheet(currentDomainDTO) { updatedDTO ->
            updateDomainButton(domainTileIndex, updatedDTO)
            lifecycleScope.launch { domainService.updateDomain(dto = updatedDTO) }
        }
    }

    /**
     * Load previously saved settings using AppSettingsStorage
     */
    private fun loadSavedSettings() {
        binding.etYourName.setText(appSettingsStorage.getYourName())
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

    private fun showSnapshotLoadingDialog() {
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
                    refreshSnapshotStats()
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
     * Save all settings using AppSettingsStorage.
     * Exceptions: voice/photo feature toggles are saved immediately (permission flow side effects).
     */
    private fun saveSettings() {
        val yourName = binding.etYourName.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.settings_your_name_default)
        appSettingsStorage.setYourName(yourName)

        appSettingsStorage.setDefaultCaptureForm(binding.tilesDefaultCaptureForm.getSelected())

        appSettingsStorage.setThoughtValueSystemId(binding.tilesValueSystem.getSelected())

        // Slow mode
        appSettingsStorage.setSlowModeEnabled(binding.switchSlowMode.isChecked)
        appSettingsStorage.setSlowModeHours(slowModeHours)

        // Dormant mode
        if (binding.switchDormantMode.isChecked && !validateDormantDaysInput()) {
            showShortToast(R.string.settings_dormant_mode_days_error_toast)
            return
        }
        if (binding.switchDormantMode.isChecked && !validateDormantValueInput()) {
            showShortToast(R.string.settings_dormant_mode_value_error_toast)
            return
        }
        appSettingsStorage.setDormantModeEnabled(binding.switchDormantMode.isChecked)
        appSettingsStorage.setDormantDaysThreshold(readDormantDays() ?: AppSettingsStorage.DORMANT_DAYS_DEFAULT)
        appSettingsStorage.setDormantValueThreshold(readDormantValue() ?: AppSettingsStorage.DORMANT_VALUE_DEFAULT)

        showShortToast(R.string.common_info_changes_saved)

        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun showDomainEditBottomSheet(currentDomainDTO: DomainDTO, onDTOUpdated: (DomainDTO) -> Unit) {
        lifecycleScope.launch {
            try {
                val availableIconsIds = domainIconsService.getAvailableIconsIds()
                val iconsMap = domainIconsService.loadIconsBatch(availableIconsIds)

                // Convert to IconsGridItem list expected by IconsGridView
                val iconItems = availableIconsIds.mapNotNull { id ->
                    iconsMap[id]?.let { resId -> IconsGridItem(id = id, iconResId = resId) }
                }

                DomainEditBottomSheet.show(
                    fragmentManager = supportFragmentManager,
                    items = iconItems,
                    domainName = currentDomainDTO.name,
                    selectedIconId = currentDomainDTO.iconId,
                    validator = domainValidator
                ) { name, iconId ->
                    val updatedDTO = DomainDTO(id = currentDomainDTO.id, name = name, iconId = iconId)
                    onDTOUpdated(updatedDTO)
                }
            }
            catch (e: Exception) {
                // TODO: Handling exception is needed?
            }
        }
    }

    /**
     * Helper method to update button icon after selection
     */
    private fun updateDomainButton(buttonIndex: Int, updatedDomainDTO : DomainDTO) {
        lifecycleScope.launch {
            // Find the button in GridLayout and update its icon
            if (buttonIndex < binding.glDomains.childCount) {
                val buttonView = binding.glDomains.getChildAt(buttonIndex)

                val resourceId = domainIconsService.getIconResourceId(updatedDomainDTO.iconId)
                buttonView.findViewById<ImageView>(R.id.iv_domain_icon).setImageResource(resourceId)
                buttonView.findViewById<TextView>(R.id.tv_domain_name).text = updatedDomainDTO.name
            }
        }
    }

    /**
     * Helper methods for removing focus from input when tapping on other widgets
     * TODO: Move it to Core?
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    view.clearFocus()
                    hideKeyboard(view)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}