package pl.hexmind.mindshaper.common.ui.views.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
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
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import timber.log.Timber
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Universal photo display and capture view
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
        private const val FULL_PHOTO_MAX_SIZE_DP = 400
        private const val EDGE_FADE_DP = 16f
    }

    interface PhotoCallback {
        fun onCameraCaptureRequested()
        fun onGalleryPickRequested()
        fun onPhotoDeleted()
        fun onPhotoClicked()
        fun onError(error: String)
    }

    // UI Components
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
        inflate(context, R.layout.common_photo_display_view, this)
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
            context.withStyledAttributes(it, R.styleable.HexPhotoView) {
                val modeValue = getInt(R.styleable.HexPhotoView_photoMode, 0)
                mode = if (modeValue == 1) Mode.DISPLAY_ONLY else Mode.CAPTURE_DISPLAY
                compact = getBoolean(R.styleable.HexPhotoView_compact, false)
                setupUIForMode()
            }
        }

        setupPhotoSizing()
        setupListeners()
        updateUIForState()
    }

    // Compact - fixed square frame, center crop (layout default); full - whole photo, own aspect ratio, capped size
    private fun setupPhotoSizing() {
        if (compact) return

        ivPhoto.layoutParams = (ivPhoto.layoutParams as FrameLayout.LayoutParams).apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER_HORIZONTAL
        }
        ivPhoto.adjustViewBounds = true
        ivPhoto.maxHeight = (FULL_PHOTO_MAX_SIZE_DP * resources.displayMetrics.density).toInt()
        ivPhoto.scaleType = ImageView.ScaleType.FIT_CENTER
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

        // // Display-only mode - no full-screen preview, let the tap reach the card (open thought details)
        if (mode == Mode.CAPTURE_DISPLAY) {
            ivPhoto.setOnClickListener {
                if (state == State.LOADED) {
                    callback?.onPhotoClicked()
                }
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
        ActionsDialog.Builder(context)
            .setTitle(context.getString(R.string.photos_removing_header))
            .setDescription(context.getString(R.string.common_deletion_dialog_warning))
            .setPrimaryAction(context.getString(R.string.photos_removing_yes), caution = true) {
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

    // onLoaded lets a host stay hidden until there's something to show, instead of reserving an empty frame
    fun loadPhoto(photoData: ByteArray, onLoaded: (Boolean) -> Unit = {}) {
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
                    onLoaded(true)
                }
                else {
                    showError(context.getString(R.string.photos_loading_error))
                    onLoaded(false)
                }
            }
            catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading photo")
                showError(context.getString(R.string.photos_loading_error))
                onLoaded(false)
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
            val thumbnailSizePx = ((if (compact) THUMBNAIL_SIZE_DP else FULL_PHOTO_MAX_SIZE_DP) * density).toInt()

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
     * Compact - square, center crop; full - longer side fits the size, aspect ratio kept, no upscaling
     */
    private fun createHighQualityThumbnail(source: Bitmap, size: Int): Bitmap {
        val widthScale = size.toFloat() / source.width
        val heightScale = size.toFloat() / source.height
        val scale = if (compact) max(widthScale, heightScale) else minOf(widthScale, heightScale, 1f)

        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val outputWidth = if (compact) size else scaledWidth.roundToInt()
        val outputHeight = if (compact) size else scaledHeight.roundToInt()

        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val left = (outputWidth - scaledWidth) / 2f
        val top = (outputHeight - scaledHeight) / 2f

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(source, null, destRect, paint)
        fadeEdges(canvas, outputWidth, outputHeight)

        return output
    }

    /**
     * Feathers all four edges into transparency - the photo blends into the background behind it
     */
    private fun fadeEdges(canvas: Canvas, width: Int, height: Int) {
        val fade = minOf(EDGE_FADE_DP * resources.displayMetrics.density, width / 4f, height / 4f)
        val w = width.toFloat()
        val h = height.toFloat()

        val maskPaint = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Each strip: gradient from transparent (outer edge) to opaque (inner side), keeps only the alpha it carries
        fun fadeStrip(rect: RectF, x0: Float, y0: Float, x1: Float, y1: Float) {
            maskPaint.shader = LinearGradient(x0, y0, x1, y1, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            canvas.drawRect(rect, maskPaint)
        }

        fadeStrip(RectF(0f, 0f, fade, h), 0f, 0f, fade, 0f)            // left
        fadeStrip(RectF(w - fade, 0f, w, h), w, 0f, w - fade, 0f)      // right
        fadeStrip(RectF(0f, 0f, w, fade), 0f, 0f, 0f, fade)            // top
        fadeStrip(RectF(0f, h - fade, w, h), 0f, h, 0f, h - fade)      // bottom
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
        // no cleanup here (cleanupResources()) - RecyclerView detaches rows on scroll, which killed
        // the decode mid-load; recycling hosts clean up on rebind
    }
}