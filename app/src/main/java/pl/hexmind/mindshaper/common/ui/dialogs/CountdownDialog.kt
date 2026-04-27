package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.os.CountDownTimer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration

/**
 * Dialog with a live countdown timer — updates message every minute until time runs out.
 * Cancels the timer automatically on dismiss.
 */
class CountdownDialog(
    private val context: Context,
    private val title: String,
    private val durationMs: Long,
    private val formatMessage: (Duration) -> String,
    private val onFinish: (() -> Unit)? = null
) {

    fun show() {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(formatMessage(Duration.ofMillis(durationMs)))
            .setPositiveButton(context.getString(android.R.string.ok)) { d, _ -> d.dismiss() }
            .show()

        val timer = object : CountDownTimer(durationMs, 60_000) {
            override fun onTick(millisUntilFinished: Long) {
                dialog.setMessage(formatMessage(Duration.ofMillis(millisUntilFinished)))
            }
            override fun onFinish() {
                dialog.dismiss()
                onFinish?.invoke()
            }
        }

        timer.start()
        dialog.setOnDismissListener { timer.cancel() }
    }
}