package pl.hexmind.mindshaper.activities

import android.app.Dialog
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

class DomainsListDialog private constructor(
    private val context: Context,
    private val icons: List<DomainsListItem>,
    private val onIconSelected: (DomainsListItem) -> Unit,
    private val dimAmount: Float,
    private val marginDp: Int
) {

    private var dialog: Dialog? = null

    fun show() {
        dialog = Dialog(context).apply {
            setContentView(R.layout.common_dialog_items_list)
            setupRecyclerView()
            setupButtons()
            setupWindow()
        }
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    private fun Dialog.setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_icons_list)
        val adapter = DialogDomainsListAdapter(icons) { selectedIcon ->
            onIconSelected(selectedIcon)
            dismiss()
        }
        recyclerView.adapter = adapter
    }

    private fun Dialog.setupButtons() {
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }
    }

    private fun Dialog.setupWindow() {
        window?.apply {
            setBackgroundDrawable(TRANSPARENT.toDrawable())
            setDimAmount(dimAmount)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            val displayMetrics = context.resources.displayMetrics
            val marginPx = (marginDp * displayMetrics.density).toInt()
            val width = displayMetrics.widthPixels - (marginPx * 2)

            setLayout(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    class Builder(private val context: Context) {
        private var icons: List<DomainsListItem> = emptyList()
        private var onIconSelected: (DomainsListItem) -> Unit = {}
        private var dimAmount: Float = 0.9f
        private var marginDp: Int = 48

        fun setIcons(icons: List<DomainsListItem>) = apply { this.icons = icons }

        fun setOnIconSelected(callback: (DomainsListItem) -> Unit) = apply {
            this.onIconSelected = callback
        }

        fun setDimAmount(amount: Float) = apply { this.dimAmount = amount }

        fun setMargin(marginDp: Int) = apply { this.marginDp = marginDp }

        fun build(): DomainsListDialog {
            require(icons.isNotEmpty()) { "Icons list cannot be empty" }
            return DomainsListDialog(context, icons, onIconSelected, dimAmount, marginDp)
        }

        fun show(): DomainsListDialog {
            return build().apply { show() }
        }
    }
}