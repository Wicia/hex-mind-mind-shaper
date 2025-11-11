package pl.hexmind.mindshaper.common.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

class CommonIconsListDialog private constructor(
    private val context: Context,
    private val title: String?,
    private val icons: List<CommonIconsListItem>,
    private val onIconSelected: (CommonIconsListItem) -> Unit,
) {
    private val dimAmount: Float = 0.9f
    private val marginDp: Int = 48

    private var dialog: Dialog? = null

    fun show() {
        dialog = Dialog(context).apply {
            setContentView(R.layout.common_dialog_icons_list)
            setupTitle()
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
        val adapter = CommonIconsListAdapter(icons) { selectedIcon ->
            onIconSelected(selectedIcon)
            dismiss()
        }
        recyclerView.adapter = adapter
    }

    private fun Dialog.setupTitle() {
        findViewById<TextView>(R.id.tv_dialog_title).text = (title)
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

        private var title : String = ""
        private var icons: List<CommonIconsListItem> = emptyList()
        private var onIconSelected: (CommonIconsListItem) -> Unit = {}

        fun setTitle(title : String) = apply {
            this.title = title
        }

        fun setIcons(icons: List<CommonIconsListItem>) = apply { this.icons = icons }

        fun setOnIconSelected(callback: (CommonIconsListItem) -> Unit) = apply {
            this.onIconSelected = callback
        }
        fun build(): CommonIconsListDialog {
            require(icons.isNotEmpty()) { "Icons list cannot be empty" }
            return CommonIconsListDialog(context, title, icons, onIconSelected)
        }

        fun show(): CommonIconsListDialog {
            return build().apply { show() }
        }
    }
}