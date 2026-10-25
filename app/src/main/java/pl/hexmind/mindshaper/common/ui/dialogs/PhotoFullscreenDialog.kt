package pl.hexmind.mindshaper.common.ui.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.R
import timber.log.Timber

/**
 * Fullscreen photo viewer with zoom support.
 * Screens are portrait-only; while shown, the host activity follows the device rotation.
 */
class PhotoFullscreenDialog(
    context: Context,
    private val photoData: ByteArray
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var photoView: PhotoView
    private lateinit var btnClose: ImageButton
    private lateinit var progressBar: ProgressBar

    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val hostActivity = context as? Activity
    private var hostOrientation: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.common_photo_fullscreen_dialog)

        // Full screen setup
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        photoView = findViewById(R.id.photo_view)
        btnClose = findViewById(R.id.btn_close)
        progressBar = findViewById(R.id.progress_bar)

        btnClose.setOnClickListener { dismiss() }
        keepCloseButtonAboveSystemBar()

        loadPhoto()
    }

    // Base bottom margin from the layout + system navigation bar (3 buttons or gesture bar)
    private fun keepCloseButtonAboveSystemBar() {
        val baseMargin = (btnClose.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(btnClose) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseMargin + systemBars.bottom
            }
            insets
        }
    }

    // USER respects the system auto-rotate switch
    override fun onStart() {
        super.onStart()
        hostActivity?.let {
            hostOrientation = it.requestedOrientation
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    override fun onStop() {
        hostOrientation?.let { hostActivity?.requestedOrientation = it }
        hostOrientation = null
        super.onStop()
    }

    private fun loadPhoto() {
        dialogScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(photoData, 0, photoData.size)
                }

                if (bitmap != null) {
                    photoView.setImageBitmap(bitmap)
                    progressBar.visibility = View.GONE
                } else {
                    Timber.e("Failed to decode photo")
                    dismiss()
                }
            }
            catch (e: Exception) {
                Timber.e(e, "Error loading fullscreen photo")
                dismiss()
            }
        }
    }

    override fun dismiss() {
        dialogScope.cancel()
        photoView.setImageDrawable(null) // Free memory
        super.dismiss()
    }
}