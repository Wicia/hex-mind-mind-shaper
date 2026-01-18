package pl.hexmind.mindshaper.common.ui

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.R
import timber.log.Timber

/**
 * Fullscreen photo viewer with zoom support
 */
class PhotoFullscreenDialog(
    context: Context,
    private val photoData: ByteArray
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var photoView: PhotoView
    private lateinit var btnClose: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_photo_fullscreen)

        // Full screen setup
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        photoView = findViewById(R.id.photo_view)
        btnClose = findViewById(R.id.btn_close)
        progressBar = findViewById(R.id.progress_bar)

        btnClose.setOnClickListener { dismiss() }

        loadPhoto()
    }

    private fun loadPhoto() {
        dialogScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(photoData, 0, photoData.size)
                }

                if (bitmap != null) {
                    photoView.setImageBitmap(bitmap)
                    progressBar.visibility = android.view.View.GONE
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