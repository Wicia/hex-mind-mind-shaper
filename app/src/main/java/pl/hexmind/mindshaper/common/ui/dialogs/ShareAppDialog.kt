package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R

/**
 * Sharing the app with others: QR code of the APK folder link, the link itself to copy or send,
 * and the installed version.
 */
class ShareAppDialog(
    private val context: Context,
    private val versionText: String
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.common_share_app_dialog, null)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_share_app_version).text = versionText

        dialogView.findViewById<MaterialButton>(R.id.btn_share_app_copy).setOnClickListener {
            copyLink()
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_share_app_send).setOnClickListener {
            sendLink()
            dialog.dismiss()
        }

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun copyLink() {
        val link = context.getString(R.string.settings_share_app_link)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(link, link))

        // ! Android 13+ confirms the copy itself - a toast there would double it
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.settings_share_app_copied, Toast.LENGTH_SHORT).show()
        }
    }

    // System share sheet - messenger, mail or any other app
    private fun sendLink() {
        val message = context.getString(
            R.string.settings_share_app_message,
            context.getString(R.string.settings_share_app_link)
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }
}
