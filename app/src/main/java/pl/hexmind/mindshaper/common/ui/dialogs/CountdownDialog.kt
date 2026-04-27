package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R
import java.time.Duration

/**
 * Dialog with a live countdown timer — updates every minute until time runs out.
 * Cancels the timer automatically on dismiss.
 */
class CountdownDialog(
    private val context: Context,
    private val title: String,
    private val durationMs: Long,
    private val message: String,
    private val onFinish: (() -> Unit)? = null
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.common_countdown_dialog, null)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        val tvCountdown = dialogView.findViewById<TextView>(R.id.tv_countdown)

        dialogView.findViewById<TextView>(R.id.tv_info_header).text = title
        dialogView.findViewById<TextView>(R.id.tv_description).text = message
        tvCountdown.text = formatCompact(Duration.ofMillis(durationMs))

        dialogView.findViewById<MaterialButton>(R.id.btn_dismiss).setOnClickListener {
            dialog.dismiss()
        }

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()

        val timer = object : CountDownTimer(durationMs, 60_000) {
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = formatCompact(Duration.ofMillis(millisUntilFinished))
            }
            override fun onFinish() {
                dialog.dismiss()
                onFinish?.invoke()
            }
        }

        timer.start()
        dialog.setOnDismissListener { timer.cancel() }
    }

    // Compact time label e.g. "2h 30m" or "45m"
    private fun formatCompact(remaining: Duration): String {
        val h = remaining.toHours()
        val m = remaining.toMinutes() % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0          -> "${h}h"
            else           -> "${m}m"
        }
    }
}