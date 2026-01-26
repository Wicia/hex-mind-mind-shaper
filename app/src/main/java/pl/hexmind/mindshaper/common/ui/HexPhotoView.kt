package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.MultipleActionsDialog
import timber.log.Timber
import kotlin.math.max

/**
 * Universal photo display and capture view
 * Pattern: AudioRecordingView
 */
class HexPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Mode {
        CAPTURE_DISPLAY,  // Capture + Display
        DISPLAY_ONLY      // View only
    }

    enum class State {
        EMPTY,      // No photo
        LOADING,    // Loading
        LOADED      // Displayed
    }

    companion object {
        private const val TAG = "HexPhotoView"
        private const val THUMBNAIL_SIZE_DP = 200
    }

    interface PhotoCallback {
        fun onCameraCaptureRequested()
        fun onGalleryPickRequested()
        fun onPhotoDeleted()
        fun onPhotoClicked()
        fun onError(error: String)
    }

    // UI Components (public like AudioRecordingView)
    val ivPhoto: ImageView
    private val tvInfo: TextView
    private val progressBar: ProgressBar
    val btnCamera: MaterialButton
    val btnGallery: MaterialButton
    val btnDelete: MaterialButton

    // State
    private var mode: Mode = Mode.CAPTURE_DISPLAY
    private var state: State = State.EMPTY
    private var currentBitmap: Bitmap? = null
    private var compact: Boolean = false
    private var callback: PhotoCallback? = null
    private var viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        inflate(context, R.layout.view_photo_display, this)
        orientation = VERTICAL

        // Initialize UI components
        ivPhoto = findViewById(R.id.iv_photo)
        tvInfo = findViewById(R.id.tv_info)
        progressBar = findViewById(R.id.progress_bar)
        btnCamera = findViewById(R.id.btn_camera)
        btnGallery = findViewById(R.id.btn_gallery)
        btnDelete = findViewById(R.id.btn_delete)

        // Read XML attributes
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.PhotoDisplayView) {
                val modeValue = getInt(R.styleable.PhotoDisplayView_photoMode, 0)
                mode = if (modeValue == 1) Mode.DISPLAY_ONLY else Mode.CAPTURE_DISPLAY
                compact = getBoolean(R.styleable.PhotoDisplayView_compact, false)
                setupUIForMode()
            }
        }

        setupListeners()
        updateUIForState()
    }

    private fun setupUIForMode() {
        when (mode) {
            Mode.CAPTURE_DISPLAY -> {
                btnCamera.visibility = VISIBLE
                btnGallery.visibility = VISIBLE
            }
            Mode.DISPLAY_ONLY -> {
                btnCamera.visibility = GONE
                btnGallery.visibility = GONE
                btnDelete.visibility = GONE
            }
        }
    }

    private fun setupListeners() {
        btnCamera.setOnClickListener {
            callback?.onCameraCaptureRequested()
        }

        btnGallery.setOnClickListener {
            callback?.onGalleryPickRequested()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        ivPhoto.setOnClickListener {
            if (state == State.LOADED) {
                callback?.onPhotoClicked()
            }
        }
    }

    private fun updateUIForState() {
        val hasPhoto = state == State.LOADED
        val isLoading = state == State.LOADING

        when {
            isLoading -> {
                ivPhoto.visibility = GONE
                tvInfo.visibility = GONE
                progressBar.visibility = VISIBLE
                btnCamera.visibility = GONE
                btnGallery.visibility = GONE
                btnDelete.visibility = GONE
            }
            hasPhoto -> {
                ivPhoto.visibility = VISIBLE
                tvInfo.visibility = GONE
                progressBar.visibility = GONE
                btnCamera.visibility = GONE
                btnGallery.visibility = GONE
                btnDelete.visibility = if (mode == Mode.CAPTURE_DISPLAY) VISIBLE else GONE
            }
            else -> { // EMPTY
                ivPhoto.visibility = GONE
                tvInfo.visibility = VISIBLE
                progressBar.visibility = GONE
                btnCamera.visibility = if (mode == Mode.CAPTURE_DISPLAY) VISIBLE else GONE
                btnGallery.visibility = if (mode == Mode.CAPTURE_DISPLAY) VISIBLE else GONE
                btnDelete.visibility = GONE
            }
        }
    }

    private fun showDeleteConfirmation() {
        MultipleActionsDialog.Builder(context)
            .setTitle(context.getString(R.string.photos_removing_header))
            .setDescription(context.getString(R.string.photos_removing_file))
            .setCautionAction(context.getString(R.string.common_deletion_dialog_yes_2)) {
                deletePhoto()
            }
            .show()
    }

    private fun deletePhoto() {
        clearPhoto()
        callback?.onPhotoDeleted()
    }

    // ===========================================
    //      Public API Methods
    // ===========================================

    fun loadPhoto(photoData: ByteArray) {
        viewScope.launch {
            try {
                state = State.LOADING
                updateUIForState()

                val bitmap = withContext(Dispatchers.Default) {
                    createThumbnail(photoData)
                }

                if (bitmap != null) {
                    currentBitmap?.recycle()
                    currentBitmap = bitmap
                    ivPhoto.setImageBitmap(bitmap)

                    state = State.LOADED
                    updateUIForState()
                }
                else {
                    showError(context.getString(R.string.photos_loading_error))
                }
            }
            catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading photo")
                showError(context.getString(R.string.photos_loading_error))
            }
        }
    }

    fun clearPhoto() {
        currentBitmap?.recycle()
        currentBitmap = null
        ivPhoto.setImageDrawable(null)
        state = State.EMPTY
        updateUIForState()
    }

    fun setCallback(callback: PhotoCallback) {
        this.callback = callback
    }

    fun showStatus(text: String, colorRes: Int) {
        ivPhoto.visibility = GONE
        tvInfo.visibility = VISIBLE
        tvInfo.text = text
        tvInfo.setTextColor(ContextCompat.getColor(context, colorRes))
        progressBar.visibility = GONE
    }

    fun showStatus(textRes: Int, colorRes: Int) {
        ivPhoto.visibility = GONE
        tvInfo.visibility = VISIBLE
        tvInfo.text = context.getString(textRes)
        tvInfo.setTextColor(ContextCompat.getColor(context, colorRes))
        progressBar.visibility = GONE
    }

    fun showError(message: String) {
        showStatus(message, R.color.validation_error)
        state = State.EMPTY
        updateUIForState()
        callback?.onError(message)
    }

    fun showLoading() {
        state = State.LOADING
        updateUIForState()
    }

    // ===========================================
    //      Helper Methods
    // ===========================================

    private fun createThumbnail(photoData: ByteArray): Bitmap? {
        return try {
            // Calculate density-aware thumbnail size
            val density = context.resources.displayMetrics.density
            val thumbnailSizePx = (THUMBNAIL_SIZE_DP * density).toInt()

            // Decode bounds first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(photoData, 0, photoData.size, options)

            // Calculate sample size for memory efficiency
            // Use power of 2 for better quality
            var scale = 1
            while (options.outWidth / (scale * 2) >= thumbnailSizePx &&
                options.outHeight / (scale * 2) >= thumbnailSizePx) {
                scale *= 2
            }

            // Decode with sample size
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = scale
                inPreferredConfig = Bitmap.Config.ARGB_8888  // High quality
            }

            val scaledBitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.size, options)
                ?: return null

            // Create high-quality thumbnail using Canvas
            val thumbnail = createHighQualityThumbnail(scaledBitmap, thumbnailSizePx)

            scaledBitmap.recycle()
            thumbnail
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error creating thumbnail")
            null
        }
    }

    /**
     * Creates high-quality thumbnail using Canvas with anti-aliasing
     */
    private fun createHighQualityThumbnail(source: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        // Calculate scaling to fit and center crop
        val scale = max(
            size.toFloat() / source.width,
            size.toFloat() / source.height
        )

        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val left = (size - scaledWidth) / 2f
        val top = (size - scaledHeight) / 2f

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(source, null, destRect, paint)

        return output
    }

    // ===========================================
    //      Lifecycle Methods
    // ===========================================

    fun cleanupResources(cancelCoroutines: Boolean = true) {
        currentBitmap?.recycle()
        currentBitmap = null
        ivPhoto.setImageDrawable(null)

        if (cancelCoroutines) {
            viewScope.cancel()
            viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        state = State.EMPTY
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanupResources()
    }
}